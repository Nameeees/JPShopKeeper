package com.jp.shopkeeper.listener;

import com.jp.shopkeeper.JPShopKeeper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Captura la siguiente linea de chat cuando el editor pide un valor
 * (nombre, cantidad, permiso...). El evento de chat es asincrono: el
 * texto se procesa en el hilo principal para poder tocar inventarios.
 */
public final class ChatInputListener implements Listener {

    private final JPShopKeeper plugin;

    public ChatInputListener(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (plugin.chatInput().wasJustClaimed(player, event.getMessage())) {
            // El evento moderno de Paper ya lo consumio.
            event.setCancelled(true);
            return;
        }
        // La entrada se reclama aqui (hilo del chat): asi dos mensajes del
        // mismo tick no se cancelan los dos y no se pierde el segundo.
        final com.jp.shopkeeper.manager.ChatInputManager.Input input =
                plugin.chatInput().claim(player, event.getMessage());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        final String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.chatInput().deliver(player, message, input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.chatInput().clear(event.getPlayer());
    }
}
