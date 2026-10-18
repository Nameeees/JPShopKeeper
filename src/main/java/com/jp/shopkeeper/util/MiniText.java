package com.jp.shopkeeper.util;

/**
 * Soporte opcional de MiniMessage (Adventure, incluido en Paper 1.16+).
 *
 * Se activa con settings.mini-message en config.yml. Todo se resuelve por
 * reflexion: en Spigot (o en versiones sin Adventure) simplemente no esta
 * disponible y el plugin sigue usando los colores legacy de siempre.
 */
public final class MiniText {

    private static boolean enabled;
    private static boolean resolved;
    private static Object miniMessage;
    private static Object legacySerializer;
    private static Class<?> componentClass;
    private static java.lang.reflect.Method deserialize;
    private static java.lang.reflect.Method serialize;

    private MiniText() {
    }

    /** Lo llama ConfigManager al cargar o recargar la configuracion. */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** true si el servidor trae Adventure (Paper 1.16+). */
    public static boolean isAvailable() {
        resolve();
        return miniMessage != null && legacySerializer != null && componentClass != null;
    }

    /**
     * Convierte un texto MiniMessage en texto con formato legacy (con §).
     * Devuelve null si el soporte esta apagado, no hay Adventure o el texto
     * no es MiniMessage valido: en ese caso se usa el texto tal cual.
     */
    public static String render(String input) {
        if (!enabled || input == null || input.indexOf('<') < 0) {
            return null;
        }
        resolve();
        if (miniMessage == null || legacySerializer == null || componentClass == null
                || deserialize == null || serialize == null) {
            return null;
        }
        try {
            Object component = deserialize.invoke(miniMessage, input);
            Object legacy = serialize.invoke(legacySerializer, component);
            return legacy instanceof String ? (String) legacy : null;
        } catch (Throwable ignored) {
            // etiqueta mal escrita: mejor mostrar el texto original que perderlo
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> miniClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Object mini = miniClass.getMethod("miniMessage").invoke(null);
            componentClass = Class.forName("net.kyori.adventure.text.Component");
            // Las implementaciones de Adventure no son clases publicas: hay que
            // abrir el metodo o invoke() lanza IllegalAccessException.
            deserialize = mini.getClass().getMethod("deserialize", String.class);
            deserialize.setAccessible(true);

            Class<?> serializerClass = Class.forName(
                    "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Object legacy = serializerClass.getMethod("legacySection").invoke(null);
            serialize = legacy.getClass().getMethod("serialize", componentClass);
            serialize.setAccessible(true);

            miniMessage = mini;
            legacySerializer = legacy;
        } catch (Throwable ignored) {
            miniMessage = null;
            legacySerializer = null;
            componentClass = null;
            deserialize = null;
            serialize = null;
        }
    }
}
