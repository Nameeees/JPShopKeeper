package com.jp.shopkeeper.menu;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.util.Ph;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Confirmacion generica para acciones destructivas: eliminar trader,
 * pagina o tradeo. Nunca se ejecuta nada al primer clic.
 */
public class ConfirmMenu extends Menu {

    private final String question;
    private final List<String> details;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(JPShopKeeper plugin, Player player, String question, List<String> details,
                       Runnable onConfirm, Runnable onCancel) {
        super(plugin, player);
        this.question = question;
        this.details = details;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected String title() {
        return plugin.menus().title("confirm-menu", null);
    }

    @Override
    protected int rows() {
        return plugin.menus().rows("confirm-menu", 3);
    }

    @Override
    protected void populate() {
        fill(plugin.menus().fillerSlots("confirm-menu"), plugin.menus().filler("confirm-menu"));

        StringBuilder detailsText = new StringBuilder();
        if (details != null) {
            for (String line : details) {
                if (detailsText.length() > 0) {
                    detailsText.append('\n');
                }
                detailsText.append(line);
            }
        }
        Map<String, String> placeholders = Ph.of("question", question, "details", detailsText.toString());

        set(plugin.menus().slot("confirm-menu", "confirm", 11),
                plugin.menus().button("confirm-menu", "confirm", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        plugin.sounds().play(player, "set");
                        if (onConfirm != null) {
                            onConfirm.run();
                        }
                    }
                });

        ItemStack info = plugin.menus().button("confirm-menu", "info", placeholders);
        set(plugin.menus().slot("confirm-menu", "info", 13), info);

        set(plugin.menus().slot("confirm-menu", "cancel", 15),
                plugin.menus().button("confirm-menu", "cancel", placeholders),
                new Action() {
                    @Override
                    public void run(InventoryClickEvent event) {
                        plugin.sounds().play(player, "open");
                        if (onCancel != null) {
                            onCancel.run();
                        } else {
                            player.closeInventory();
                        }
                    }
                });
    }
}
