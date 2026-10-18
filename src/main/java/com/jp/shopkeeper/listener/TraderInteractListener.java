package com.jp.shopkeeper.listener;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.menu.AdminMenu;
import com.jp.shopkeeper.menu.TradeMenu;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Ph;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Clic derecho en un NPC = menu de tradeos.
 * Shift + clic derecho (con permiso) = panel de administracion.
 *
 * Se escuchan LOS DOS eventos de interaccion: hasta la 1.8 el evento "at"
 * tiene su propia lista de handlers y el servidor solo dispara uno de los
 * dos, asi que con un unico handler el clic no llegaria nunca en las
 * versiones antiguas. El guard anti-duplicados evita abrir el menu dos veces.
 *
 * Ademas se cierra la GUI de comercio vanilla del aldeano: en 1.8 el clic
 * derecho la abre aunque el evento se cancele, y tapaba el menu del plugin.
 */
public final class TraderInteractListener implements Listener {

    private static final long DUPLICATE_WINDOW_MS = 250L;

    private final JPShopKeeper plugin;
    private final Map<String, Long> recent = new HashMap<String, Long>();

    public TraderInteractListener(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent event) {
        handle(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handle(event.getPlayer(), event.getRightClicked(), event);
    }

    /** La GUI de comercio vanilla de un NPC del plugin nunca se deja pasar. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVanillaTrade(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Entity)) {
            return;
        }
        Trader trader = plugin.traderManager().byEntity((Entity) holder);
        if (trader == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (recentlyHandled(player, (Entity) holder)) {
            // Nuestro menu ya se abrio con este mismo clic.
            return;
        }
        if (plugin.configManager().debugEnabled()) {
            plugin.configManager().debug("GUI vanilla del aldeano cerrada para " + player.getName()
                    + ": se abre el menu del plugin.");
        }
        openMenu(player, trader, (Entity) holder);
    }

    private void handle(Player player, Entity clicked, Cancellable event) {
        Trader trader = plugin.traderManager().byEntity(clicked);
        if (plugin.configManager().debugEnabled()) {
            plugin.configManager().debug("Interaccion de " + player.getName() + " con " + clicked.getType()
                    + " -> trader=" + (trader == null ? "ninguno" : trader.getId()));
        }
        if (trader == null) {
            return;
        }
        // nunca se deja pasar la GUI de comercio vanilla del aldeano
        event.setCancelled(true);

        if (isDuplicate(player, clicked)) {
            return;
        }
        openMenu(player, trader, clicked);
    }

    /** Menu de tradeos, o panel de administracion si el jugador va agachado. */
    private void openMenu(Player player, Trader trader, Entity clicked) {
        if (player.isSneaking()) {
            if (player.hasPermission("jpshopkeeper.admin.edit")) {
                new AdminMenu(plugin, player, trader).open();
            } else {
                plugin.messages().send(player, "general.no-permission", Ph.of("permission", "jpshopkeeper.admin.edit"));
                plugin.sounds().play(player, "error");
            }
            return;
        }

        if (!player.hasPermission("jpshopkeeper.use")) {
            plugin.messages().send(player, "general.no-permission", Ph.of("permission", "jpshopkeeper.use"));
            plugin.sounds().play(player, "error");
            return;
        }

        if (trader.pageCount() == 0) {
            plugin.messages().send(player, "trade.empty-trader");
            plugin.sounds().play(player, "error");
            return;
        }

        new TradeMenu(plugin, player, trader, 0).open();
    }

    private boolean isDuplicate(Player player, Entity entity) {
        long now = System.currentTimeMillis();
        String key = player.getUniqueId() + "|" + entity.getUniqueId();
        Long last = recent.put(key, now);
        if (recent.size() > 200) {
            prune(now);
        }
        return last != null && now - last < DUPLICATE_WINDOW_MS;
    }

    /** Igual que isDuplicate pero sin consumir el clic: solo consulta. */
    private boolean recentlyHandled(Player player, Entity entity) {
        Long last = recent.get(player.getUniqueId() + "|" + entity.getUniqueId());
        return last != null && System.currentTimeMillis() - last < DUPLICATE_WINDOW_MS;
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> iterator = recent.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > DUPLICATE_WINDOW_MS * 4) {
                iterator.remove();
            }
        }
    }
}
