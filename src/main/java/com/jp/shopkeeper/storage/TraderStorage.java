package com.jp.shopkeeper.storage;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.NbtUtil;
import com.jp.shopkeeper.util.Numbers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Persistencia en YAML: un archivo por trader en plugins/JPShopKeeper/traders/.
 * Los items se guardan con la serializacion nativa de Bukkit (material,
 * cantidad, meta y encantamientos) para que sobrevivan reinicios.
 *
 * El archivo es editable a mano: tras retocarlo, /trader reload.
 */
public final class TraderStorage {

    private static final String HEADER =
            "============================================================\n"
            + " JPShopKeeper - datos de un trader (un archivo por trader).\n"
            + " Editable a mano: usa /trader reload (o reinicia) al terminar.\n"
            + " Costes: hasta 3 items en 'cost' (indices 0-2).\n"
            + " Cada item es 'item' (mapa legible) + 'nbt' en base64 opcional:\n"
            + " los items con NBT de otros plugins se guardan tal cual.\n"
            + " money = coste en monedas (Vault) - permission = permiso, vacio = ninguno\n"
            + " cooldown = segundos entre usos - result = item que recibe el jugador\n"
            + "============================================================";

    private final JPShopKeeper plugin;
    private final File folder;

    public TraderStorage(JPShopKeeper plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "traders");
        ensureFolder();
    }

    private void ensureFolder() {
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("No se pudo crear la carpeta de traders: " + folder.getPath());
        }
    }

    public File fileFor(String id) {
        return new File(folder, id.toLowerCase(Locale.ROOT) + ".yml");
    }

    public boolean exists(String id) {
        return fileFor(id).isFile();
    }

    public List<Trader> loadAll() {
        List<Trader> out = new ArrayList<Trader>();
        File[] files = folder.listFiles();
        if (files == null) {
            return out;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            File canonical = canonicalFile(file);
            if (canonical == null) {
                continue;
            }
            try {
                Trader trader = load(canonical);
                if (trader != null) {
                    out.add(trader);
                }
            } catch (Throwable throwable) {
                plugin.getLogger().warning("No se pudo cargar el trader " + canonical.getName() + ": " + throwable.getMessage());
            }
        }
        return out;
    }

    /**
     * Normaliza el nombre del archivo del trader a minusculas antes de cargarlo.
     *
     * El plugin siempre guarda en minusculas: si en el servidor existe
     * "Tienda.yml", ese archivo quedaria congelado (se leeria al arrancar pero
     * los cambios irian a "tienda.yml"), asi que se renombra una sola vez. Si
     * ya existen los dos archivos se avisa y se ignora el de mayusculas. En
     * sistemas sin distincion de mayusculas (Windows) es el mismo archivo y no
     * se toca nada.
     */
    private File canonicalFile(File file) {
        String name = file.getName();
        String id = name.substring(0, name.length() - 4);
        String key = id.toLowerCase(Locale.ROOT);
        if (id.equals(key)) {
            return file;
        }
        File canonical = new File(folder, key + ".yml");
        try {
            if (canonical.isFile() && Files.isSameFile(canonical.toPath(), file.toPath())) {
                return file;
            }
        } catch (IOException ignored) {
            // el archivo destino no existe: no son el mismo
        }
        if (canonical.isFile()) {
            plugin.getLogger().warning("Hay dos archivos para el mismo trader ('" + name + "' y '"
                    + canonical.getName() + "'): se ignora '" + name + "'. Borra uno de los dos.");
            return null;
        }
        if (file.renameTo(canonical)) {
            plugin.getLogger().info("Archivo del trader renombrado a minusculas: " + name + " -> " + canonical.getName());
            return canonical;
        }
        plugin.getLogger().warning("No se pudo renombrar '" + name + "' a '" + canonical.getName()
                + "': renombralo a mano o el trader perdera los cambios.");
        return file;
    }

    public Trader load(File file) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String id = file.getName().substring(0, file.getName().length() - ".yml".length());
        String displayName = yml.getString("display-name", id);
        String entityType = yml.getString("entity-type", "VILLAGER");
        String profession = yml.getString("profession", "");
        Location home = readLocation(yml.getConfigurationSection("location"));
        if (home == null) {
            plugin.getLogger().warning("El trader " + id + " no tiene ubicacion valida y se omite.");
            return null;
        }
        boolean lookAtPlayers = yml.getBoolean("look-at-players", true);
        Trader trader = new Trader(id, displayName, entityType, home, lookAtPlayers);
        trader.setProfessionName(profession);
        trader.setHomeWorldName(home.getWorld() != null ? home.getWorld().getName() : yml.getString("location.world", ""));
        String uuidRaw = yml.getString("entity-uuid", "");
        if (uuidRaw != null && !uuidRaw.isEmpty()) {
            try {
                trader.setEntityId(UUID.fromString(uuidRaw));
            } catch (IllegalArgumentException ignored) {
                // uuid corrupto: el mantenimiento regenerara el NPC igualmente
            }
        }
        readPages(yml, trader);
        plugin.configManager().debug("Trader '" + id + "' cargado: " + trader.pageCount()
                + " pagina(s), " + trader.totalTrades() + " tradeo(s).");
        return trader;
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world", "");
        World world = worldName.isEmpty() ? null : Bukkit.getWorld(worldName);
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        // Si el mundo aun no esta cargado guardamos la posicion sin mundo;
        // el manager la resuelve por nombre cuando el mundo exista.
        if (world == null && worldName.isEmpty()) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void readPages(YamlConfiguration yml, Trader trader) {
        ConfigurationSection pagesSection = yml.getConfigurationSection("pages");
        plugin.configManager().debug("  [pages] seccion=" + (pagesSection == null ? "null" : pagesSection.getKeys(false)));
        if (pagesSection == null) {
            return;
        }
        for (String pageKey : pagesSection.getKeys(false)) {
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageKey);
            plugin.configManager().debug("  [pages] clave=" + pageKey + " seccion=" + (pageSection == null ? "null" : "ok"));
            if (pageSection == null) {
                continue;
            }
            TradePage page = new TradePage(pageSection.getString("name", ""));
            ConfigurationSection tradesSection = pageSection.getConfigurationSection("trades");
            plugin.configManager().debug("  [pages] trades=" + (tradesSection == null ? "null" : tradesSection.getKeys(false)));
            if (tradesSection != null) {
                for (String slotKey : tradesSection.getKeys(false)) {
                    int slot = parseInt(slotKey, -1);
                    if (slot < 0 || slot >= TradePage.TRADE_SLOTS) {
                        continue;
                    }
                    ConfigurationSection tradeSection = tradesSection.getConfigurationSection(slotKey);
                    if (tradeSection == null) {
                        continue;
                    }
                    try {
                        Trade trade = readTrade(tradeSection);
                        page.setTrade(slot, trade);
                        plugin.configManager().debug("  [pages] tradeo slot=" + slotKey
                                + " coste=" + trade.getCostItems().size()
                                + " resultado=" + (trade.getResult() == null ? "null" : "ok")
                                + " dinero=" + trade.getMoney());
                    } catch (Throwable throwable) {
                        plugin.getLogger().warning("No se pudo leer el tradeo " + slotKey + " del trader '"
                                + trader.getId() + "': " + throwable);
                    }
                }
            }
            trader.addPage(page);
        }
    }

    private Trade readTrade(ConfigurationSection tradeSection) {
        Trade trade = new Trade();
        ConfigurationSection costSection = tradeSection.getConfigurationSection("cost");
        if (costSection != null) {
            for (String indexKey : costSection.getKeys(false)) {
                int index = parseInt(indexKey, -1);
                ItemStack item = readItemEntry(costSection, indexKey);
                if (index >= 0 && index < Trade.COST_SLOTS && item != null) {
                    trade.setCost(index, item);
                }
            }
        }
        ItemStack result = readItemEntry(tradeSection, "result");
        if (result != null) {
            trade.setResult(result);
        }
        trade.setMoney(tradeSection.getDouble("money", 0.0));
        trade.setPermission(tradeSection.getString("permission", ""));
        trade.setCooldownSeconds(tradeSection.getInt("cooldown", 0));
        return trade;
    }

    /** Entrada de item en formato nuevo: { item: <mapa>, nbt: "<base64>" }. */
    private Map<String, Object> itemEntryMap(ItemStack item) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("item", item);
        String nbt = NbtUtil.toBase64(item);
        if (nbt != null && NbtUtil.shouldStoreBlob(item)) {
            entry.put("nbt", nbt);
        }
        return entry;
    }

    /**
     * Lee un item guardado, soportando los dos formatos:
     *  - nuevo: { item: <mapa estandar>, nbt: "<base64>" } (fidelidad total)
     *  - antiguo: el item directo en la clave (archivos de versiones previas)
     */
    private ItemStack readItemEntry(ConfigurationSection parent, String key) {
        if (parent.isConfigurationSection(key)) {
            ConfigurationSection entry = parent.getConfigurationSection(key);
            if (entry != null && (entry.isItemStack("item") || entry.isString("nbt"))) {
                ItemStack plain = entry.isItemStack("item") ? entry.getItemStack("item") : null;
                ItemStack loaded = entry.isString("nbt") ? NbtUtil.fromBase64(entry.getString("nbt")) : null;
                if (loaded == null) {
                    loaded = plain;
                }
                if (loaded != null && plain != null && plain.getAmount() != loaded.getAmount()) {
                    // Permite ajustar la cantidad a mano en el YAML.
                    loaded.setAmount(plain.getAmount());
                }
                if (loaded != null) {
                    return loaded;
                }
            }
        }
        return parent.getItemStack(key);
    }

    public void save(Trader trader) {
        File file = fileFor(trader.getId());
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().header(HEADER).copyHeader(true);
        yml.set("display-name", trader.getDisplayName());
        yml.set("entity-type", trader.getEntityTypeName());
        yml.set("profession", trader.getProfessionName());
        yml.set("entity-uuid", trader.getEntityId() == null ? "" : trader.getEntityId().toString());
        yml.set("look-at-players", trader.isLookAtPlayers());
        Location home = trader.getHome();
        if (home != null) {
            yml.set("location.world", trader.getHomeWorldName());
            yml.set("location.x", home.getX());
            yml.set("location.y", home.getY());
            yml.set("location.z", home.getZ());
            yml.set("location.yaw", (double) home.getYaw());
            yml.set("location.pitch", (double) home.getPitch());
        }
        Map<String, Object> pagesMap = new LinkedHashMap<String, Object>();
        int pageIndex = 0;
        for (TradePage page : trader.getPages()) {
            Map<String, Object> pageMap = new LinkedHashMap<String, Object>();
            pageMap.put("name", page.getName());
            Map<String, Object> tradesMap = new LinkedHashMap<String, Object>();
            for (Map.Entry<Integer, Trade> entry : page.getTrades().entrySet()) {
                Trade trade = entry.getValue();
                if (trade == null || trade.isEmpty()) {
                    continue;
                }
                Map<String, Object> tradeMap = new LinkedHashMap<String, Object>();
                Map<String, Object> costMap = new LinkedHashMap<String, Object>();
                for (int i = 0; i < Trade.COST_SLOTS; i++) {
                    ItemStack costItem = trade.getCost(i);
                    if (costItem != null) {
                        costMap.put(String.valueOf(i), itemEntryMap(costItem));
                    }
                }
                if (!costMap.isEmpty()) {
                    tradeMap.put("cost", costMap);
                }
                if (trade.getResult() != null) {
                    tradeMap.put("result", itemEntryMap(trade.getResult()));
                }
                if (trade.getMoney() > 0) {
                    tradeMap.put("money", trade.getMoney());
                }
                if (trade.hasPermission()) {
                    tradeMap.put("permission", trade.getPermission());
                }
                if (trade.getCooldownSeconds() > 0) {
                    tradeMap.put("cooldown", trade.getCooldownSeconds());
                }
                tradesMap.put(String.valueOf(entry.getKey()), tradeMap);
            }
            pageMap.put("trades", tradesMap);
            pagesMap.put(String.valueOf(pageIndex), pageMap);
            pageIndex++;
        }
        yml.set("pages", pagesMap);
        saveAtomically(yml, file);
    }

    /**
     * Guarda en un archivo temporal y lo renombra encima del original: si el
     * servidor muere a mitad de escritura, el YAML bueno sigue intacto.
     */
    private void saveAtomically(YamlConfiguration yml, File file) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            yml.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudo guardar " + file.getName() + ": " + throwable);
            if (temp.isFile() && !temp.delete()) {
                plugin.getLogger().warning("Quedo un archivo temporal sin borrar: " + temp.getName());
            }
        }
    }

    public void delete(Trader trader) {
        File file = fileFor(trader.getId());
        if (file.isFile() && !file.delete()) {
            plugin.getLogger().warning("No se pudo borrar " + file.getName());
        }
    }

    private static int parseInt(String raw, int fallback) {
        Integer value = Numbers.parseIntOrNull(raw);
        return value == null ? fallback : value;
    }
}
