package com.jp.shopkeeper.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidades de texto: colores legacy (compatibles 1.8+), placeholders
 * {clave} y recortes seguros para titulos de inventario.
 */
public final class Text {

    private Text() {
    }

    /**
     * Traduce codigos & a colores (§).
     *
     * Ademas de los 16 colores legacy acepta &#RRGGBB (formato que usan los
     * servidores modernos): en 1.16+ se convierte al formato nativo de color
     * hexadecimal y en versiones anteriores se aproxima al color legacy mas
     * parecido, para que el texto siga viendose bien.
     */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        // MiniMessage (opcional) primero: sus etiquetas se convierten a § y el
        // texto que no sea una etiqueta sigue pasando por los codigos &.
        String mini = MiniText.render(input);
        return ChatColor.translateAlternateColorCodes('&', expandHex(mini == null ? input : mini));
    }

    /** Colores legacy con su RGB, para aproximar un hex en versiones antiguas. */
    private static final ChatColor[] LEGACY_COLORS = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE,
    };
    private static final int[][] LEGACY_RGB = {
            {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
            {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
            {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
            {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255},
    };

    private static Boolean hexCapable;

    /** true si el servidor soporta colores hexadecimales (1.16+). */
    public static boolean hexSupported() {
        if (hexCapable == null) {
            hexCapable = Boolean.valueOf(detectHexSupport());
        }
        return hexCapable.booleanValue();
    }

    private static boolean detectHexSupport() {
        try {
            String version = org.bukkit.Bukkit.getBukkitVersion();
            if (version == null || version.isEmpty()) {
                return false;
            }
            String[] parts = version.split("[.-]");
            int major = Integer.parseInt(parts[0].trim());
            if (major > 1) {
                return true;
            }
            return parts.length > 1 && Integer.parseInt(parts[1].trim()) >= 16;
        } catch (Throwable ignored) {
            // sin servidor (pruebas) o version rara: modo legacy
            return false;
        }
    }

    /** Convierte &#RRGGBB al formato de la version en uso. */
    private static String expandHex(String input) {
        if (input.indexOf("&#") < 0) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        int index = 0;
        while (index < input.length()) {
            if (input.charAt(index) == '&' && index + 8 <= input.length() && input.charAt(index + 1) == '#') {
                String hex = input.substring(index + 2, index + 8);
                if (isHex(hex)) {
                    out.append(hexCode(hex));
                    index += 8;
                    continue;
                }
            }
            out.append(input.charAt(index));
            index++;
        }
        return out.toString();
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            boolean valid = (character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F');
            if (!valid) {
                return false;
            }
        }
        return value.length() == 6;
    }

    private static String hexCode(String hex) {
        if (!hexSupported()) {
            return nearestLegacy(hex).toString();
        }
        StringBuilder out = new StringBuilder(14);
        out.append('§').append('x');
        for (int i = 0; i < 6; i++) {
            out.append('§').append(Character.toLowerCase(hex.charAt(i)));
        }
        return out.toString();
    }

    private static ChatColor nearestLegacy(String hex) {
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);
        ChatColor best = ChatColor.WHITE;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < LEGACY_COLORS.length; i++) {
            long dr = red - LEGACY_RGB[i][0];
            long dg = green - LEGACY_RGB[i][1];
            long db = blue - LEGACY_RGB[i][2];
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = LEGACY_COLORS[i];
            }
        }
        return best;
    }

    private static Integer titleLength;

    /**
     * Longitud maxima del titulo de un menu: los clientes antiguos cortan a
     * 32 caracteres visibles y los modernos (1.13+) no tienen ese limite.
     */
    public static int maxTitleLength() {
        if (titleLength == null) {
            titleLength = Integer.valueOf(detectTitleLength());
        }
        return titleLength.intValue();
    }

    private static int detectTitleLength() {
        try {
            String version = org.bukkit.Bukkit.getBukkitVersion();
            if (version == null || version.isEmpty()) {
                return 32;
            }
            String[] parts = version.split("[.-]");
            int major = Integer.parseInt(parts[0].trim());
            int minor = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            boolean modern = major > 1 || (major == 1 && minor >= 13);
            return modern ? 128 : 32;
        } catch (Throwable ignored) {
            return 32;
        }
    }

    /** Traduce codigos & de una lista completa. */
    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<String>();
        if (input == null) {
            return out;
        }
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }

    /** Quita todos los colores de un texto. */
    public static String strip(String input) {
        return ChatColor.stripColor(color(input));
    }

    /**
     * Reemplaza placeholders {clave} por su valor.
     * El valor puede contener saltos de linea (\n) para varias lineas.
     */
    public static String apply(String text, Map<String, String> placeholders) {
        if (text == null) {
            return "";
        }
        String out = text;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue();
                out = out.replace("{" + entry.getKey() + "}", value);
            }
        }
        return out;
    }

    /**
     * Aplica placeholders a una lista de lineas y expande los valores
     * multilinea: un {coste} con dos lineas produce dos lineas reales.
     */
    public static List<String> apply(List<String> lines, Map<String, String> placeholders) {
        List<String> out = new ArrayList<String>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            String replaced = apply(line, placeholders);
            String[] split = replaced.split("\n", -1);
            for (String part : split) {
                out.add(part);
            }
        }
        return out;
    }

    /**
     * Recorta un texto sin cortar los codigos de color (&a, &l...) a mitad.
     * Los titulos de cofre deben quedar por debajo de 32 caracteres visibles
     * para que la 1.8 los acepte.
     */
    public static String truncate(String text, int maxVisible) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int visible = 0;
        for (int i = 0; i < text.length() && visible < maxVisible; i++) {
            char current = text.charAt(i);
            if ((current == '&' || current == '\u00A7') && i + 1 < text.length()) {
                out.append(current).append(text.charAt(i + 1));
                i++;
                continue;
            }
            out.append(current);
            visible++;
        }
        return out.toString();
    }

    /**
     * Nombre legible de un item: nombre personalizado si existe,
     * o el nombre del material con formato ("DIAMOND_SWORD" -> "Diamond Sword").
     */
    public static String itemName(ItemStack item) {
        if (item == null) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isEmpty()) {
            return meta.getDisplayName();
        }
        return prettyMaterial(item);
    }

    /** Nombre legible del material de un item, con su variante de datos si la tiene. */
    public static String prettyMaterial(ItemStack item) {
        if (item == null) {
            return "";
        }
        String name = Materials.nameOf(item.getType()).toLowerCase(Locale.ROOT);
        String[] parts = name.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        try {
            if (item.getDurability() != 0) {
                out.append(" (").append(item.getDurability()).append(')');
            }
        } catch (Throwable ignored) {
            // sin durabilidad disponible, no pasa nada
        }
        return out.toString();
    }
}
