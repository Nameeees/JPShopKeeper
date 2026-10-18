package com.jp.shopkeeper.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Una pagina de tradeos: hasta 28 tradeos visibles, uno por slot del
 * grid interior del menu (el menu lleva marco perimetral completo).
 */
public class TradePage {

    /** Slots validos del modelo (compatibilidad con datos antiguos). */
    public static final int TRADE_SLOTS = 45;

    /**
     * Slots VISIBLES del grid interior del menu (marco perimetral de 1 slot):
     * filas 1-4, columnas 1-7 del cofre de 54 → 28 tradeos por pagina.
     */
    public static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    /** Posicion (1..28) de un slot dentro del grid visible; -1 si esta fuera. */
    public static int gridPosition(int slot) {
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slot) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Numero legible para mensajes del editor (posicion 1..28 o slot+1). */
    public static int displaySlot(int slot) {
        int position = gridPosition(slot);
        return position > 0 ? position : slot + 1;
    }

    private String name;
    private final Map<Integer, Trade> trades = new TreeMap<Integer, Trade>();

    public TradePage(String name) {
        this.name = name == null ? "" : name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public Trade getTrade(int slot) {
        return trades.get(slot);
    }

    public void setTrade(int slot, Trade trade) {
        if (slot < 0 || slot >= TRADE_SLOTS) {
            return;
        }
        if (trade == null) {
            trades.remove(slot);
        } else {
            trades.put(slot, trade);
        }
    }

    public void removeTrade(int slot) {
        trades.remove(slot);
    }

    /** Tradeos por slot (ordenados por slot). */
    public Map<Integer, Trade> getTrades() {
        return Collections.unmodifiableMap(trades);
    }

    public int count() {
        return trades.size();
    }
}
