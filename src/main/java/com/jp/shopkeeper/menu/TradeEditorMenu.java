package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.manager.ChatInputManager;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.ItemBuilder;
import com.jp.shopkeeper.util.ItemMatch;
import com.jp.shopkeeper.util.Numbers;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Editor de un tradeo individual. Flujo de configuracion:
 *  - Clic izquierdo en un slot de coste/resultado: lo selecciona.
 *  - Clic en un item de TU inventario: lo asigna al slot seleccionado.
 *  - Clic derecho en un slot con item: ajustar cantidad (chat).
 *  - Shift + clic derecho: vaciar el slot.
 * Dinero, permiso y enfriamiento se editan por chat. Todo se guarda al vuelo.
 */
public class TradeEditorMenu extends Menu {

    private static final int RESULT_SELECTION = 3;

    private final Trader trader;
    private final int pageIndex;
    private final int slot;
    private int selection = -1;

    public TradeEditorMenu(JPShopKeeper plugin, Player player, Trader trader, int pageIndex, int slot) {
        super(plugin, player);
        this.trader = trader;
        this.pageIndex = pageIndex;
        this.slot = slot;
    }

    @Override
    public void open() {
        TradePage page = trader.getPage(pageIndex);
        if (page == null) {
            new PageEditorMenu(plugin, player, trader, pageIndex).open();
            return;
        }
        if (page.getTrade(slot) == null) {
            page.setTrade(slot, new Trade());
        }
        super.open();
    }

    @Override
    protected String title() {
        return plugin.menus().title("trade-editor-menu", Ph.of(
                "slot", TradePage.displaySlot(slot),
                "page", pageIndex + 1,
                "trader", Text.truncate(Text.color(trader.getDisplayName()), 12)
        ));
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("trade-editor-menu", 6);
    }

    private Trade trade() {
        TradePage page = trader.getPage(pageIndex);
        return page == null ? null : page.getTrade(slot);
    }

    @Override
    protected void populate() {
        Trade trade = trade();
        if (trade == null) {
            return;
        }
        fill(plugin.menus().fillerSlots("trade-editor-menu"), plugin.menus().filler("trade-editor-menu"));

        set(plugin.menus().slot("trade-editor-menu", "info", 4), plugin.menus().button("trade-editor-menu", "info", null));

        int maxCost = plugin.configManager().maxCostSlots();
        for (int index = 0; index < Trade.COST_SLOTS; index++) {
            final int costIndex = index;
            int uiSlot = plugin.menus().slot("trade-editor-menu", "cost-" + (index + 1), 19 + index);
            if (index >= maxCost) {
                set(uiSlot, plugin.menus().filler("trade-editor-menu"));
                continue;
            }
            ItemStack cost = trade.getCost(index);
            if (cost == null) {
                ItemStack placeholder = plugin.menus().item("trade-editor-menu.cost-placeholder", Ph.of("index", index + 1));
                if (placeholder != null && selection == index) {
                    ItemBuilder.glint(placeholder);
                }
                set(uiSlot, placeholder, new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        handleSlotClick(event, costIndex);
                    }
                });
            } else {
                ItemStack display = cost.clone();
                if (selection == index) {
                    ItemBuilder.glint(display);
                }
                set(uiSlot, display, new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        handleSlotClick(event, costIndex);
                    }
                });
            }
        }

        set(plugin.menus().slot("trade-editor-menu", "arrow", 22), plugin.menus().button("trade-editor-menu", "arrow", null));

        ItemStack result = trade.getResult();
        int resultSlot = plugin.menus().slot("trade-editor-menu", "result", 23);
        if (result == null) {
            ItemStack placeholder = plugin.menus().item("trade-editor-menu.result-placeholder", null);
            if (placeholder != null && selection == RESULT_SELECTION) {
                ItemBuilder.glint(placeholder);
            }
            set(resultSlot, placeholder, new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    handleSlotClick(event, RESULT_SELECTION);
                }
            });
        } else {
            ItemStack display = result.clone();
            if (selection == RESULT_SELECTION) {
                ItemBuilder.glint(display);
            }
            set(resultSlot, display, new Action() {
                @Override
                public void run(InventoryClickEvent event) {
                    handleSlotClick(event, RESULT_SELECTION);
                }
            });
        }

        set(plugin.menus().slot("trade-editor-menu", "money", 28),
                plugin.menus().button("trade-editor-menu", "money", Ph.of("money", Numbers.format(trade.getMoney()))),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        if (event.isRightClick()) {
                            Trade current = trade();
                            if (current == null) {
                                return;
                            }
                            current.setMoney(0);
                            plugin.traderManager().save(trader);
                            plugin.messages().send(player, "editor.money-set", Ph.of("money", "0"));
                            plugin.sounds().play(player, "remove");
                            refresh();
                        } else {
                            promptMoney();
                        }
                    }
                });

        String permissionLabel = trade.hasPermission()
                ? trade.getPermission() : plugin.messages().text("editor.none-label");
        set(plugin.menus().slot("trade-editor-menu", "permission", 30),
                plugin.menus().button("trade-editor-menu", "permission", Ph.of("permission", permissionLabel)),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        promptPermission();
                    }
                });

        set(plugin.menus().slot("trade-editor-menu", "cooldown", 32),
                plugin.menus().button("trade-editor-menu", "cooldown", Ph.of("time", trade.getCooldownSeconds())),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        promptCooldown();
                    }
                });

        set(plugin.menus().slot("trade-editor-menu", "save", 38), plugin.menus().button("trade-editor-menu", "save", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        saveTrade();
                    }
                });

        set(plugin.menus().slot("trade-editor-menu", "delete", 42), plugin.menus().button("trade-editor-menu", "delete", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        confirmDelete();
                    }
                });

        set(plugin.menus().slot("trade-editor-menu", "back", 45), plugin.menus().button("trade-editor-menu", "back", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        new PageEditorMenu(plugin, player, trader, pageIndex).open();
                    }
                });

        set(plugin.menus().slot("trade-editor-menu", "close", 53), plugin.menus().button("trade-editor-menu", "close", null),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        player.closeInventory();
                    }
                });
    }

    // ------------------------------------------------------------------
    //  Seleccion y asignacion de items
    // ------------------------------------------------------------------

    private void handleSlotClick(InventoryClickEvent event, int index) {
        if (event.isShiftClick() && event.isRightClick()) {
            clearSlot(index);
            return;
        }
        if (event.isRightClick()) {
            promptAmount(index);
            return;
        }
        select(index);
    }

    private void select(int index) {
        selection = index;
        plugin.messages().send(player, "editor.slot-selected", Ph.of("slot", labelFor(index)));
        plugin.sounds().play(player, "set");
        refresh();
    }

    private String labelFor(int index) {
        if (index == RESULT_SELECTION) {
            return Text.strip(plugin.messages().text("editor.label-result"));
        }
        return Text.strip(plugin.messages().text("editor.label-cost", Ph.of("index", index + 1)));
    }

    private void clearSlot(int index) {
        Trade trade = trade();
        if (trade == null) {
            return;
        }
        if (index == RESULT_SELECTION) {
            trade.setResult(null);
        } else {
            trade.setCost(index, null);
        }
        plugin.traderManager().save(trader);
        plugin.messages().send(player, "editor.item-cleared", Ph.of("slot", labelFor(index)));
        plugin.sounds().play(player, "remove");
        refresh();
    }

    /** Cantidad maxima real de ese item: la de una pila, nunca mas. */
    private static int maxAmount(ItemStack item) {
        return Numbers.clamp(item.getType().getMaxStackSize(), 1, Trade.MAX_AMOUNT);
    }

    private void promptAmount(final int index) {
        Trade trade = trade();
        if (trade == null) {
            return;
        }
        final ItemStack current = index == RESULT_SELECTION ? trade.getResult() : trade.getCost(index);
        if (current == null) {
            select(index);
            return;
        }
        final String itemName = Text.strip(Text.itemName(current));
        plugin.chatInput().request(player, "editor.prompt-amount", Ph.of("item", itemName), new ChatInputManager.Input() {
            @Override
            public void accept(Player target, String text) {
                Integer amount = Numbers.parseIntOrNull(text);
                TradePage page = trader.getPage(pageIndex);
                Trade tradeNow = page == null ? null : page.getTrade(slot);
                if (tradeNow == null) {
                    return;
                }
                ItemStack current = index == RESULT_SELECTION ? tradeNow.getResult() : tradeNow.getCost(index);
                if (current == null) {
                    return;
                }
                int max = maxAmount(current);
                if (amount == null || amount < 1 || amount > max) {
                    plugin.messages().send(target, "general.invalid-integer",
                            Ph.of("input", text, "min", 1, "max", max));
                    plugin.sounds().play(target, "error");
                    return;
                }
                ItemStack copy = current.clone();
                copy.setAmount(amount);
                if (index == RESULT_SELECTION) {
                    tradeNow.setResult(copy);
                } else {
                    tradeNow.setCost(index, copy);
                }
                plugin.traderManager().save(trader);
                plugin.messages().send(target, "editor.amount-set", Ph.of("item", itemName, "amount", amount));
                plugin.sounds().play(target, "set");
                new TradeEditorMenu(plugin, target, trader, pageIndex, slot).open();
            }
        });
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        Trade trade = trade();
        if (trade == null || selection < 0) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ItemStack copy = clicked.clone();
        copy.setAmount(Math.min(copy.getAmount(), maxAmount(copy)));
        if (selection == RESULT_SELECTION) {
            trade.setResult(copy);
        } else {
            trade.setCost(selection, copy);
        }
        String label = labelFor(selection);
        selection = -1;
        plugin.traderManager().save(trader);
        plugin.messages().send(player, "editor.item-set", Ph.of("slot", label, "item", ItemMatch.describe(copy)));
        plugin.sounds().play(player, "set");
        refresh();
    }

    // ------------------------------------------------------------------
    //  Dinero, permiso y enfriamiento
    // ------------------------------------------------------------------

    private void promptMoney() {
        plugin.chatInput().request(player, "editor.prompt-money", null, new ChatInputManager.Input() {
            @Override
            public void accept(Player target, String text) {
                Double value = Numbers.parseDoubleOrNull(text);
                if (value == null || value < 0) {
                    plugin.messages().send(target, "general.invalid-number", Ph.of("input", text));
                    plugin.sounds().play(target, "error");
                    return;
                }
                reopen(target, new EditorAction() {
                    @Override
                    public void apply(Player p, Trade trade) {
                        trade.setMoney(value);
                        plugin.messages().send(p, "editor.money-set", Ph.of("money", Numbers.format(value)));
                    }
                });
            }
        });
    }

    private void promptPermission() {
        plugin.chatInput().request(player, "editor.prompt-permission",
                Ph.of("none", plugin.messages().text("editor.none-word")),
                new ChatInputManager.Input() {
                    @Override
                    public void accept(Player target, final String text) {
                        final String stripped = text.trim();
                        final String noneWord = Text.strip(plugin.messages().text("editor.none-word")).trim();
                        reopen(target, new EditorAction() {
                            @Override
                            public void apply(Player p, Trade trade) {
                                if (stripped.isEmpty() || stripped.equalsIgnoreCase(noneWord)) {
                                    trade.setPermission("");
                                    plugin.messages().send(p, "editor.permission-cleared");
                                } else {
                                    trade.setPermission(stripped);
                                    plugin.messages().send(p, "editor.permission-set", Ph.of("permission", stripped));
                                }
                            }
                        });
                    }
                });
    }

    private void promptCooldown() {
        plugin.chatInput().request(player, "editor.prompt-cooldown", null, new ChatInputManager.Input() {
            @Override
            public void accept(Player target, String text) {
                Integer value = Numbers.parseIntOrNull(text);
                if (value == null || value < 0 || value > 86400) {
                    plugin.messages().send(target, "general.invalid-integer",
                            Ph.of("input", text, "min", 0, "max", 86400));
                    plugin.sounds().play(target, "error");
                    return;
                }
                reopen(target, new EditorAction() {
                    @Override
                    public void apply(Player p, Trade trade) {
                        trade.setCooldownSeconds(value);
                        plugin.messages().send(p, "editor.cooldown-set", Ph.of("time", value));
                    }
                });
            }
        });
    }

    private interface EditorAction {
        void apply(Player target, Trade trade);
    }

    /** Aplica un cambio al tradeo actual, guarda y reabre el editor. */
    private void reopen(Player target, EditorAction action) {
        TradePage page = trader.getPage(pageIndex);
        Trade trade = page == null ? null : page.getTrade(slot);
        if (trade == null) {
            return;
        }
        action.apply(target, trade);
        plugin.traderManager().save(trader);
        plugin.sounds().play(target, "set");
        new TradeEditorMenu(plugin, target, trader, pageIndex, slot).open();
    }

    // ------------------------------------------------------------------
    //  Guardar / eliminar
    // ------------------------------------------------------------------

    private void saveTrade() {
        Trade trade = trade();
        if (trade == null) {
            return;
        }
        if (trade.getResult() == null) {
            plugin.messages().send(player, "editor.trade-incomplete");
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.traderManager().save(trader);
        plugin.messages().send(player, "editor.trade-saved");
        plugin.sounds().play(player, "success");
        new PageEditorMenu(plugin, player, trader, pageIndex).open();
    }

    private void confirmDelete() {
        new ConfirmMenu(plugin, player,
                plugin.messages().text("confirm.delete-trade.question", Ph.of("slot", TradePage.displaySlot(slot))),
                plugin.messages().lines("confirm.delete-trade.details", null),
                new Runnable() {
                    @Override
                    public void run() {
                        TradePage page = trader.getPage(pageIndex);
                        if (page != null) {
                            page.removeTrade(slot);
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
                        new TradeEditorMenu(plugin, player, trader, pageIndex, slot).open();
                    }
                }).open();
    }

    @Override
    public void onClosed() {
        // Cerrar el inventario forma parte del prompt de chat (dinero, permiso,
        // enfriamiento): si se borrara el tradeo vacio, lo tecleado se perderia.
        if (plugin.chatInput().isWaiting(player)) {
            return;
        }
        Trade trade = trade();
        if (trade != null && trade.isEmpty()) {
            TradePage page = trader.getPage(pageIndex);
            if (page != null) {
                page.removeTrade(slot);
                plugin.traderManager().save(trader);
            }
        }
    }
}
