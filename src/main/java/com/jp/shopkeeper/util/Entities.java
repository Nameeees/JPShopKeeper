package com.jp.shopkeeper.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolucion de tipos de entidad y extras de NPC compatibles
 * 1.8 - ultima version. Algunos nombres cambiaron con los anos
 * (SNOWMAN -> SNOW_GOLEM, PIG_ZOMBIE -> ZOMBIFIED_PIGLIN) y las
 * profesiones de aldeano se renombraron en 1.14: aqui se traducen
 * ambos sentidos y nunca se referencia una constante directa.
 */
public final class Entities {

    private static final Map<String, String> TYPE_ALIASES = new HashMap<String, String>();
    private static final Map<String, String> PROFESSION_ALIASES = new HashMap<String, String>();

    static {
        bidirectional(TYPE_ALIASES, "SNOWMAN", "SNOW_GOLEM");
        bidirectional(TYPE_ALIASES, "PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
        bidirectional(TYPE_ALIASES, "MUSHROOM_COW", "MOOSHROOM");
        bidirectional(PROFESSION_ALIASES, "PRIEST", "CLERIC");
        bidirectional(PROFESSION_ALIASES, "BLACKSMITH", "TOOLSMITH");
        bidirectional(PROFESSION_ALIASES, "BLACKSMITH", "WEAPONSMITH");
    }

    private Entities() {
    }

    private static void bidirectional(Map<String, String> map, String a, String b) {
        map.put(a, b);
        map.put(b, a);
    }

    /** Resuelve un EntityType por nombre (o alias); null si no existe. */
    public static EntityType type(String raw) {
        if (raw == null) {
            return null;
        }
        String name = Materials.normalize(raw);
        if (name.isEmpty()) {
            return null;
        }
        EntityType direct = valueOf(name);
        if (direct != null) {
            return direct;
        }
        String alias = TYPE_ALIASES.get(name);
        if (alias != null) {
            EntityType aliased = valueOf(alias);
            if (aliased != null) {
                return aliased;
            }
        }
        try {
            return EntityType.fromName(name.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Aplica la profesion a un aldeano (solo estetica). Nombres antiguos
     * y modernos se traducen; si la version no soporta algo, se ignora.
     *
     * @return true si se aplico alguna profesion
     */
    public static boolean applyProfession(Entity entity, String profession) {
        if (entity == null || profession == null || profession.trim().isEmpty()) {
            return false;
        }
        String name = Materials.normalize(profession);
        Object value = professionValue(name);
        if (value == null) {
            String alias = PROFESSION_ALIASES.get(name);
            if (alias != null) {
                value = professionValue(alias);
            }
        }
        if (value == null) {
            return false;
        }
        return Reflect.call(entity, "setProfession", value) != null;
    }

    private static Object professionValue(String name) {
        try {
            Class<?> professionClass = Class.forName("org.bukkit.entity.Villager$Profession");
            try {
                Method valueOf = professionClass.getMethod("valueOf", String.class);
                Object result = valueOf.invoke(null, name);
                if (result != null) {
                    return result;
                }
            } catch (Throwable ignored) {
                // la version convirtio el enum en otra cosa: probamos campos
            }
            try {
                Field field = professionClass.getField(name);
                return field.get(null);
            } catch (Throwable ignored) {
                return null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static EntityType valueOf(String name) {
        try {
            return EntityType.valueOf(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
