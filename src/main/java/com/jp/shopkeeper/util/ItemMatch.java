package com.jp.shopkeeper.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Comparacion, conteo y retirada de items para los costes de los
 * tradeos. Las reglas (nombre, lore, encantamientos, durabilidad)
 * salen de config.yml para que cada servidor decida lo estricto que es.
 *
 * Solo se miran los 36 slots de almacenamiento del jugador: en 1.9+
 * getContents() anade armadura y mano secundaria, y el coste nunca debe
 * cobrarse de la armadura que el jugador lleva puesta.
 */
public final class ItemMatch {

    /** Slots de almacenamiento del inventario del jugador (0-35). */
    private static final int STORAGE_SLOTS = 36;

    private ItemMatch() {
    }

    public static boolean matches(ItemStack actual, ItemStack required,
                                  boolean matchName, boolean matchLore,
                                  boolean matchEnchantments, boolean matchDurability,
                                  boolean matchNbt) {
        return matches(actual, required, matchName, matchLore, matchEnchantments, matchDurability, matchNbt,
                matchNbt && required != null && NbtUtil.hasCustomNbt(required));
    }

    /**
     * Version interna: el analisis NBT del item pedido se recibe ya hecho
     * para no repetirlo en cada slot del inventario (era el coste dominante
     * al repintar un menu con muchos iconos).
     */
    private static boolean matches(ItemStack actual, ItemStack required,
                                   boolean matchName, boolean matchLore,
                                   boolean matchEnchantments, boolean matchDurability,
                                   boolean matchNbt, boolean requiredHasNbt) {
        if (actual == null || required == null) {
            return false;
        }
        if (actual.getType() != required.getType()) {
            return false;
        }
        if (matchDurability && actual.getDurability() != required.getDurability()) {
            return false;
        }
        ItemMeta actualMeta = actual.getItemMeta();
        ItemMeta requiredMeta = required.getItemMeta();
        if (matchName) {
            String actualName = actualMeta == null ? null : actualMeta.getDisplayName();
            String requiredName = requiredMeta == null ? null : requiredMeta.getDisplayName();
            if (!equalText(actualName, requiredName)) {
                return false;
            }
        }
        if (matchLore) {
            List<String> actualLore = actualMeta == null ? null : actualMeta.getLore();
            List<String> requiredLore = requiredMeta == null ? null : requiredMeta.getLore();
            if (!equalLines(actualLore, requiredLore)) {
                return false;
            }
        }
        if (matchEnchantments) {
            Map<Enchantment, Integer> actualEnchants = actualMeta == null
                    ? Collections.<Enchantment, Integer>emptyMap() : actualMeta.getEnchants();
            Map<Enchantment, Integer> requiredEnchants = requiredMeta == null
                    ? Collections.<Enchantment, Integer>emptyMap() : requiredMeta.getEnchants();
            if (!actualEnchants.equals(requiredEnchants)) {
                return false;
            }
        }
        // NBT propio de otros plugins (tokens, vouchers...): si el coste
        // lo trae, el item del jugador debe traerlo identico.
        if (matchNbt && requiredHasNbt && !NbtUtil.customNbtMatches(required, actual)) {
            return false;
        }
        return true;
    }

    /** Cuenta cuantos items iguales al requerido hay en el inventario. */
    public static int count(Inventory inventory, ItemStack required,
                            boolean matchName, boolean matchLore,
                            boolean matchEnchantments, boolean matchDurability,
                            boolean matchNbt) {
        int total = 0;
        if (inventory == null || required == null) {
            return total;
        }
        ItemStack[] contents = storageContents(inventory);
        if (contents == null) {
            return total;
        }
        boolean requiredHasNbt = matchNbt && NbtUtil.hasCustomNbt(required);
        for (ItemStack stack : contents) {
            if (stack != null && matches(stack, required, matchName, matchLore, matchEnchantments, matchDurability,
                    matchNbt, requiredHasNbt)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /** Retira la cantidad indicada de items que coincidan con el requerido. */
    public static void remove(Inventory inventory, ItemStack required, int amount,
                              boolean matchName, boolean matchLore,
                              boolean matchEnchantments, boolean matchDurability,
                              boolean matchNbt) {
        if (inventory == null || required == null || amount <= 0) {
            return;
        }
        int remaining = amount;
        ItemStack[] contents = storageContents(inventory);
        if (contents == null) {
            return;
        }
        boolean requiredHasNbt = matchNbt && NbtUtil.hasCustomNbt(required);
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !matches(stack, required, matchName, matchLore, matchEnchantments, matchDurability,
                    matchNbt, requiredHasNbt)) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            remaining -= take;
            if (stack.getAmount() - take <= 0) {
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
                inventory.setItem(i, stack);
            }
        }
    }

    /**
     * Contenido que se puede gastar: los 36 slots de almacenamiento.
     * En 1.9+ getContents() devuelve ademas armadura y mano secundaria
     * (41 elementos en total) y sus indices van al final.
     */
    private static ItemStack[] storageContents(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return null;
        }
        if (contents.length > STORAGE_SLOTS && inventory instanceof PlayerInventory) {
            return Arrays.copyOf(contents, STORAGE_SLOTS);
        }
        return contents;
    }

    /** Descripcion corta y sin color: "64x Diamante". */
    public static String describe(ItemStack item) {
        if (item == null) {
            return "?";
        }
        return item.getAmount() + "x " + Text.strip(Text.itemName(item));
    }

    private static boolean equalText(String one, String two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return Text.strip(one).equals(Text.strip(two));
    }

    private static boolean equalLines(List<String> one, List<String> two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        if (one.size() != two.size()) {
            return false;
        }
        for (int i = 0; i < one.size(); i++) {
            if (!Text.strip(one.get(i)).equals(Text.strip(two.get(i)))) {
                return false;
            }
        }
        return true;
    }
}
