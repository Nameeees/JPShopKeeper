package com.jp.shopkeeper.model;

import com.jp.shopkeeper.util.Numbers;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Un tradeo configurable: hasta 3 items de coste + dinero, un resultado,
 * permiso opcional y enfriamiento opcional.
 */
public class Trade {

    /** Slots de coste disponibles en el editor. */
    public static final int COST_SLOTS = 3;
    /** Cantidad maxima por slot (igual que una pila). */
    public static final int MAX_AMOUNT = 64;

    private final ItemStack[] cost = new ItemStack[COST_SLOTS];
    private ItemStack result;
    private double money;
    private String permission = "";
    private int cooldownSeconds;

    public ItemStack getCost(int index) {
        if (index < 0 || index >= COST_SLOTS) {
            return null;
        }
        return cost[index];
    }

    public void setCost(int index, ItemStack item) {
        if (index < 0 || index >= COST_SLOTS) {
            return;
        }
        cost[index] = sanitize(item);
    }

    /** Items de coste no vacios, en orden. */
    public List<ItemStack> getCostItems() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ItemStack item : cost) {
            if (item != null) {
                out.add(item);
            }
        }
        return out;
    }

    public boolean hasCostItems() {
        for (ItemStack item : cost) {
            if (item != null) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = sanitize(result);
    }

    /**
     * Copia defensiva de un item: nunca guarda cantidades imposibles (0,
     * negativas o mayores que la pila maxima de ese material), que de otro
     * modo se convertirian en items duplicados al comerciar.
     */
    private static ItemStack sanitize(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack copy = item.clone();
        int max;
        try {
            max = Numbers.clamp(copy.getType().getMaxStackSize(), 1, MAX_AMOUNT);
        } catch (Throwable ignored) {
            max = MAX_AMOUNT;
        }
        if (copy.getAmount() < 1) {
            copy.setAmount(1);
        } else if (copy.getAmount() > max) {
            copy.setAmount(max);
        }
        return copy;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        // Math.max(0, NaN) sigue siendo NaN y NaN > 0 es false: un precio asi
        // dejaria el tradeo gratis sin cobrar nada.
        this.money = Double.isFinite(money) && money > 0.0 ? money : 0.0;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission.trim();
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    /** Un tradeo es usable por jugadores cuando tiene resultado. */
    public boolean isComplete() {
        return result != null;
    }

    /** Sin nada configurado: se limpia al cerrar el editor. */
    public boolean isEmpty() {
        return !hasCostItems() && result == null && money <= 0 && !hasPermission() && cooldownSeconds <= 0;
    }
}
