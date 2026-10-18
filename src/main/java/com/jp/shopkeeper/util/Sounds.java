package com.jp.shopkeeper.util;

import com.jp.shopkeeper.JPShopKeeper;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sonidos por clave semantica, compatibles 1.8 - ultima version.
 *
 * Los nombres de sonido cambiaron por completo en 1.9 (CLICK ->
 * UI_BUTTON_CLICK, LEVEL_UP -> ENTITY_PLAYER_LEVELUP...) y en versiones
 * recientes Sound paso de enum a interfaz. Estrategia en cascada:
 *  1) Sound.valueOf(nombre) por reflexion
 *  2) campo estatico Sound.NOMBRE por reflexion (Paper moderno)
 *  3) playSound con nombre de recurso ("ui.button.click") como ultimo recurso
 */
public final class Sounds {

    private static final Object STRING_MODE = new Object();

    private final JPShopKeeper plugin;
    private final Map<String, List<String>> candidates = new HashMap<String, List<String>>();
    private final Map<String, Object> cache = new HashMap<String, Object>();

    private boolean enabled;
    private float volume;
    private float pitch;

    public Sounds(JPShopKeeper plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        candidates.clear();
        cache.clear();
        enabled = plugin.getConfig().getBoolean("sounds.enabled", true);
        volume = (float) plugin.getConfig().getDouble("sounds.volume", 0.7);
        pitch = (float) plugin.getConfig().getDouble("sounds.pitch", 1.0);
        candidates.put("open", read("sounds.open", "open"));
        candidates.put("success", read("sounds.success", "success"));
        candidates.put("error", read("sounds.error", "error"));
        candidates.put("set", read("sounds.set", "set"));
        candidates.put("remove", read("sounds.remove", "remove"));
    }

    private List<String> read(String path, String key) {
        List<String> list = plugin.getConfig().getStringList(path);
        if (list == null || list.isEmpty()) {
            list = defaults(key);
        }
        return list;
    }

    /** Reproduce un sonido semantico: open, success, error, set, remove. */
    public void play(Player player, String key) {
        if (!enabled || player == null) {
            return;
        }
        Object resolved = resolve(key);
        if (resolved == null) {
            return;
        }
        try {
            if (resolved == STRING_MODE) {
                List<String> names = candidates.get(key);
                if (names == null || names.isEmpty()) {
                    return;
                }
                String resource = names.get(0).toLowerCase(Locale.ROOT).replace('_', '.');
                player.playSound(player.getLocation(), resource, volume, pitch);
            } else {
                player.playSound(player.getLocation(), (Sound) resolved, volume, pitch);
            }
        } catch (Throwable ignored) {
            // sonido no disponible en esta version: silencio, nunca romper el flujo
        }
    }

    private Object resolve(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Object found = null;
        List<String> names = candidates.get(key);
        if (names != null) {
            for (String name : names) {
                Object sound = findSound(name);
                if (sound != null) {
                    found = sound;
                    break;
                }
            }
        }
        if (found == null) {
            found = STRING_MODE;
        }
        cache.put(key, found);
        return found;
    }

    private Object findSound(String name) {
        try {
            Method valueOf = Sound.class.getMethod("valueOf", String.class);
            Object result = valueOf.invoke(null, name);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
            // sin valueOf: probamos campos (Sound como interfaz en Paper reciente)
        }
        try {
            Field field = Sound.class.getField(name);
            Object result = field.get(null);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
            // tampoco hay campo con ese nombre
        }
        return null;
    }

    private List<String> defaults(String key) {
        if ("open".equals(key)) {
            return new ArrayList<String>(Arrays.asList("UI_BUTTON_CLICK", "CLICK"));
        }
        if ("success".equals(key)) {
            return new ArrayList<String>(Arrays.asList("ENTITY_PLAYER_LEVELUP", "LEVEL_UP"));
        }
        if ("error".equals(key)) {
            return new ArrayList<String>(Arrays.asList("ENTITY_VILLAGER_NO", "VILLAGER_NO"));
        }
        if ("set".equals(key)) {
            return new ArrayList<String>(Arrays.asList("ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"));
        }
        return new ArrayList<String>(Arrays.asList("ENTITY_VILLAGER_YES", "VILLAGER_YES"));
    }
}
