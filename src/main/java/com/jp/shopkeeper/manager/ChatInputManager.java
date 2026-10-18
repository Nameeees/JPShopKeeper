package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.util.Text;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entradas por chat: pide un valor (nombre, cantidad, permiso...) y lo
 * entrega al callback en el hilo principal. Mientras hay una entrada
 * pendiente, el mensaje del jugador no se envia al chat.
 *
 * El evento de chat llega en otro hilo, asi que la entrada se "reclama" de
 * forma atomica antes de cancelar el mensaje: dos lineas seguidas del mismo
 * tick no pueden cancelarse las dos. Cada entrada caduca sola, para que un
 * prompt abandonado no se coma el siguiente mensaje del jugador.
 */
public final class ChatInputManager {

    /** Tiempo maximo que se espera el valor escrito por el jugador. */
    private static final long TIMEOUT_MS = 120000L;

    public interface Input {
        void accept(Player player, String text);
    }

    /** Entrada pendiente con su caducidad. */
    private static final class Pending {
        final Input input;
        final long expiresAt;

        Pending(Input input, long expiresAt) {
            this.input = input;
            this.expiresAt = expiresAt;
        }
    }

    private final JPShopKeeper plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<UUID, Pending>();
    /** Ultimo texto consumido por jugador, para no procesarlo dos veces. */
    private final Map<UUID, String> lastClaimed = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Long> lastClaimedAt = new ConcurrentHashMap<UUID, Long>();

    public ChatInputManager(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    /** Cierra el inventario, envia el prompt y registra el callback. */
    public void request(Player player, String promptKey, Map<String, String> placeholders, Input input) {
        pending.put(player.getUniqueId(), new Pending(input, System.currentTimeMillis() + TIMEOUT_MS));
        player.closeInventory();
        plugin.messages().send(player, "general.input-hint");
        plugin.messages().send(player, promptKey, placeholders);
    }

    /** Hay una entrada esperando (y sin caducar) para este jugador. No la consume. */
    public boolean isWaiting(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        Pending waiting = pending.get(uuid);
        if (waiting == null) {
            return false;
        }
        if (waiting.expiresAt <= System.currentTimeMillis()) {
            pending.remove(uuid, waiting);
            return false;
        }
        return true;
    }

    /**
     * Retira la entrada pendiente. Se llama desde el hilo del chat para
     * decidir si el mensaje se cancela o no; solo uno de los mensajes del
     * mismo tick puede quedarse con ella.
     */
    public Input claim(Player player, String message) {
        if (player == null) {
            return null;
        }
        Pending waiting = pending.remove(player.getUniqueId());
        if (waiting == null || waiting.expiresAt <= System.currentTimeMillis()) {
            return null;
        }
        lastClaimed.put(player.getUniqueId(), message);
        lastClaimedAt.put(player.getUniqueId(), System.currentTimeMillis());
        return waiting.input;
    }

    /**
     * true si ese mismo texto ya lo consumio el otro evento de chat hace menos
     * de medio segundo (Paper dispara AsyncChatEvent y AsyncPlayerChatEvent).
     */
    public boolean wasJustClaimed(Player player, String message) {
        if (player == null || message == null) {
            return false;
        }
        String text = lastClaimed.get(player.getUniqueId());
        Long at = lastClaimedAt.get(player.getUniqueId());
        return text != null && at != null && message.equals(text)
                && System.currentTimeMillis() - at < 500L;
    }

    /** Procesa el texto ya reclamado. Se ejecuta en el hilo principal. */
    public void deliver(Player player, String message, Input input) {
        if (player == null || input == null) {
            return;
        }
        String cancelWord = Text.strip(plugin.messages().text("general.cancel-word")).trim();
        if (message != null && message.trim().equalsIgnoreCase(cancelWord)) {
            plugin.messages().send(player, "general.input-cancelled");
            return;
        }
        try {
            input.accept(player, message == null ? "" : message);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Error procesando la entrada de " + player.getName() + ": " + throwable);
        }
    }

    public void clear(Player player) {
        if (player != null) {
            pending.remove(player.getUniqueId());
        }
    }
}
