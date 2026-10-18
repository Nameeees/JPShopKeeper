package com.jp.shopkeeper.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolucion de materiales compatible 1.8 - ultima version.
 *
 * Regla de oro: este plugin NUNCA usa constantes del enum Material
 * directamente (Material.GRASS dejo de existir en 1.13 y romperia el
 * JAR en servidores nuevos). Todo material llega como texto desde los
 * YAML y se resuelve aqui en tiempo de ejecucion.
 *
 * Se aceptan las tres formas de escribir un material:
 *   - moderno:  GRAY_STAINED_GLASS_PANE      (lo normal en 1.13+)
 *   - antiguo:  STAINED_GLASS_PANE:7         (estilo 1.8)
 *   - prefijos: minecraft:stone, LEGACY_STONE
 *
 * En 1.13+ los nombres antiguos se traducen a su material moderno con la
 * data exacta (STAINED_GLASS_PANE:7 -> GRAY_STAINED_GLASS_PANE) y en
 * 1.8/1.12 los nombres modernos se traducen al material antiguo con su
 * data (GRAY_STAINED_GLASS_PANE -> STAINED_GLASS_PANE:7).
 */
public final class Materials {

    /** Material resuelto + variante de datos legacy (para 1.8). */
    public static final class Resolved {
        public final Material material;
        public final short data;

        Resolved(Material material, short data) {
            this.material = material;
            this.data = data;
        }
    }

    private static final class Alias {
        final String legacyName;
        final short legacyData;

        Alias(String legacyName, short legacyData) {
            this.legacyName = legacyName;
            this.legacyData = legacyData;
        }
    }

    /** Nombre moderno -> nombre antiguo y data con la que se representa en 1.8/1.12. */
    private static final Map<String, Alias> MODERN_TO_LEGACY = new HashMap<String, Alias>();
    /** "NOMBRE_ANTIGUO:data" -> nombre moderno equivalente (1.13+). */
    private static final Map<String, String> LEGACY_TO_MODERN = new HashMap<String, String>();

    static {
        alias("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 7);
        alias("LIGHT_GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 8);
        alias("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 15);
        alias("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 0);
        alias("LIME_DYE", "INK_SACK", 10);
        alias("RED_DYE", "INK_SACK", 1);
        alias("VILLAGER_SPAWN_EGG", "MONSTER_EGG", 120);
        alias("WRITABLE_BOOK", "BOOK_AND_QUILL", 0);
        alias("CLOCK", "WATCH", 0);
        alias("GRASS_BLOCK", "GRASS", 0);
        alias("COMPARATOR", "REDSTONE_COMPARATOR", 0);
        alias("EXPERIENCE_BOTTLE", "EXP_BOTTLE", 0);
        alias("PLAYER_HEAD", "SKULL_ITEM", 3);
    }

    private Materials() {
    }

    private static void alias(String modern, String legacy, int data) {
        MODERN_TO_LEGACY.put(modern, new Alias(legacy, (short) data));
        // La clave lleva la data: si no, el ultimo alias registrado pisaria a
        // los demas (STAINED_GLASS_PANE tiene cuatro variantes).
        LEGACY_TO_MODERN.put(legacy + ":" + data, modern);
    }

    /**
     * Resuelve un nombre de material de cualquier version.
     * Devuelve null si no existe ni como nombre directo ni como alias.
     */
    public static Resolved resolve(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return null;
        }
        int colon = normalized.indexOf(':');
        String name = colon < 0 ? normalized : normalized.substring(0, colon);
        short data = 0;
        if (colon >= 0) {
            try {
                data = Short.parseShort(normalized.substring(colon + 1).trim());
            } catch (NumberFormatException ignored) {
                data = 0;
            }
        }
        if (name.isEmpty()) {
            return null;
        }

        // 1) El nombre existe tal cual en esta version (STAINED_GLASS_PANE en
        //    1.8, GRAY_STAINED_GLASS_PANE en 1.13+): se respeta su data.
        Material direct = match(name);
        if (direct != null) {
            return new Resolved(direct, data);
        }

        // 2) Nombre moderno en un servidor antiguo: STAINED_GLASS_PANE + data 7.
        Alias alias = MODERN_TO_LEGACY.get(name);
        if (alias != null) {
            Material legacy = match(alias.legacyName);
            if (legacy != null) {
                return new Resolved(legacy, alias.legacyData);
            }
        }

        // 3) Nombre antiguo en un servidor moderno: la data decide el material.
        String modern = LEGACY_TO_MODERN.get(name + ":" + data);
        if (modern != null) {
            Material material = match(modern);
            if (material != null) {
                return new Resolved(material, (short) 0);
            }
        }
        return null;
    }

    /** Crea un ItemStack desde un nombre de material; null si no se pudo resolver. */
    public static ItemStack stack(String raw, int amount) {
        Resolved resolved = resolve(raw);
        if (resolved == null) {
            return null;
        }
        int safeAmount = Numbers.clamp(amount, 1, 64);
        ItemStack item = new ItemStack(resolved.material, safeAmount);
        if (resolved.data != 0) {
            try {
                item.setDurability(resolved.data);
            } catch (Throwable ignored) {
                // sin datos en esta version, no pasa nada
            }
        }
        return item;
    }

    /**
     * Nombre de un material en texto, a prueba de versiones.
     *
     * En las ultimas versiones del servidor Material#name() puede devolver
     * nombres recortados o lanzar una excepcion (comprobado en Paper 26.3:
     * GRAY_STAINED_GLASS_PANE devuelve "AINED_GLASS_PANE" y STONE lanza
     * StringIndexOutOfBoundsException), asi que nunca se usa a ciegas:
     * se prefiere la clave del registro (1.13+) y name() queda como
     * ultimo recurso, siempre dentro de un try/catch.
     */
    public static String nameOf(Material material) {
        if (material == null) {
            return "";
        }
        try {
            Object key = Reflect.call(material, "getKey");
            if (key != null) {
                Object value = Reflect.call(key, "getKey");
                if (value instanceof String && !((String) value).isEmpty()) {
                    return ((String) value).toUpperCase(Locale.ROOT);
                }
            }
        } catch (Throwable ignored) {
            // sin getKey (1.8-1.12): se usa name()
        }
        try {
            String name = material.name();
            return name == null ? "" : name;
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** Nombre normalizado: mayusculas, espacios a guion bajo, sin prefijos. */
    public static String normalize(String raw) {
        String name = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        if (name.startsWith("LEGACY_")) {
            name = name.substring("LEGACY_".length());
        }
        return name;
    }

    private static Material match(String name) {
        try {
            return Material.matchMaterial(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
