package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.ItemMatch;
import com.jp.shopkeeper.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validacion y ejecucion de tradeos: permisos, enfriamientos, dinero
 * (Vault opcional) e items. El menu solo pinta; toda la logica vive aqui.
 */
public final class TradeService {

    public enum Status {
        OK,
        NO_PERMISSION,
        COOLDOWN,
        MISSING_ITEMS,
        MISSING_MONEY,
        NO_ECONOMY,
        FULL
    }

    /** Resultado de validar/ejecutar un tradeo. */
    public static final class Outcome {
        public final Status status;
        public final String missingItems;
        public final double missingMoney;
        public final double balance;
        public final long cooldownSeconds;
        public final String permission;
        public final boolean dropped;

        private Outcome(Status status, String missingItems, double missingMoney, double balance,
                        long cooldownSeconds, String permission, boolean dropped) {
            this.status = status;
            this.missingItems = missingItems;
            this.missingMoney = missingMoney;
            this.balance = balance;
            this.cooldownSeconds = cooldownSeconds;
            this.permission = permission;
            this.dropped = dropped;
        }

        static Outcome ok(boolean dropped) {
            return new Outcome(Status.OK, "", 0.0, 0.0, 0, "", dropped);
        }

        static Outcome noPermission(String permission) {
            return new Outcome(Status.NO_PERMISSION, "", 0.0, 0.0, 0, permission, false);
        }

        static Outcome cooldown(long seconds) {
            return new Outcome(Status.COOLDOWN, "", 0.0, 0.0, seconds, "", false);
        }

        static Outcome missingItems(String items) {
            return new Outcome(Status.MISSING_ITEMS, items, 0.0, 0.0, 0, "", false);
        }

        static Outcome missingMoney(double missing, double balance) {
            return new Outcome(Status.MISSING_MONEY, "", missing, balance, 0, "", false);
        }

        static Outcome noEconomy() {
            return new Outcome(Status.NO_ECONOMY, "", 0.0, 0.0, 0, "", false);
        }

        static Outcome full() {
            return new Outcome(Status.FULL, "", 0.0, 0.0, 0, "", false);
        }
    }

    private final JPShopKeeper plugin;
    /** clave: uuidJugador|trader|pagina|slot -> timestamp final del enfriamiento */
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();
    private final File cooldownFile;

    public TradeService(JPShopKeeper plugin) {
        this.plugin = plugin;
        this.cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        loadCooldowns();
    }

    /** Recupera los enfriamientos que seguian activos antes del ultimo apagado. */
    private void loadCooldowns() {
        if (!cooldownFile.isFile()) {
            return;
        }
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(cooldownFile);
            long now = System.currentTimeMillis();
            int recovered = 0;
            for (String key : yml.getKeys(false)) {
                long until = yml.getLong(key, 0L);
                if (until > now) {
                    cooldowns.put(key, Long.valueOf(until));
                    recovered++;
                }
            }
            if (recovered > 0) {
                plugin.configManager().debug("Enfriamientos recuperados: " + recovered);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudieron leer los enfriamientos: " + throwable);
        }
    }

    /** Guarda los enfriamientos que siguen activos (se llama al apagar). */
    public void saveCooldowns() {
        try {
            YamlConfiguration yml = new YamlConfiguration();
            long now = System.currentTimeMillis();
            int saved = 0;
            for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() != null && entry.getValue().longValue() > now) {
                    yml.set(entry.getKey(), entry.getValue());
                    saved++;
                }
            }
            if (saved == 0) {
                if (cooldownFile.isFile() && !cooldownFile.delete()) {
                    plugin.getLogger().warning("No se pudo borrar cooldowns.yml");
                }
                return;
            }
            yml.save(cooldownFile);
            plugin.configManager().debug("Enfriamientos guardados: " + saved);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudieron guardar los enfriamientos: " + throwable);
        }
    }

    /** Valida el tradeo sin ejecutarlo (para pintar estados y avisos). */
    public Outcome check(Player player, Trader trader, int page, int slot, Trade trade) {
        pruneCooldowns(System.currentTimeMillis());
        if (trade == null) {
            return Outcome.missingItems("");
        }
        if (trade.hasPermission() && !player.hasPermission(trade.getPermission())) {
            return Outcome.noPermission(trade.getPermission());
        }
        long remaining = cooldownRemaining(player, trader, page, slot, trade);
        if (remaining > 0) {
            return Outcome.cooldown(remaining);
        }
        if (trade.getMoney() > 0) {
            if (!plugin.economy().enabled()) {
                return Outcome.noEconomy();
            }
            double balance = plugin.economy().balance(player);
            if (balance + 0.0001 < trade.getMoney()) {
                return Outcome.missingMoney(trade.getMoney() - balance, balance);
            }
        }
        List<String> missing = new ArrayList<String>();
        for (ItemStack required : trade.getCostItems()) {
            int have = ItemMatch.count(player.getInventory(), required,
                    plugin.configManager().matchName(), plugin.configManager().matchLore(),
                    plugin.configManager().matchEnchantments(), plugin.configManager().matchDurability(),
                    plugin.configManager().matchNbt());
            if (have < required.getAmount()) {
                missing.add((required.getAmount() - have) + "x " + Text.strip(Text.itemName(required)));
            }
        }
        if (!missing.isEmpty()) {
            return Outcome.missingItems(join(missing));
        }
        if (!plugin.configManager().dropResultIfFull() && trade.getResult() != null) {
            // Copia solo los 36 slots de almacenamiento: en 1.9+ getContents()
            // devuelve tambien armadura y mano secundaria (41 elementos) y un
            // inventario de 36 no los acepta.
            Inventory simulation = Bukkit.createInventory(null, 36);
            ItemStack[] contents = player.getInventory().getContents();
            int slots = Math.min(36, contents.length);
            for (int i = 0; i < slots; i++) {
                simulation.setItem(i, contents[i]);
            }
            Map<Integer, ItemStack> leftover = simulation.addItem(trade.getResult().clone());
            if (!leftover.isEmpty()) {
                return Outcome.full();
            }
        }
        return Outcome.ok(false);
    }

    /** Ejecuta el tradeo si la validacion lo permite. */
    public Outcome execute(Player player, Trader trader, int page, int slot, Trade trade) {
        Outcome outcome = check(player, trader, page, slot, trade);
        if (outcome.status != Status.OK || trade.getResult() == null) {
            return outcome;
        }
        // El dinero se cobra primero: si el proveedor falla, no se entregan items.
        if (trade.getMoney() > 0 && !plugin.economy().withdraw(player, trade.getMoney())) {
            return Outcome.missingMoney(trade.getMoney(), plugin.economy().balance(player));
        }
        for (ItemStack required : trade.getCostItems()) {
            ItemMatch.remove(player.getInventory(), required, required.getAmount(),
                    plugin.configManager().matchName(), plugin.configManager().matchLore(),
                    plugin.configManager().matchEnchantments(), plugin.configManager().matchDurability(),
                    plugin.configManager().matchNbt());
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(trade.getResult().clone());
        boolean dropped = false;
        if (!leftover.isEmpty()) {
            dropped = true;
            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
        if (trade.getCooldownSeconds() > 0) {
            cooldowns.put(key(player, trader, page, slot),
                    System.currentTimeMillis() + trade.getCooldownSeconds() * 1000L);
        }
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
            // algunas versiones no necesitan refresco manual
        }
        return Outcome.ok(dropped);
    }

    /** Segundos restantes de enfriamiento (0 = listo). */
    public long cooldownRemaining(Player player, Trader trader, int page, int slot, Trade trade) {
        if (trade == null || trade.getCooldownSeconds() <= 0) {
            return 0;
        }
        Long until = cooldowns.get(key(player, trader, page, slot));
        long now = System.currentTimeMillis();
        if (until == null || until <= now) {
            return 0;
        }
        return (until - now + 999) / 1000;
    }

    /**
     * Los enfriamientos caducados no se acumulan sin fin: se limpian en
     * cuanto el mapa pasa de un tamano razonable (servidores con muchos
     * jugadores y tradeos).
     */
    private void pruneCooldowns(long now) {
        if (cooldowns.size() < 256) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private String key(Player player, Trader trader, int page, int slot) {
        UUID uuid = player.getUniqueId();
        return uuid.toString() + "|" + trader.getId() + "|" + page + "|" + slot;
    }

    private static String join(List<String> parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(part);
        }
        return out.toString();
    }
}
