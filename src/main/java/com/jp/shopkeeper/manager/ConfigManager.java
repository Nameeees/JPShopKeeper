package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.util.MiniText;
import com.jp.shopkeeper.util.Numbers;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lectura de config.yml con valores por defecto y limites sensatos.
 * Cualquier numero ajustable del plugin vive aqui, nunca en el codigo.
 */
public final class ConfigManager {

    private final JPShopKeeper plugin;

    private boolean debug;
    private boolean logNpcRespawn;
    private String language;
    private boolean placeholderApi;
    private boolean miniMessage;

    private String defaultType;
    private List<String> allowedTypes;

    private boolean npcLookAtPlayers;
    private double lookRadius;
    private int maintenanceInterval;
    private int anchorInterval;
    private double snapDistance;
    private boolean npcProtect;
    private boolean npcForcePersistent;

    private boolean matchName;
    private boolean matchLore;
    private boolean matchEnchantments;
    private boolean matchDurability;
    private boolean matchNbt;
    private boolean dropResultIfFull;
    private int maxCostSlots;
    private int maxPages;

    public ConfigManager(JPShopKeeper plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        debug = config.getBoolean("settings.debug", false);
        logNpcRespawn = config.getBoolean("settings.log-npc-respawn", true);
        language = sanitizeLanguage(config.getString("settings.language", "es"));
        placeholderApi = config.getBoolean("hooks.placeholderapi", true);
        miniMessage = config.getBoolean("settings.mini-message", false);
        MiniText.setEnabled(miniMessage);
        if (miniMessage && !MiniText.isAvailable()) {
            plugin.getLogger().warning("settings.mini-message esta activado pero este servidor no incluye"
                    + " Adventure (Paper 1.16+): se usaran los colores legacy.");
        }

        defaultType = config.getString("npc.default-type", "VILLAGER");
        List<String> types = config.getStringList("npc.allowed-types");
        if (types == null || types.isEmpty()) {
            types = new ArrayList<String>(Arrays.asList("VILLAGER"));
        }
        allowedTypes = types;

        npcLookAtPlayers = config.getBoolean("npc.look-at-players", true);
        lookRadius = Numbers.clamp(config.getDouble("npc.look-radius", 6.0), 1.0, 32.0);
        maintenanceInterval = Numbers.clamp(config.getInt("npc.maintenance-interval-ticks", 40), 5, 1200);
        anchorInterval = Numbers.clamp(config.getInt("npc.anchor-interval-ticks", 1), 1, 40);
        snapDistance = Numbers.clamp(config.getDouble("npc.snap-distance", 0.35), 0.05, 5.0);
        npcProtect = config.getBoolean("npc.protect", true);
        npcForcePersistent = config.getBoolean("npc.force-persistent", true);

        matchName = config.getBoolean("trades.match.name", true);
        matchLore = config.getBoolean("trades.match.lore", false);
        matchEnchantments = config.getBoolean("trades.match.enchantments", true);
        matchDurability = config.getBoolean("trades.match.durability", true);
        matchNbt = config.getBoolean("trades.match.nbt", true);
        dropResultIfFull = config.getBoolean("trades.drop-result-if-full", true);
        maxCostSlots = Numbers.clamp(config.getInt("trades.max-cost-slots", 3), 1, 3);
        maxPages = Numbers.clamp(config.getInt("trades.max-pages", 45), 1, 45);
    }

    /** true si settings.debug esta activo (para no construir el texto si no hace falta). */
    public boolean debugEnabled() {
        return debug;
    }

    /** Log de depuracion: solo con settings.debug en true. */
    public void debug(String message) {
        if (debug) {
            plugin.getLogger().info("[debug] " + message);
        }
    }

    public boolean logNpcRespawn() {
        return logNpcRespawn;
    }

    /** Idioma de los textos: es, en... (cada idioma tiene sus messages_XX.yml y menus_XX.yml). */
    public String language() {
        return language;
    }

    /** Registra los placeholders %jpsk_...% si PlaceholderAPI esta instalado. */
    public boolean placeholderApi() {
        return placeholderApi;
    }

    /** Un valor raro en config.yml no puede salirse de la carpeta del plugin. */
    private static String sanitizeLanguage(String raw) {
        if (raw == null) {
            return "es";
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return clean.isEmpty() ? "es" : clean;
    }

    public String defaultType() {
        return defaultType;
    }

    public List<String> allowedTypes() {
        return allowedTypes;
    }

    public boolean npcLookAtPlayers() {
        return npcLookAtPlayers;
    }

    public double lookRadius() {
        return lookRadius;
    }

    public int maintenanceInterval() {
        return maintenanceInterval;
    }

    public int anchorInterval() {
        return anchorInterval;
    }

    public double snapDistance() {
        return snapDistance;
    }

    public boolean npcProtect() {
        return npcProtect;
    }

    public boolean npcForcePersistent() {
        return npcForcePersistent;
    }

    public boolean matchName() {
        return matchName;
    }

    public boolean matchLore() {
        return matchLore;
    }

    public boolean matchEnchantments() {
        return matchEnchantments;
    }

    public boolean matchDurability() {
        return matchDurability;
    }

    public boolean matchNbt() {
        return matchNbt;
    }

    public boolean dropResultIfFull() {
        return dropResultIfFull;
    }

    public int maxCostSlots() {
        return maxCostSlots;
    }

    public int maxPages() {
        return maxPages;
    }
}
