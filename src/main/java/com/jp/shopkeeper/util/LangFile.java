package com.jp.shopkeeper.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;

/**
 * Resuelve el archivo de textos del idioma activo.
 *
 * Orden de busqueda:
 *   1. &lt;base&gt;_&lt;idioma&gt;.yml en la carpeta del plugin (se extrae del JAR
 *      la primera vez si viene empaquetado: messages_en.yml, menus_en.yml...).
 *   2. &lt;base&gt;.yml, el archivo base, que ademas se usa como respaldo por
 *      clave: lo que falte en una traduccion se toma de ahi.
 *
 * El archivo base se extrae siempre (aunque el idioma activo sea otro) para
 * que el respaldo exista en disco y el administrador tenga una referencia.
 * Anadir un idioma nuevo es solo copiar los textos a textos_XX.yml.
 */
public final class LangFile {

    private LangFile() {
    }

    /** Archivo del idioma activo; nunca devuelve null. */
    public static File resolve(JavaPlugin plugin, String base, String language) {
        File baseFile = baseFile(plugin, base);
        if (language != null && !language.isEmpty()) {
            String localized = base + "_" + language + ".yml";
            File file = new File(plugin.getDataFolder(), localized);
            if (!file.isFile()) {
                copyIfPackaged(plugin, localized);
            }
            if (file.isFile()) {
                return file;
            }
        }
        return baseFile;
    }

    /** Archivo base (messages.yml, menus.yml), extraido si falta. */
    public static File baseFile(JavaPlugin plugin, String base) {
        String name = base + ".yml";
        File file = new File(plugin.getDataFolder(), name);
        if (!file.isFile()) {
            copyIfPackaged(plugin, name);
        }
        return file;
    }

    /** Extrae un recurso del JAR si existe (y si se puede escribir). */
    private static void copyIfPackaged(JavaPlugin plugin, String resource) {
        InputStream in = null;
        try {
            in = plugin.getResource(resource);
            if (in == null) {
                return;
            }
            in.close();
            in = null;
            plugin.saveResource(resource, false);
        } catch (Throwable ignored) {
            // recurso ausente o carpeta sin permisos: se usara el archivo base
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                    // nada que hacer
                }
            }
        }
    }
}
