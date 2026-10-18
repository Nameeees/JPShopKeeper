package com.jp.shopkeeper.hook;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.manager.ChatInputManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * Chat moderno de Paper (io.papermc.paper.event.player.AsyncChatEvent).
 *
 * Paper marca AsyncPlayerChatEvent como obsoleto desde la 1.19; este hook
 * escucha el evento nuevo para que la edicion por chat siga funcionando si
 * algun dia retiran el antiguo. Todo va por reflexion y solo se registra si
 * la clase existe, asi que en Spigot o en versiones antiguas no hace nada.
 */
public final class PaperChatHook {

    private PaperChatHook() {
    }

    @SuppressWarnings("unchecked")
    public static void register(final JPShopKeeper plugin) {
        final Class<?> eventClass;
        try {
            eventClass = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
        } catch (Throwable ignored) {
            return; // Spigot o version sin el evento
        }
        try {
            plugin.getServer().getPluginManager().registerEvent(
                    (Class<? extends Event>) eventClass,
                    new Listener() {
                    },
                    EventPriority.LOWEST,
                    new EventExecutor() {
                        @Override
                        public void execute(Listener listener, Event event) {
                            handle(plugin, event);
                        }
                    },
                    plugin);
            plugin.getLogger().info("Chat moderno de Paper detectado: la edicion por chat usa AsyncChatEvent.");
        } catch (Throwable throwable) {
            plugin.getLogger().warning("No se pudo escuchar AsyncChatEvent: " + throwable);
        }
    }

    private static void handle(JPShopKeeper plugin, Event event) {
        try {
            if (Boolean.TRUE.equals(event.getClass().getMethod("isCancelled").invoke(event))) {
                return;
            }
            Object sender = event.getClass().getMethod("getPlayer").invoke(event);
            if (!(sender instanceof Player)) {
                return;
            }
            final Player player = (Player) sender;
            Object component = event.getClass().getMethod("message").invoke(event);
            final String message = plainText(component);
            if (message == null) {
                return;
            }
            if (plugin.chatInput().wasJustClaimed(player, message)) {
                // El evento antiguo ya lo consumio: no puede ademas ir al chat.
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
                return;
            }
            final ChatInputManager.Input input = plugin.chatInput().claim(player, message);
            if (input == null) {
                return;
            }
            event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.chatInput().deliver(player, message, input);
                }
            });
        } catch (Throwable ignored) {
            // si Paper cambia la firma, el chat antiguo sigue funcionando
        }
    }

    private static String plainText(Object component) {
        if (component == null) {
            return null;
        }
        try {
            Class<?> serializerClass = Class.forName(
                    "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Object serializer = serializerClass.getMethod("plainText").invoke(null);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object text = serializer.getClass().getMethod("serialize", componentClass).invoke(serializer, component);
            return text instanceof String ? (String) text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
