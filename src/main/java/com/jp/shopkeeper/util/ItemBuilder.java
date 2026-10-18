package com.jp.shopkeeper.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Construccion de items con nombre, lore y brillo, sin Adventure
 * (la 1.8 no lo soporta): usa el API legacy presente en todas las
 * versiones. El brillo se logra con un encantamiento oculto cuyo
 * nombre se resuelve por reflexion (DURABILITY en 1.8, UNBREAKING en 1.13+).
 */
public final class ItemBuilder {

    private final ItemStack item;
    private String name;
    private final List<String> lore = new ArrayList<String>();
    private boolean glow;
    private int customModelData = -1;

    private static Object glowCache;
    private static boolean glowResolved;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(ItemStack item) {
        if (item == null) {
            return null;
        }
        return new ItemBuilder(item.clone());
    }

    /** Crea un builder desde un nombre de material (multi-version). Null si no existe. */
    public static ItemBuilder of(String materialName, int amount) {
        ItemStack stack = Materials.stack(materialName, amount);
        if (stack == null) {
            return null;
        }
        return new ItemBuilder(stack);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Numbers.clamp(amount, 1, 64));
        return this;
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (lines != null) {
            this.lore.addAll(lines);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (lines != null) {
            for (String line : lines) {
                this.lore.add(line);
            }
        }
        return this;
    }

    public ItemBuilder glow() {
        this.glow = true;
        return this;
    }

    /** Modelo personalizado del resource pack (1.14+); en versiones antiguas se ignora. */
    public ItemBuilder customModelData(int value) {
        this.customModelData = value;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(Text.color(name));
            }
            if (!lore.isEmpty()) {
                meta.setLore(Text.color(lore));
            }
            if (customModelData > 0) {
                // 1.14+: texturas de resource pack. Si el metodo no existe,
                // Reflect devuelve null y el icono se queda como estaba.
                Reflect.call(meta, "setCustomModelData", Integer.valueOf(customModelData));
            }
            item.setItemMeta(meta);
        }
        if (glow) {
            glint(item);
        }
        return item;
    }

    /** Anade brillo a un item existente con un encantamiento oculto. */
    public static void glint(ItemStack item) {
        if (item == null) {
            return;
        }
        Object enchantment = glowEnchantment();
        if (enchantment == null) {
            return;
        }
        try {
            item.addUnsafeEnchantment((Enchantment) enchantment, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // sin brillo posible en esta version
        }
    }

    private static Object glowEnchantment() {
        if (glowResolved) {
            return glowCache;
        }
        glowResolved = true;
        glowCache = findEnchantment("DURABILITY");
        if (glowCache == null) {
            glowCache = findEnchantment("UNBREAKING");
        }
        return glowCache;
    }

    private static Object findEnchantment(String name) {
        try {
            Method getByName = Enchantment.class.getMethod("getByName", String.class);
            Object result = getByName.invoke(null, name);
            if (result != null) {
                return result;
            }
        } catch (Throwable ignored) {
            // seguimos con campos
        }
        try {
            Field field = Enchantment.class.getField(name);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
