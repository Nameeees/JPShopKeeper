package com.jp.shopkeeper.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Base64;

/**
 * Compatibilidad total con NBT: guarda, restaura y compara el NBT crudo
 * de los items (el que usan plugins de tokens, vouchers, items custom...).
 *
 * Estrategia multicapa a prueba de versiones:
 *  - 1.16.4+ : API de Bukkit (ItemStack#serializeAsBytes/deserializeBytes).
 *  - 1.8-1.16: NMS por reflexion (NBTTagCompound + NBTCompressedStreamTools,
 *    tolerando las distintas firmas de a() entre versiones).
 *  - Comparacion de NBT: tag NMS sin "display" ni "Enchantments" (esos los
 *    cubren los flags de config.yml: nombre, lore, encantamientos).
 *
 * Cada pieza se resuelve por separado: si una falla, las demas siguen.
 * Si no hay NBT disponible, toBase64 devuelve null y el plugin usa el
 * guardado clasico sin romperse.
 */
public final class NbtUtil {

    private NbtUtil() {
    }

    private static boolean resolved;
    private static boolean nbtAvailable;

    private static Method bukkitSerializeBytes;    // ItemStack#serializeAsBytes()
    private static Method bukkitDeserializeBytes;  // ItemStack#deserializeBytes(byte[])
    private static Method asNmsCopy;               // CraftItemStack.asNMSCopy(ItemStack)
    private static Method asCraftMirror;           // CraftItemStack.asCraftMirror(nmsItem)
    private static Method legacySave;              // nmsItem.save(tagNbt)
    private static Method legacyWrite;             // NBTCompressedStreamTools.a(tag, DataOutputStream)
    private static Method legacyRead;              // NBTCompressedStreamTools.a(DataInputStream)
    private static Method legacyLoadNbt;           // instance nms.c(tagNbt)  (carga en 1.8)
    private static Method legacyLoadStatic;        // static nms.a(tagNbt) -> ItemStack (otras versiones)
    private static Constructor<?> legacyTagCtor;   // new NBTTagCompound()
    private static Constructor<?> legacyItemCtor;  // new ItemStack(tagNbt) (1.9+; en 1.8 no existe)

    private static Class<?> tagClass;
    private static Method tagGetTag;               // nmsItem.getTag()
    private static Method tagClone;                // tag.clone()
    private static Method tagRemove;               // tag.remove(String)
    private static Method tagIsEmpty;              // tag.isEmpty()

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        // API moderna de Bukkit
        bukkitSerializeBytes = tryMethod(ItemStack.class, "serializeAsBytes");
        bukkitDeserializeBytes = tryMethod(ItemStack.class, "deserializeBytes", byte[].class);

        String craftPackage = null;
        try {
            craftPackage = Bukkit.getServer().getClass().getPackage().getName();
        } catch (Throwable ignored) {
            // sin servidor: solo queda la via de Bukkit (si existe)
        }

        if (craftPackage != null) {
            // versionTag sin literales fragiles (Paper remapea nombres de clases):
            // "org.bukkit.craftbukkit.v1_8_R3" -> "v1_8_R3"; en moderno -> ""
            String suffix = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            String versionTag = (suffix.length() > 1 && suffix.charAt(0) == 'v' && suffix.indexOf('_') > 0)
                    ? suffix : "";
            Class<?> craftItemStack = tryClass(craftPackage + ".inventory.CraftItemStack");
            Class<?> nmsItemClass = tryClass(versionTag.isEmpty()
                    ? "net.minecraft.world.item.ItemStack"                     // 1.17+ (mappings Mojang)
                    : "net.minecraft.server." + versionTag + ".ItemStack");    // 1.8 - 1.16

            if (craftItemStack != null) {
                asNmsCopy = tryMethod(craftItemStack, "asNMSCopy", ItemStack.class);
                if (nmsItemClass != null) {
                    asCraftMirror = tryMethod(craftItemStack, "asCraftMirror", nmsItemClass);
                }
            }

            if (!versionTag.isEmpty() && nmsItemClass != null) {
                Class<?> tagNbtClass = tryClass("net.minecraft.server." + versionTag + ".NBTTagCompound");
                Class<?> streamTools = tryClass("net.minecraft.server." + versionTag + ".NBTCompressedStreamTools");
                if (tagNbtClass != null) {
                    legacyTagCtor = tryConstructor(tagNbtClass);
                    legacySave = tryMethod(nmsItemClass, "save", tagNbtClass);
                    legacyItemCtor = tryConstructor(nmsItemClass, tagNbtClass);
                    legacyLoadNbt = tryMethod(nmsItemClass, "c", tagNbtClass);
                    if (legacyLoadNbt == null) {
                        legacyLoadNbt = findInstanceLoader(nmsItemClass, tagNbtClass);
                    }
                    legacyLoadStatic = findStaticLoader(nmsItemClass, tagNbtClass);
                }
                if (streamTools != null && tagNbtClass != null) {
                    // getMethods segun version: a(tag, DataOutput|OutputStream) y
                    // a(DataInputStream|InputStream|(DataInput, NBTReadLimiter)).
                    legacyWrite = findStaticAssignable(streamTools, "a", tagNbtClass, DataOutputStream.class);
                    legacyRead = findStaticAssignable(streamTools, "a", DataInputStream.class);
                }
            }
        }

        nbtAvailable = bukkitSerializeBytes != null
                || (asNmsCopy != null && legacySave != null && legacyWrite != null
                && legacyRead != null && legacyTagCtor != null
                && (legacyItemCtor != null || legacyLoadNbt != null || legacyLoadStatic != null));
    }

    private static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method tryMethod(Class<?> owner, String name, Class<?>... parameters) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, parameters);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Constructor<?> tryConstructor(Class<?> owner, Class<?>... parameters) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(parameters);
        } catch (Throwable ignored) {
            // algunos constructores NMS no son publicos: se busca el declarado
        }
        try {
            Constructor<?> constructor = owner.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return constructor;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Busca un metodo estatico por nombre que acepte los tipos indicados
     * (los parametros reales pueden ser una superclase/superinterface).
     */
    private static Method findStaticAssignable(Class<?> owner, String name, Class<?>... passedTypes) {
        if (owner == null) {
            return null;
        }
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name) || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length != passedTypes.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < types.length; i++) {
                if (!wrap(types[i]).isAssignableFrom(wrap(passedTypes[i]))) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    /** Busca un metodo de instancia void con un unico parametro del tipo indicado (carga NBT). */
    private static Method findInstanceLoader(Class<?> owner, Class<?> tagClass) {
        for (Method method : owner.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].equals(tagClass) || method.getReturnType() != void.class) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // sigue buscando
            }
        }
        return null;
    }

    /** Busca un metodo estatico que construya un item desde el tag (otras versiones). */
    private static Method findStaticLoader(Class<?> owner, Class<?> tagClass) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].equals(tagClass)) {
                continue;
            }
            if (!owner.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // sigue buscando
            }
        }
        return null;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }

    /** true si esta version soporta NBT (cualquier capa). */
    public static boolean isAvailable() {
        resolve();
        return nbtAvailable;
    }

    /**
     * Serializa un item COMPLETO (incluido NBT crudo) a base64.
     * Devuelve null si no se pudo (el llamador usa el guardado clasico).
     */
    public static String toBase64(ItemStack item) {
        if (item == null) {
            return null;
        }
        resolve();
        if (bukkitSerializeBytes != null) {
            try {
                Object bytes = bukkitSerializeBytes.invoke(item);
                if (bytes instanceof byte[]) {
                    return Base64.getEncoder().encodeToString((byte[]) bytes);
                }
            } catch (Throwable ignored) {
                // se intenta la via NMS
            }
        }
        try {
            if (asNmsCopy == null || legacySave == null || legacyWrite == null || legacyTagCtor == null) {
                return null;
            }
            Object nms = asNmsCopy.invoke(null, item);
            if (nms == null) {
                return null;
            }
            Object tag = legacyTagCtor.newInstance();
            legacySave.invoke(nms, tag);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(buffer);
            legacyWrite.invoke(null, tag, output);
            output.flush();
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Restaura un item completo desde base64; null si no se pudo. */
    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        resolve();
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
        } catch (Throwable ignored) {
            return null;
        }
        if (bukkitDeserializeBytes != null) {
            try {
                Object item = bukkitDeserializeBytes.invoke(null, (Object) bytes);
                if (item instanceof ItemStack) {
                    return (ItemStack) item;
                }
            } catch (Throwable ignored) {
                // se intenta la via NMS
            }
        }
        try {
            if (legacyRead == null || asCraftMirror == null) {
                return null;
            }
            Object tag = legacyRead.invoke(null, new DataInputStream(new ByteArrayInputStream(bytes)));
            if (tag == null) {
                return null;
            }
            Object nms = null;
            if (legacyItemCtor != null) {
                // 1.9+: constructor directo desde NBT
                nms = legacyItemCtor.newInstance(tag);
            } else if (legacyLoadStatic != null) {
                // otras versiones: metodo estatico que construye desde el tag
                nms = legacyLoadStatic.invoke(null, tag);
            } else if (legacyLoadNbt != null && asNmsCopy != null) {
                // 1.8: no existe constructor; se carga el NBT en un item vacio
                nms = asNmsCopy.invoke(null, new ItemStack(org.bukkit.Material.AIR));
                if (nms != null) {
                    legacyLoadNbt.invoke(nms, tag);
                }
            }
            if (nms == null) {
                return null;
            }
            Object item = asCraftMirror.invoke(null, nms);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    //  Comparacion de NBT (para exigir el mismo item al cobrar)
    // ------------------------------------------------------------------

    /**
     * true si el item tiene NBT/datos propios de otros plugins
     * (mas alla de nombre/lore/encantamientos, que ya cubren los flags).
     * Cubre: NBT crudo (1.8-1.20.4) y PDC/CustomModelData (1.14+).
     */
    public static boolean hasCustomNbt(ItemStack item) {
        Object tag = normalizedTag(item);
        if (tag != null && !isTagEmpty(tag)) {
            return true;
        }
        return pdcHasContent(item);
    }

    /**
     * Compara el NBT "custom" de dos items.
     * Regla: si el item requerido NO tiene datos propios, no se exige nada
     * (lo cubren los demas flags). Si los tiene, el otro debe coincidir al 100%.
     */
    public static boolean customNbtMatches(ItemStack required, ItemStack actual) {
        Object requiredTag = normalizedTag(required);
        boolean requiredHasTag = requiredTag != null && !isTagEmpty(requiredTag);
        String requiredSignature = pdcHasContent(required) ? pdcSignature(required) : null;
        if (!requiredHasTag && requiredSignature == null) {
            return true;
        }
        if (requiredHasTag) {
            Object actualTag = normalizedTag(actual);
            if (actualTag == null || !requiredTag.equals(actualTag)) {
                return false;
            }
        }
        if (requiredSignature != null) {
            String actualSignature = pdcSignature(actual);
            if (actualSignature == null || !requiredSignature.equals(actualSignature)) {
                return false;
            }
        }
        return true;
    }

    /**
     * true si conviene guardar el blob NBT del item: siempre en versiones
     * modernas (fidelidad de componentes) o cuando el item tiene datos custom.
     */
    public static boolean shouldStoreBlob(ItemStack item) {
        resolve();
        return bukkitSerializeBytes != null || hasCustomNbt(item);
    }

    // ------------------------------------------------------------------
    //  Capa PDC / CustomModelData (1.14+): datos custom sin getTag
    // ------------------------------------------------------------------

    private static boolean pdcResolved;
    private static Method metaGetPdc;
    private static Method pdcIsEmpty;
    private static Method pdcGetKeys;
    private static Method pdcGet;
    private static Object[] pdcTypes;
    private static Method metaHasCmd;
    private static Method metaGetCmd;

    private static void resolvePdc() {
        if (pdcResolved) {
            return;
        }
        pdcResolved = true;
        try {
            Class<?> metaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
            metaGetPdc = metaClass.getMethod("getPersistentDataContainer");
            Class<?> pdcClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            pdcIsEmpty = pdcClass.getMethod("isEmpty");
            pdcGetKeys = pdcClass.getMethod("getKeys");
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            pdcGet = pdcClass.getMethod("get", Class.forName("org.bukkit.NamespacedKey"), typeClass);
            java.util.List<Object> types = new java.util.ArrayList<Object>();
            String[] names = {"STRING", "INTEGER", "LONG", "DOUBLE", "FLOAT", "SHORT", "BYTE",
                    "BYTE_ARRAY", "INTEGER_ARRAY", "LONG_ARRAY", "TAG_CONTAINER"};
            for (String name : names) {
                try {
                    types.add(typeClass.getField(name).get(null));
                } catch (Throwable ignored) {
                    // tipo no disponible en esta version
                }
            }
            pdcTypes = types.toArray();
        } catch (Throwable ignored) {
            metaGetPdc = null;
        }
        try {
            Class<?> metaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
            metaHasCmd = metaClass.getMethod("hasCustomModelData");
            metaGetCmd = metaClass.getMethod("getCustomModelData");
        } catch (Throwable ignored) {
            metaHasCmd = null;
            metaGetCmd = null;
        }
    }

    private static Object persistentContainer(ItemStack item) {
        resolvePdc();
        if (item == null || metaGetPdc == null) {
            return null;
        }
        try {
            Object meta = item.getItemMeta();
            return meta == null ? null : metaGetPdc.invoke(meta);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean pdcHasContent(ItemStack item) {
        Object pdc = persistentContainer(item);
        if (pdc != null && pdcIsEmpty != null) {
            try {
                Object empty = pdcIsEmpty.invoke(pdc);
                if (empty instanceof Boolean && !(Boolean) empty) {
                    return true;
                }
            } catch (Throwable ignored) {
                // se sigue con CustomModelData
            }
        }
        try {
            if (metaHasCmd != null && item != null) {
                Object meta = item.getItemMeta();
                if (meta != null) {
                    Object has = metaHasCmd.invoke(meta);
                    if (has instanceof Boolean && (Boolean) has) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // sin CMD disponible
        }
        return false;
    }

    /** Firma determinista del contenido custom (PDC + CustomModelData). */
    private static String pdcSignature(ItemStack item) {
        resolvePdc();
        StringBuilder out = new StringBuilder();
        try {
            if (metaHasCmd != null && item != null) {
                Object meta = item.getItemMeta();
                if (meta != null) {
                    Object has = metaHasCmd.invoke(meta);
                    if (has instanceof Boolean && (Boolean) has) {
                        out.append("cmd=").append(metaGetCmd.invoke(meta)).append(';');
                    }
                }
            }
        } catch (Throwable ignored) {
            // sin CMD
        }
        Object pdc = persistentContainer(item);
        if (pdc != null && pdcGetKeys != null && pdcGet != null && pdcTypes != null) {
            try {
                Object keysObject = pdcGetKeys.invoke(pdc);
                if (keysObject instanceof java.util.Set) {
                    java.util.List<String> entries = new java.util.ArrayList<String>();
                    for (Object key : (java.util.Set<?>) keysObject) {
                        for (Object type : pdcTypes) {
                            try {
                                Object value = pdcGet.invoke(pdc, key, type);
                                if (value != null) {
                                    entries.add(String.valueOf(key) + "=" + signatureOf(value) + ";");
                                    break;
                                }
                            } catch (Throwable ignored) {
                                // tipo incorrecto para esta clave
                            }
                        }
                    }
                    java.util.Collections.sort(entries);
                    for (String entry : entries) {
                        out.append(entry);
                    }
                }
            } catch (Throwable ignored) {
                // PDC ilegible en esta version
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    /**
     * Representacion estable de un valor del PDC. String.valueOf de un array
     * devuelve "[B@1a2b3c" (identidad, distinta en cada item y arranque): dos
     * items identicos con un array en el PDC nunca coincidirian.
     */
    private static String signatureOf(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof byte[]) {
            return java.util.Arrays.toString((byte[]) value);
        }
        if (value instanceof int[]) {
            return java.util.Arrays.toString((int[]) value);
        }
        if (value instanceof long[]) {
            return java.util.Arrays.toString((long[]) value);
        }
        if (value instanceof Object[]) {
            return java.util.Arrays.deepToString((Object[]) value);
        }
        return String.valueOf(value);
    }

    private static Object normalizedTag(ItemStack item) {
        if (item == null) {
            return null;
        }
        resolve();
        Object nms = nmsCopy(item);
        if (nms == null) {
            return null;
        }
        Object tag = getTag(nms);
        if (tag == null) {
            return null;
        }
        Object copy = cloneTag(tag);
        if (copy == null) {
            return null;
        }
        // Las claves de los datos que ya comparan los flags de config.yml:
        // "ench" en 1.8-1.12, "Enchantments" desde 1.13. En 1.8 el nombre
        // moderno no existe, asi que un item encantado se colaba como "NBT
        // propio" y obligaba a un NBT identico al 100% (incluido RepairCost).
        removeTag(copy, "display");
        removeTag(copy, "Enchantments");
        removeTag(copy, "ench");
        removeTag(copy, "RepairCost");
        // La durabilidad la decide match.durability en config.yml: si el tag
        // "Damage" se queda, ese flag deja de servir para cualquier item gastado.
        removeTag(copy, "Damage");
        return copy;
    }

    private static Object nmsCopy(ItemStack item) {
        if (asNmsCopy == null) {
            return null;
        }
        try {
            return asNmsCopy.invoke(null, item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getTag(Object nms) {
        try {
            if (tagGetTag == null || !tagGetTag.getDeclaringClass().isInstance(nms)) {
                tagGetTag = nms.getClass().getMethod("getTag");
            }
            return tagGetTag.invoke(nms);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object cloneTag(Object tag) {
        try {
            resolveTagMethods(tag);
            return tagClone == null ? null : tagClone.invoke(tag);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void removeTag(Object tag, String key) {
        try {
            resolveTagMethods(tag);
            if (tagRemove != null) {
                tagRemove.invoke(tag, key);
            }
        } catch (Throwable ignored) {
            // clave ausente: nada que quitar
        }
    }

    private static boolean isTagEmpty(Object tag) {
        try {
            resolveTagMethods(tag);
            if (tagIsEmpty == null) {
                return true;
            }
            Object result = tagIsEmpty.invoke(tag);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void resolveTagMethods(Object tag) {
        if (tag == null || tag.getClass().equals(tagClass)) {
            return;
        }
        tagClass = tag.getClass();
        tagClone = null;
        tagRemove = null;
        tagIsEmpty = null;
        try {
            tagClone = tagClass.getMethod("clone");
        } catch (Throwable ignored) {
            tagClone = null;
        }
        try {
            tagRemove = tagClass.getMethod("remove", String.class);
        } catch (Throwable ignored) {
            tagRemove = null;
        }
        try {
            tagIsEmpty = tagClass.getMethod("isEmpty");
        } catch (Throwable ignored) {
            tagIsEmpty = null;
        }
    }
}
