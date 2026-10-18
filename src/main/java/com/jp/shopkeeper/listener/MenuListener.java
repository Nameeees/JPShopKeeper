package com.jp.shopkeeper.listener;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Reparte los clics de todos los menus del plugin. Antes de cualquier
 * accion se cancela el evento: jamas se puede mover un item de un menu
 * al inventario del jugador (ni con shift-click).
 */
public final class MenuListener implements Listener {

    private final JPShopKeeper plugin;

    public MenuListener(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu)) {
            return;
        }
        Menu menu = (Menu) top.getHolder();
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (!menu.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()) {
            menu.handleClick(event);
        } else {
            menu.handlePlayerInventoryClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof Menu) {
            try {
                ((Menu) top.getHolder()).onClosed();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Error cerrando un menu: " + throwable);
            }
        }
    }
}
