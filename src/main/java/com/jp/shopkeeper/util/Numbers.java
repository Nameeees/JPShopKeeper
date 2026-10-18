package com.jp.shopkeeper.util;

import java.util.Locale;

/**
 * Formato de numeros para mensajes: sin decimales cuando el valor es
 * entero (250 -> "250") y con dos decimales cuando no (250.5 -> "250.50").
 */
public final class Numbers {

    private Numbers() {
    }

    public static String format(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    public static Integer parseIntOrNull(String input) {
        if (input == null) {
            return null;
        }
        try {
            return Integer.valueOf(input.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Double parseDoubleOrNull(String input) {
        if (input == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(input.trim().replace(',', '.'));
            // "NaN" e "Infinity" se parsean sin error pero rompen las
            // comparaciones (un precio NaN nunca es > 0: el tradeo seria gratis).
            return Double.isFinite(value) ? Double.valueOf(value) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
