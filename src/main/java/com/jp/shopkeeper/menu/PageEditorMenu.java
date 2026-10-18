package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.manager.ChatInputManager;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.ItemBuilder;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Editor de una pagina: el grid completo (45 slots) refleja exactamente
 * lo que vera el jugador. Slot vacio = crear tradeo; clic = editar;
 * shift + clic = eliminar (con confirmacion).
 */
public class PageEditorMenu extends Menu {

    private final Trader trader;
    private int pageIndex;

    public PageEditorMenu(JPShopKeeper plugin, Player player, Trader trader, int pageIndex) {
        super(plugin, player);
        this.trader = trader;
        this.pageIndex = Math.max(0, Math.min(pageIndex, Math.max(0, trader.pageCount() - 1)));
    }

    @Override
    public void open() {
        if (trader.pageCount() == 0) {
            new PagesMenu(plugin, player, trader).open();
            return;
        }
        super.open();
    }

    @Override
    protected String title() {
        TradePage page = trader.getPage(pageIndex);
        String name = page == null ? "" : Text.strip(page.getName());
        return plugin.menus().title("page-editor-menu", Ph.of(
                "page", pageIndex + 1,
                "name", Text.truncate(name, 12),
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 12)
        ));
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("page-editor-menu", 6);
    }

    @Override
    protected void populate() {
        TradePage page = trader.getPage(pageIndex);
        if (page == null) {
            return;
        }
        fill(plugin.menus().fillerSlots("page-editor-menu"), plugin.menus().filler("page-editor-menu"));

        for (final int tradeSlot : TradePage.GRID_SLOTS) {
            final Trade trade = page.getTrade(tradeSlot);
            if (trade == null) {
                set(tradeSlot, plugin.menus().item("page-editor-menu.empty-slot", null), new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        TradePage current = trader.getPage(pageIndex);
                        if (current == null) {
                            return;
                        }
                        current.setTrade(tradeSlot, new Trade());
                        new TradeEditorMenu(plugin, player, trader, pageIndex, tradeSlot).open();
                    }
                });
            } else {
                ItemStack icon = trade.isComplete()
                        ? buildCompleteIcon(trade)
                        : plugin.menus().item("page-editor-menu.incomplete-slot", null);
                set(tradeSlot, icon, new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        if (event.isShiftClick()) {
                            confirmDeleteTrade(tradeSlot);
                        } else {
                            new TradeEditorMenu(plugin, player, trader, pageIndex, tradeSlot).open();
                        }
                    }
                });
            }
        }

        Map<String, String> placeholders = Ph.of("page", pageIndex + 1, "name", Text.strip(page.getName()));

        set(plugin.menus().slot("page-editor-menu", "back", 45), plugin.menus().button("page-editor-menu", "back", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        new PagesMenu(plugin, player, trader).open();
                    }
                });

        if (pageIndex > 0) {
            set(plugin.menus().slot("page-editor-menu", "prev", 46), plugin.menus().button("page-editor-menu", "prev", placeholders),
                    new Action() {
                        @Override
                        public void run(InventoryClickEvent event) {
                            pageIndex--;
                            open();
                        }
                    });
        }

        set(plugin.menus().slot("page-editor-menu", "rename", 48), plugin.menus().button("page-editor-menu", "rename", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        promptRename();
                    }
                });

        set(plugin.menus().slot("page-editor-menu", "preview", 49), plugin.menus().button("page-editor-menu", "preview", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        new TradeMenu(plugin, player, trader, pageIndex).open();
                    }
                });

        set(plugin.menus().slot("page-editor-menu", "delete", 50), plugin.menus().button("page-editor-menu", "delete", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        confirmDeletePage();
                    }
                });

        if (pageIndex < trader.pageCount() - 1) {
            set(plugin.menus().slot("page-editor-menu", "next", 52), plugin.menus().button("page-editor-menu", "next", placeholders),
                    new Action() {
                        @Override
                        public void run(InventoryClickEvent event) {
                            pageIndex++;
                            open();
                        }
                    });
        }

        set(plugin.menus().slot("page-editor-menu", "close", 53), plugin.menus().button("page-editor-menu", "close", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                    }
                });
    }

    private ItemStack buildCompleteIcon(Trade trade) {
        ItemStack result = trade.getResult();
        if (result == null) {
            return plugin.menus().item("page-editor-menu.incomplete-slot", null);
        }
        ItemBuilder builder = ItemBuilder.of(result);
        if (builder == null) {
            return result.clone();
        }
        ItemMeta meta = result.getItemMeta();
        boolean hasName = meta != null && meta.hasDisplayName();
        if (!hasName) {
            builder.name("&f" + Text.prettyMaterial(result));
        }
        List<String> lore = new ArrayList<String>();
        if (meta != null && meta.getLore() != null && !meta.getLore().isEmpty()) {
            lore.addAll(meta.getLore());
        }
        lore.addAll(plugin.menus().lore("page-editor-menu.trade-slot-hint", null));
        builder.lore(lore);
        return builder.build();
    }

    private void promptRename() {
        plugin.chatInput().request(player, "editor.prompt-page-name", null, new ChatInputManager.Input() {
            @Override
            public void accept(Player target, String text) {
                TradePage page = trader.getPage(pageIndex);
                if (page == null) {
                    return;
                }
                String name = Text.truncate(text.trim(), 24);
                if (name.isEmpty()) {
                    return;
                }
                page.setName(name);
                plugin.traderManager().save(trader);
                plugin.messages().send(target, "editor.page-renamed", Ph.of("name", name));
                plugin.sounds().play(target, "set");
                new PageEditorMenu(plugin, target, trader, pageIndex).open();
            }
        });
    }

    private void confirmDeleteTrade(final int tradeSlot) {
        new ConfirmMenu(plugin, player,
                plugin.messages().text("confirm.delete-trade.question", Ph.of("slot", TradePage.displaySlot(tradeSlot))),
                plugin.messages().lines("confirm.delete-trade.details", null),
                new Runnable() {
                    @Override
                    public void run() {
                        TradePage page = trader.getPage(pageIndex);
                        if (page != null) {
                            page.removeTrade(tradeSlot);
                        }
                        plugin.traderManager().save(trader);
                        plugin.messages().send(player, "editor.trade-deleted");
                        plugin.sounds().play(player, "remove");
                        new PageEditorMenu(plugin, player, trader, pageIndex).open();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        new PageEditorMenu(plugin, player, trader, pageIndex).open();
                    }
                }).open();
    }

    private void confirmDeletePage() {
        new ConfirmMenu(plugin, player,
                plugin.messages().text("confirm.delete-page.question", Ph.of("page", pageIndex + 1)),
                plugin.messages().lines("confirm.delete-page.details", null),
                new Runnable() {
                    @Override
                    public void run() {
                        trader.removePage(pageIndex);
                        plugin.traderManager().save(trader);
                        plugin.messages().send(player, "editor.page-deleted");
                        plugin.sounds().play(player, "remove");
                        new PagesMenu(plugin, player, trader).open();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        new PageEditorMenu(plugin, player, trader, pageIndex).open();
                    }
                }).open();
    }
}
