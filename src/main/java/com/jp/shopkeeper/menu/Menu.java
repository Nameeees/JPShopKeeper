package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de todos los menus de cofre. Implementa InventoryHolder: el
 * listener global detecta asi cada menu y cancela SIEMPRE los clics
 * (anti shift-click y anti robo) antes de repartir acciones.
 *
 * El patron funciona igual en 1.8 y en la ultima version: solo usa
 * Bukkit.createInventory + titulos legacy de maximo 32 caracteres.
 */
public abstract class Menu implements InventoryHolder {

    /** Accion asociada a un slot del inventario superior. */
    public interface Action {
        void run(InventoryClickEvent event);
    }

    protected final JPShopKeeper plugin;
    protected final Player player;

    private Inventory inventory;
    private final Map<Integer, Action> actions = new HashMap<Integer, Action>();

    protected Menu(JPShopKeeper plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract String title();

    protected abstract int rows();

    /** Pinta el contenido usando set(...). */
    protected abstract void populate();

    public Player getPlayer() {
        return player;
    }

    /** Crea el inventario, lo pinta y lo abre. */
    public void open() {
        int size = Math.max(1, Math.min(6, rows())) * 9;
        inventory = Bukkit.createInventory(this, size, title());
        actions.clear();
        inventory.clear();
        try {
            populate();
        } catch (Throwable throwable) {
            // Un fallo pintando el menu no puede dejar al jugador sin inventario.
            plugin.getLogger().warning("Error pintando el menu '" + title() + "': " + throwable);
        }
        player.openInventory(inventory);
        plugin.sounds().play(player, "open");
    }

    /** Reconstruye el contenido del inventario abierto (por ejemplo, estados de tradeo). */
    protected void refresh() {
        if (inventory == null) {
            open();
            return;
        }
        actions.clear();
        inventory.clear();
        try {
            populate();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Error repintando el menu '" + title() + "': " + throwable);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void set(int slot, ItemStack item, Action action) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize() || item == null) {
            return;
        }
        // Copia defensiva: fill() reparte la misma instancia por muchos slots.
        ItemStack copy = item.clone();
        if (copy.getAmount() < 1) {
            copy.setAmount(1);
        }
        inventory.setItem(slot, copy);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    protected void fill(List<Integer> slots, ItemStack item) {
        if (slots == null || item == null) {
            return;
        }
        for (Integer slot : slots) {
            set(slot, item);
        }
    }

    /** Clic en el inventario del menu (ya cancelado por el listener). */
    public void handleClick(InventoryClickEvent event) {
        Action action = actions.get(event.getRawSlot());
        if (action != null) {
            action.run(event);
        }
    }

    /** Clic en el inventario del jugador mientras el menu esta abierto (ya cancelado). */
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        // por defecto no se hace nada: los clics del jugador no mueven items
    }

    /** Se llama cuando el inventario de este menu se cierra. */
    public void onClosed() {
        // opcional por menu
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
