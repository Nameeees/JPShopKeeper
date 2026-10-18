package com.jp.shopkeeper.util;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invocacion de metodos por reflexion "a prueba de versiones":
 * si el metodo no existe en la version del servidor, devuelve null
 * en vez de lanzar error. Base de toda la compatibilidad 1.8 - ultima.
 */
public final class Reflect {

    /** Cache de metodos ya resueltos: getMethods() es caro y se llamaba por entidad y por tick. */
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<String, Method>();
    private static final Set<String> MISSING =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private Reflect() {
    }

    /** Llama a un metodo por nombre con los argumentos dados.
     *  Devuelve el resultado; Boolean.TRUE cuando el metodo es void y
     *  funciono; null si el metodo no existe o fallo. */
    public static Object call(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        Object[] safeArgs = args == null ? new Object[0] : args;
        String key = cacheKey(target.getClass(), methodName, safeArgs);
        if (MISSING.contains(key)) {
            return null;
        }
        Method method = METHODS.get(key);
        if (method == null) {
            method = find(target.getClass(), methodName, safeArgs);
            if (method == null) {
                MISSING.add(key);
                return null;
            }
            METHODS.put(key, method);
        }
        try {
            Object result = method.invoke(target, safeArgs);
            return result == null ? Boolean.TRUE : result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method find(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length == args.length && compatible(types, args)) {
                return method;
            }
        }
        return null;
    }

    private static String cacheKey(Class<?> type, String methodName, Object[] args) {
        StringBuilder key = new StringBuilder(type.getName()).append('#').append(methodName);
        for (Object arg : args) {
            key.append('/').append(arg == null ? "null" : arg.getClass().getName());
        }
        return key.toString();
    }

    private static boolean compatible(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            Object value = args[i];
            if (value == null) {
                if (types[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (!wrap(types[i]).isInstance(value)) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
