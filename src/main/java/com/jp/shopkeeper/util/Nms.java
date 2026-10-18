package com.jp.shopkeeper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/**
 * Puente NMS minimo y por reflexion para lo que el API de Bukkit no ofrecia
 * en 1.8 (esa version no tiene setAI ni setSilent en el API).
 *
 * Orden de intentos:
 *  1. NMS con nombre legible: EntityInsentient.setNoAI (1.9-1.16) y
 *     Mob.setNoAi (1.17+, mappings Mojang) / Entity.setSilent.
 *  2. 1.8: en ese NMS no existe ninguno de los dos (comprobado con javap
 *     sobre patched_1.8.8.jar), asi que se escriben las etiquetas NoAI y
 *     Silent en el NBT del mob y se vuelve a cargar (Entity.f).
 *
 * Nunca hay referencias directas a clases NMS: si algo no existe en la
 * version actual, el intento devuelve false y el plugin sigue igual.
 */
public final class Nms {

    private static final String NBT_TAG_CLASS = "NBTTagCompound";

    private Nms() {
    }

    /** Objeto NMS detras de una entidad Bukkit (CraftEntity#getHandle). */
    public static Object handle(Entity entity) {
        if (entity == null) {
            return null;
        }
        try {
            return entity.getClass().getMethod("getHandle").invoke(entity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Desactiva IA y sonido con todos los medios disponibles. */
    public static void applyPeaceful(Entity entity, boolean noAi, boolean silent) {
        if (noAi) {
            noAi(entity);
        }
        if (silent) {
            silent(entity, true);
        }
    }

    /** Intenta desactivar la IA del mob; true si la version lo acepto. */
    public static boolean noAi(Entity entity) {
        Object handle = handle(entity);
        if (handle == null) {
            return false;
        }
        if (Reflect.call(handle, "setNoAI", Boolean.TRUE) != null) {
            return true;
        }
        if (Reflect.call(handle, "setNoAi", Boolean.TRUE) != null) {
            return true;
        }
        return applyNbtFlags(entity, Boolean.TRUE, null);
    }

    /** Intenta silenciar el mob; true si la version lo acepto. */
    public static boolean silent(Entity entity, boolean silent) {
        Object handle = handle(entity);
        if (handle == null) {
            return false;
        }
        if (Reflect.call(handle, "setSilent", silent) != null) {
            return true;
        }
        return applyNbtFlags(entity, null, Boolean.valueOf(silent));
    }

    /**
     * 1.8: escribe NoAI / Silent en el NBT del mob y lo vuelve a cargar.
     * Devuelve true si pudo aplicar y releer el valor.
     */
    private static boolean applyNbtFlags(Entity entity, Boolean noAi, Boolean silent) {
        Object handle = handle(entity);
        if (handle == null) {
            return false;
        }
        try {
            Class<?> tagClass = nbtTagClass();
            if (tagClass == null) {
                return false;
            }
            Object tag = tagClass.newInstance();
            Method save = handle.getClass().getMethod("e", tagClass);
            Method load = handle.getClass().getMethod("f", tagClass);
            Method set = tagClass.getMethod("setBoolean", String.class, boolean.class);
            save.invoke(handle, tag);
            if (noAi != null) {
                set.invoke(tag, "NoAI", noAi.booleanValue());
            }
            if (silent != null) {
                set.invoke(tag, "Silent", silent.booleanValue());
            }
            load.invoke(handle, tag);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Lee una etiqueta booleana del NBT del mob (null si no se pudo). */
    public static Boolean nbtFlag(Entity entity, String key) {
        Object handle = handle(entity);
        if (handle == null) {
            return null;
        }
        try {
            Class<?> tagClass = nbtTagClass();
            if (tagClass == null) {
                return null;
            }
            Object tag = tagClass.newInstance();
            handle.getClass().getMethod("e", tagClass).invoke(handle, tag);
            Object value = tagClass.getMethod("getBoolean", String.class).invoke(tag, key);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** net.minecraft.server.<version>.NBTTagCompound de esta version. */
    private static Class<?> nbtTagClass() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.substring(packageName.lastIndexOf('.') + 1);
            return Class.forName("net.minecraft.server." + version + "." + NBT_TAG_CLASS);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
