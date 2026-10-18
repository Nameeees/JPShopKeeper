package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.manager.ChatInputManager;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Entities;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;

/**
 * Panel de administracion del trader: se abre con shift + clic derecho
 * sobre el NPC (permiso jpshopkeeper.admin.edit) o con /trader edit.
 */
public class AdminMenu extends Menu {

    private final Trader trader;

    public AdminMenu(JPShopKeeper plugin, Player player, Trader trader) {
        super(plugin, player);
        this.trader = trader;
    }

    @Override
    protected String title() {
        return plugin.menus().title("admin-menu", placeholders());
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("admin-menu", 6);
    }

    private Map<String, String> placeholders() {
        return Ph.of(
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 14),
                "id", trader.getId(),
                "type", trader.getEntityTypeName(),
                "world", trader.getHomeWorldName(),
                "coords", trader.homeString(),
                "pages", trader.pageCount(),
                "trades", trader.totalTrades()
        );
    }

    @Override
    protected void populate() {
        Map<String, String> placeholders = placeholders();
        fill(plugin.menus().fillerSlots("admin-menu"), plugin.menus().filler("admin-menu"));

        set(plugin.menus().slot("admin-menu", "info", 4), plugin.menus().button("admin-menu", "info", placeholders));

        set(plugin.menus().slot("admin-menu", "rename", 20), plugin.menus().button("admin-menu", "rename", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        promptRename();
                    }
                });

        set(plugin.menus().slot("admin-menu", "type", 22), plugin.menus().button("admin-menu", "type", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        cycleType();
                    }
                });

        set(plugin.menus().slot("admin-menu", "pages", 24), plugin.menus().button("admin-menu", "pages", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        new PagesMenu(plugin, player, trader).open();
                    }
                });

        set(plugin.menus().slot("admin-menu", "preview", 29), plugin.menus().button("admin-menu", "preview", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        preview();
                    }
                });

        set(plugin.menus().slot("admin-menu", "move", 31), plugin.menus().button("admin-menu", "move", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        moveHere();
                    }
                });

        set(plugin.menus().slot("admin-menu", "delete", 33), plugin.menus().button("admin-menu", "delete", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        confirmDelete();
                    }
                });

        set(plugin.menus().slot("admin-menu", "close", 49), plugin.menus().button("admin-menu", "close", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                    }
                });
    }

    private void promptRename() {
        plugin.chatInput().request(player, "editor.prompt-trader-name", null, new ChatInputManager.Input() {
            @Override
            public void accept(Player target, String text) {
                String name = Text.truncate(text.trim(), 32);
                if (name.isEmpty()) {
                    return;
                }
                plugin.traderManager().rename(trader, name);
                plugin.messages().send(target, "editor.trader-renamed", Ph.of("name", name));
                plugin.sounds().play(target, "set");
                new AdminMenu(plugin, target, trader).open();
            }
        });
    }

    private void cycleType() {
        List<String> allowed = plugin.configManager().allowedTypes();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        int current = -1;
        for (int i = 0; i < allowed.size(); i++) {
            if (allowed.get(i).equalsIgnoreCase(trader.getEntityTypeName())) {
                current = i;
                break;
            }
        }
        // Puede haber mobs de versiones modernas que este servidor no conozca:
        // se salta hasta encontrar uno que exista aqui.
        String next = null;
        for (int step = 1; step <= allowed.size(); step++) {
            String candidate = allowed.get((current + step) % allowed.size());
            if (Entities.type(candidate) != null) {
                next = candidate;
                break;
            }
        }
        if (next == null) {
            plugin.messages().send(player, "trader.invalid-type", Ph.of("type", allowed.get(0)));
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.traderManager().changeType(trader, next);
        plugin.messages().send(player, "trader.type-changed", Ph.of("trader", trader.getId(), "type", next));
        plugin.sounds().play(player, "set");
        refresh();
    }

    private void preview() {
        if (trader.pageCount() == 0) {
            plugin.messages().send(player, "trade.empty-trader");
            plugin.sounds().play(player, "error");
            return;
        }
        new TradeMenu(plugin, player, trader, 0).open();
    }

    private void moveHere() {
        plugin.traderManager().move(trader, player.getLocation());
        plugin.messages().send(player, "trader.moved", Ph.of("trader", trader.getId()));
        plugin.sounds().play(player, "set");
        refresh();
    }

    private void confirmDelete() {
        new ConfirmMenu(plugin, player,
                plugin.messages().text("confirm.delete-trader.question",
                        Ph.of("trader", Text.strip(trader.getDisplayName()))),
                plugin.messages().lines("confirm.delete-trader.details", null),
                new Runnable() {
                    @Override
                    public void run() {
                        plugin.traderManager().remove(trader.getId());
                        plugin.messages().send(player, "editor.trader-deleted", Ph.of("trader", trader.getId()));
                        plugin.sounds().play(player, "remove");
                        player.closeInventory();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        new AdminMenu(plugin, player, trader).open();
                    }
                }).open();
    }
}
