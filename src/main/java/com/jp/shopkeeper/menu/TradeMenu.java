package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.manager.TradeService;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.ItemBuilder;
import com.jp.shopkeeper.util.ItemMatch;
import com.jp.shopkeeper.util.Numbers;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de tradeos que abre cualquier jugador con clic derecho en el NPC.
 * Cada icono muestra el coste real, el estado en vivo (dinero, enfriamiento,
 * permiso, items) y al hacer clic ejecuta el tradeo con feedback completo.
 */
public class TradeMenu extends Menu {

    private final Trader trader;
    private int pageIndex;

    public TradeMenu(JPShopKeeper plugin, Player player, Trader trader, int pageIndex) {
        super(plugin, player);
        this.trader = trader;
        this.pageIndex = Math.max(0, pageIndex);
        if (trader.pageCount() > 0 && this.pageIndex > trader.pageCount() - 1) {
            this.pageIndex = trader.pageCount() - 1;
        }
    }

    @Override
    protected String title() {
        return plugin.menus().title("trade-menu", titlePlaceholders());
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("trade-menu", 6);
    }

    private Map<String, String> titlePlaceholders() {
        return Ph.of(
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 14),
                "id", trader.getId(),
                "page", pageIndex + 1,
                "pages", Math.max(1, trader.pageCount())
        );
    }

    @Override
    protected void populate() {
        ItemStack filler = plugin.menus().filler("trade-menu");
        fill(plugin.menus().fillerSlots("trade-menu"), filler);

        TradePage page = trader.getPage(pageIndex);
        int tradeCount = 0;
        for (final int slot : TradePage.GRID_SLOTS) {
            final Trade trade = page == null ? null : page.getTrade(slot);
            if (trade == null || !trade.isComplete()) {
                set(slot, filler);
                continue;
            }
            tradeCount++;
            set(slot, buildTradeIcon(trade, slot), new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    attemptTrade(trade, slot);
                }
            });
        }

        Map<String, String> placeholders = Ph.of(
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 14),
                "id", trader.getId(),
                "page", pageIndex + 1,
                "pages", Math.max(1, trader.pageCount()),
                "count", tradeCount
        );

        int prevSlot = plugin.menus().slot("trade-menu", "prev", 47);
        if (pageIndex > 0) {
            set(prevSlot, plugin.menus().button("trade-menu", "prev", placeholders), new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    pageIndex--;
                    open();
                }
            });
        } else {
            set(prevSlot, filler);
        }

        set(plugin.menus().slot("trade-menu", "info", 49), plugin.menus().button("trade-menu", "info", placeholders));

        int nextSlot = plugin.menus().slot("trade-menu", "next", 51);
        if (pageIndex < trader.pageCount() - 1) {
            set(nextSlot, plugin.menus().button("trade-menu", "next", placeholders), new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    pageIndex++;
                    open();
                }
            });
        } else {
            set(nextSlot, filler);
        }

        set(plugin.menus().slot("trade-menu", "close", 53), plugin.menus().button("trade-menu", "close", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                    }
                });
    }

    private ItemStack buildTradeIcon(Trade trade, int slot) {
        ItemStack result = trade.getResult();
        if (result == null) {
            return null;
        }
        ItemBuilder builder = ItemBuilder.of(result);
        if (builder == null) {
            return null;
        }
        ItemMeta meta = result.getItemMeta();
        boolean hasName = meta != null && meta.hasDisplayName();
        if (!hasName) {
            builder.name("&f" + Text.prettyMaterial(result));
        }
        List<String> lore = new ArrayList<String>();
        if (meta != null && meta.getLore() != null && !meta.getLore().isEmpty()) {
            lore.addAll(meta.getLore());
            lore.add("&8 ");
        }
        lore.addAll(plugin.menus().lore("trade-menu.trade-item.lore", iconPlaceholders(trade, slot)));
        builder.lore(lore);
        return builder.build();
    }

    private Map<String, String> iconPlaceholders(Trade trade, int slot) {
        return Ph.of(
                "cost_lines", costLines(trade),
                "money_line", moneyLine(trade),
                "status", statusLine(trade, slot),
                "slot", slot,
                "money", Numbers.format(trade.getMoney()),
                "permission", trade.getPermission()
        );
    }

    private String costLines(Trade trade) {
        StringBuilder out = new StringBuilder();
        for (ItemStack cost : trade.getCostItems()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append("&7• &f").append(ItemMatch.describe(cost));
        }
        return out.toString();
    }

    private String moneyLine(Trade trade) {
        if (trade.getMoney() <= 0) {
            return "";
        }
        return "&7• &6" + Numbers.format(trade.getMoney()) + " &7monedas";
    }

    private String statusLine(Trade trade, int slot) {
        TradeService.Outcome outcome = plugin.tradeService().check(player, trader, pageIndex, slot, trade);
        String key;
        Map<String, String> placeholders = new HashMap<String, String>();
        switch (outcome.status) {
            case NO_PERMISSION:
                key = "no-permission";
                placeholders.put("permission", outcome.permission);
                break;
            case COOLDOWN:
                key = "cooldown";
                placeholders.put("time", String.valueOf(outcome.cooldownSeconds));
                break;
            case MISSING_MONEY:
                key = "missing-money";
                placeholders.put("money", Numbers.format(outcome.missingMoney));
                break;
            case NO_ECONOMY:
                key = "no-economy";
                break;
            case MISSING_ITEMS:
                key = "missing-items";
                break;
            case FULL:
                key = "full";
                break;
            default:
                key = "ready";
                break;
        }
        return plugin.menus().text("trade-menu.trade-item.status." + key, placeholders);
    }

    private void attemptTrade(Trade trade, int slot) {
        // El menu captura el tradeo al abrirse: si mientras tanto se elimino
        // el trader o el tradeo, la accion se descarta.
        Trader live = plugin.traderManager().byId(trader.getId());
        TradePage livePage = live == null ? null : live.getPage(pageIndex);
        if (livePage == null || livePage.getTrade(slot) != trade) {
            plugin.messages().send(player, "trader.not-found", Ph.of("id", trader.getId()));
            plugin.sounds().play(player, "error");
            player.closeInventory();
            return;
        }
        TradeService.Outcome outcome = plugin.tradeService().execute(player, trader, pageIndex, slot, trade);
        switch (outcome.status) {
            case OK:
                plugin.messages().send(player, "trade.success", Ph.of("result", ItemMatch.describe(trade.getResult())));
                if (outcome.dropped) {
                    plugin.messages().send(player, "trade.inventory-full");
                }
                plugin.sounds().play(player, "success");
                break;
            case COOLDOWN:
                plugin.messages().send(player, "trade.cooldown", Ph.of("time", outcome.cooldownSeconds));
                plugin.sounds().play(player, "error");
                break;
            case MISSING_ITEMS:
                plugin.messages().send(player, "trade.missing-items", Ph.of("items", outcome.missingItems));
                plugin.sounds().play(player, "error");
                break;
            case MISSING_MONEY:
                plugin.messages().send(player, "trade.missing-money",
                        Ph.of("money", Numbers.format(outcome.missingMoney), "balance", Numbers.format(outcome.balance)));
                plugin.sounds().play(player, "error");
                break;
            case NO_ECONOMY:
                plugin.messages().send(player, "trade.no-economy");
                plugin.sounds().play(player, "error");
                break;
            case NO_PERMISSION:
                plugin.messages().send(player, "trade.no-permission", Ph.of("permission", outcome.permission));
                plugin.sounds().play(player, "error");
                break;
            case FULL:
                plugin.messages().send(player, "trade.cancelled-full", Ph.of("result", ItemMatch.describe(trade.getResult())));
                plugin.sounds().play(player, "error");
                break;
            default:
                break;
        }
        refresh();
    }
}
