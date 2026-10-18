package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

/**
 * Lista de paginas del trader: una pagina por slot del grid.
 * Desde aqui se anaden paginas y se entra al editor de cada una.
 */
public class PagesMenu extends Menu {

    private final Trader trader;

    public PagesMenu(JPShopKeeper plugin, Player player, Trader trader) {
        super(plugin, player);
        this.trader = trader;
    }

    @Override
    protected String title() {
        return plugin.menus().title("pages-menu", Ph.of(
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 14),
                "id", trader.getId()
        ));
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("pages-menu", 6);
    }

    @Override
    protected void populate() {
        fill(plugin.menus().fillerSlots("pages-menu"), plugin.menus().filler("pages-menu"));

        int maxPages = Math.min(plugin.configManager().maxPages(), TradePage.TRADE_SLOTS);
        for (int index = 0; index < maxPages && index < trader.pageCount(); index++) {
            final int pageIndex = index;
            TradePage page = trader.getPage(index);
            if (page == null) {
                continue;
            }
            Map<String, String> placeholders = Ph.of(
                    "page", index + 1,
                    "name", Text.strip(page.getName()),
                    "count", page.count(),
                    "trader", Text.strip(trader.getDisplayName())
            );
            set(index, plugin.menus().item("pages-menu.page-icon", placeholders), new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    new PageEditorMenu(plugin, player, trader, pageIndex).open();
                }
            });
        }

        set(plugin.menus().slot("pages-menu", "back", 45), plugin.menus().button("pages-menu", "back", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        new AdminMenu(plugin, player, trader).open();
                    }
                });

        set(plugin.menus().slot("pages-menu", "add", 49), plugin.menus().button("pages-menu", "add", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        addPage();
                    }
                });

        set(plugin.menus().slot("pages-menu", "close", 53), plugin.menus().button("pages-menu", "close", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                    }
                });
    }

    private void addPage() {
        int maxPages = Math.min(plugin.configManager().maxPages(), TradePage.TRADE_SLOTS);
        if (trader.pageCount() >= maxPages) {
            plugin.messages().send(player, "editor.page-limit", Ph.of("max", maxPages));
            plugin.sounds().play(player, "error");
            return;
        }
        int number = trader.pageCount() + 1;
        String name = plugin.messages().text("editor.page-default-name", Ph.of("n", number));
        trader.addPage(new TradePage(name));
        plugin.traderManager().save(trader);
        plugin.messages().send(player, "editor.page-added", Ph.of("page", number));
        plugin.sounds().play(player, "set");
        refresh();
    }
}
