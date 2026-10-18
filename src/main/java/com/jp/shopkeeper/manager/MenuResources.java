package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.util.ItemBuilder;
import com.jp.shopkeeper.util.LangFile;
import com.jp.shopkeeper.util.Materials;
import com.jp.shopkeeper.util.Reflect;
import com.jp.shopkeeper.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lectura de menus.yml: titulos, botones, relleno y plantillas de lore.
 * Los slots propuestos en el archivo ya estan validados; los materiales
 * se traducen entre versiones con Materials.
 */
public final class MenuResources {

    private final JPShopKeeper plugin;
    private final Set<String> warned = new HashSet<String>();

    private YamlConfiguration yml;
    private String fileName = "menus.yml";

    public MenuResources(JPShopKeeper plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String language = plugin.configManager().language();
        File file = LangFile.resolve(plugin, "menus", language);
        File base = LangFile.baseFile(plugin, "menus");
        fileName = file.getName();
        yml = YamlConfiguration.loadConfiguration(file);
        if (base.getName().equals(fileName)) {
            plugin.getLogger().info("Menus: " + fileName);
            if (language != null && !language.isEmpty() && !"es".equals(language)) {
                plugin.getLogger().warning("No existe menus_" + language
                        + ".yml: se usan los menus de " + fileName + ".");
            }
        } else {
            yml.setDefaults(YamlConfiguration.loadConfiguration(base));
            plugin.getLogger().info("Menus: " + fileName + " (lo que falte se toma de " + base.getName() + ")");
        }
        warned.clear();
    }

    /** Archivo de menus en uso (menus.yml, menus_en.yml...), para la consola. */
    public String fileName() {
        return fileName;
    }

    /** Titulo de menu, coloreado, con placeholders y recortado a 32 visibles (limite 1.8). */
    public String title(String menu, Map<String, String> placeholders) {
        String raw = yml.getString(menu + ".title", "&8Menu");
        // 32 caracteres visibles en clientes antiguos; los modernos no tienen limite.
        return Text.truncate(Text.color(Text.apply(raw, placeholders)), Text.maxTitleLength());
    }

    public int rows(String menu, int fallback) {
        int rows = yml.getInt(menu + ".rows", fallback);
        return Math.max(1, Math.min(6, rows));
    }

    public int slot(String menu, String button, int fallback) {
        return yml.getInt(menu + ".buttons." + button + ".slot", fallback);
    }

    /** Boton configurado de un menu (material, nombre, lore, glow), con placeholders. */
    public ItemStack button(String menu, String button, Map<String, String> placeholders) {
        return build(menu + ".buttons." + button, placeholders);
    }

    /** Item de una ruta arbitraria de menus.yml (page-icon, placeholders del editor...). */
    public ItemStack item(String path, Map<String, String> placeholders) {
        return build(path, placeholders);
    }

    public List<Integer> fillerSlots(String menu) {
        List<Integer> slots = yml.getIntegerList(menu + ".filler.slots");
        if (slots.isEmpty() && yml.isString(menu + ".filler.slots")) {
            slots = parseInts(yml.getString(menu + ".filler.slots", ""));
        }
        return slots;
    }

    public ItemStack filler(String menu) {
        return build(menu + ".filler", null);
    }

    /** Lista de lineas de lore de una ruta, coloreada y con placeholders expandidos. */
    public List<String> lore(String path, Map<String, String> placeholders) {
        return Text.color(Text.apply(readLines(path), placeholders));
    }

    /** Valor de texto de una ruta (por ejemplo, un estado), coloreado. */
    public String text(String path, Map<String, String> placeholders) {
        return Text.color(Text.apply(yml.getString(path, ""), placeholders));
    }

    private ItemStack build(String path, Map<String, String> placeholders) {
        String materialName = yml.getString(path + ".material", "");
        Materials.Resolved resolved = Materials.resolve(materialName);
        if (resolved == null) {
            if (warned.add(path)) {
                plugin.getLogger().warning("Material invalido en " + fileName + ": " + path + " (" + materialName + ")");
            }
            resolved = Materials.resolve("PAPER");
            if (resolved == null) {
                return null;
            }
        }
        ItemStack item = new ItemStack(resolved.material);
        if (resolved.data != 0) {
            try {
                item.setDurability(resolved.data);
            } catch (Throwable ignored) {
                // sin datos en esta version
            }
        }
        ItemBuilder builder = ItemBuilder.of(item);
        if (builder == null) {
            return item;
        }
        String name = yml.getString(path + ".name", null);
        if (name != null) {
            builder.name(Text.apply(name, placeholders));
        }
        // Opciones modernas de resource pack (se ignoran en versiones antiguas).
        int customModelData = yml.getInt(path + ".custom-model-data", 0);
        if (customModelData > 0) {
            builder.customModelData(customModelData);
        }
        String head = yml.getString(path + ".head", null);
        if (head != null && !head.isEmpty()) {
            applyHead(item, head);
        }
        List<String> loreLines = readLines(path + ".lore");
        if (!loreLines.isEmpty()) {
            builder.lore(Text.apply(loreLines, placeholders));
        }
        if (yml.getBoolean(path + ".glow", false)) {
            builder.glow();
        }
        return builder.build();
    }

    /**
     * Cabeza de jugador para un icono de menu ("head: Notch").
     *
     * Solo se usan jugadores en linea o ya conocidos por el servidor: pedir un
     * nombre desconocido a la API de Mojang bloquearia el hilo principal.
     */
    private void applyHead(ItemStack item, String ownerName) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof SkullMeta)) {
                return;
            }
            OfflinePlayer owner = Bukkit.getPlayerExact(ownerName);
            if (owner == null) {
                try {
                    java.lang.reflect.Method cached =
                            Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class);
                    Object value = cached.invoke(null, ownerName);
                    if (value instanceof OfflinePlayer) {
                        owner = (OfflinePlayer) value;
                    }
                } catch (Throwable ignored) {
                    // metodo no disponible antes de 1.16.5
                }
            }
            if (owner == null) {
                if (warned.add("head:" + ownerName)) {
                    plugin.getLogger().warning("No se pudo usar la cabeza '" + ownerName
                            + "' en " + fileName + ": ese jugador no esta en linea ni en la cache del servidor.");
                }
                return;
            }
            Object result = Reflect.call(meta, "setOwningPlayer", owner);
            if (result == null) {
                result = Reflect.call(meta, "setOwner", ownerName);
            }
            if (result != null) {
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // sin cabezas personalizadas en esta version
        }
    }

    /** Lee una lista de strings; acepta tambien un unico string multilinea. */
    private List<String> readLines(String path) {
        List<String> lines = yml.getStringList(path);
        if (lines.isEmpty() && yml.isString(path)) {
            String value = yml.getString(path, "");
            for (String part : value.split("\n")) {
                lines.add(part);
            }
        }
        return lines;
    }

    /** Convierte "45, 46, 47" en lista de enteros. */
    private List<Integer> parseInts(String raw) {
        List<Integer> out = new ArrayList<Integer>();
        if (raw == null) {
            return out;
        }
        for (String token : raw.split("[,\\s]+")) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.valueOf(token.trim()));
            } catch (NumberFormatException ignored) {
                // token invalido: se ignora
            }
        }
        return out;
    }
}
