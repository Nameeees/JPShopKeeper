package com.jp.shopkeeper.listener;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.Trader;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;

/**
 * Proteccion de los NPCs: no reciben dano (cubre ataques, fuego, rayos,
 * explosiones y la conversion de aldeano a zombi), no se queman al sol,
 * no entran en portales, no se pueden atar con rienda y ningun mob los
 * toma como objetivo. Si aun asi desaparecen, TraderManager los regenera.
 */
public final class TraderProtectionListener implements Listener {

    private final JPShopKeeper plugin;

    public TraderProtectionListener(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.configManager().npcProtect()) {
            return;
        }
        if (plugin.traderManager().byEntity(event.getEntity()) == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!plugin.configManager().npcProtect()) {
            return;
        }
        if (plugin.traderManager().byEntity(event.getEntity()) == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (!plugin.configManager().npcProtect()) {
            return;
        }
        if (event.getTarget() != null && plugin.traderManager().byEntity(event.getTarget()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (!plugin.configManager().npcProtect()) {
            return;
        }
        if (plugin.traderManager().byEntity(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(EntityPortalEvent event) {
        if (!plugin.configManager().npcProtect()) {
            return;
        }
        if (plugin.traderManager().byEntity(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        final Trader trader = plugin.traderManager().byEntity(event.getEntity());
        if (trader == null) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Trader current = plugin.traderManager().byId(trader.getId());
                if (current != null) {
                    plugin.traderManager().handleDeath(current);
                    plugin.getLogger().info("NPC del trader '" + current.getId() + "' murio pese a las protecciones: se regenera.");
                }
            }
        });
    }
}
