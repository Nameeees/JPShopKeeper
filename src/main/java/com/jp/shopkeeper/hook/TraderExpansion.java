package com.jp.shopkeeper.hook;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.Trade;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Placeholders de JPShopKeeper para Scoreboards, hologramas, tablists...
 *
 * <pre>
 *  %jpsk_traders%                       Numero de traders creados
 *  %jpsk_maxpages%                      Maximo de paginas por trader (config.yml)
 *  %jpsk_exists_&lt;id&gt;%                   si / no: existe ese trader
 *  %jpsk_name_&lt;id&gt;%                     Nombre visible del NPC (con colores)
 *  %jpsk_world_&lt;id&gt;%                    Mundo del NPC
 *  %jpsk_x_&lt;id&gt;% · _y_ · _z_            Coordenadas del NPC
 *  %jpsk_pages_&lt;id&gt;%                    Paginas del trader
 *  %jpsk_trades_&lt;id&gt;%                   Tradeos totales del trader
 *  %jpsk_cooldown_&lt;id&gt;_&lt;pagina&gt;_&lt;slot&gt;%  Segundos que le quedan al jugador (0 = listo)
 *  %jpsk_ready_&lt;id&gt;_&lt;pagina&gt;_&lt;slot&gt;%     si / no: el jugador puede comprar ya
 * </pre>
 *
 * El numero de pagina empieza en 1 (igual que en el menu) y el slot es el
 * numero visible del editor (1-28). Si un trader no existe, el placeholder
 * devuelve una cadena vacia.
 */
public final class TraderExpansion extends PlaceholderExpansion {

    private final JPShopKeeper plugin;

    public TraderExpansion(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "jpsk";
    }

    @Override
    public String getAuthor() {
        return "JP";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    /** Registrar dos veces (por ejemplo tras /trader reload) no debe avisar. */
    @Override
    public boolean register() {
        if (isRegistered()) {
            return true;
        }
        return super.register();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }
        String input = params.trim();
        if (input.isEmpty()) {
            return null;
        }
        int cut = input.indexOf('_');
        String head = (cut < 0 ? input : input.substring(0, cut)).toLowerCase(Locale.ROOT);
        String rest = cut < 0 ? "" : input.substring(cut + 1);

        if ("traders".equals(head)) {
            return String.valueOf(plugin.traderManager().all().size());
        }
        if ("maxpages".equals(head)) {
            return String.valueOf(plugin.configManager().maxPages());
        }
        if (rest.isEmpty()) {
            return null;
        }
        if ("cooldown".equals(head)) {
            return cooldown(player, rest);
        }
        if ("ready".equals(head)) {
            return cooldown(player, rest).equals("0") ? message("placeholder.yes") : message("placeholder.no");
        }

        Trader trader = plugin.traderManager().byId(rest);
        if ("exists".equals(head)) {
            return message(trader == null ? "placeholder.no" : "placeholder.yes");
        }
        if (trader == null) {
            return "";
        }
        if ("name".equals(head)) {
            return Text.color(trader.getDisplayName());
        }
        if ("world".equals(head)) {
            return trader.getHomeWorldName();
        }
        if ("pages".equals(head)) {
            return String.valueOf(trader.pageCount());
        }
        if ("trades".equals(head)) {
            return String.valueOf(trader.totalTrades());
        }
        if ("x".equals(head) || "y".equals(head) || "z".equals(head)) {
            return coordinate(trader, head);
        }
        return null;
    }

    /** rest = "&lt;id&gt;_&lt;pagina&gt;_&lt;slot&gt;" leido por la derecha: el id puede llevar _ y -. */
    private String cooldown(OfflinePlayer player, String rest) {
        if (!(player instanceof Player) || !player.isOnline()) {
            return "";
        }
        int last = rest.lastIndexOf('_');
        int prev = last > 0 ? rest.lastIndexOf('_', last - 1) : -1;
        if (prev <= 0) {
            return "";
        }
        Integer page = parsePositive(rest.substring(prev + 1, last));
        Integer displaySlot = parsePositive(rest.substring(last + 1));
        if (page == null || displaySlot == null) {
            return "";
        }
        Trader trader = plugin.traderManager().byId(rest.substring(0, prev));
        if (trader == null) {
            return "";
        }
        int pageIndex = page - 1;
        int slot = rawSlot(displaySlot);
        if (slot < 0) {
            return "";
        }
        TradePage tradePage = trader.getPage(pageIndex);
        Trade trade = tradePage == null ? null : tradePage.getTrade(slot);
        if (trade == null) {
            return "";
        }
        long remaining = plugin.tradeService().cooldownRemaining((Player) player, trader, pageIndex, slot, trade);
        return String.valueOf(remaining);
    }

    /** El editor numera los slots del 1 al 28; los datos guardan el slot real del cofre. */
    private int rawSlot(int displaySlot) {
        if (displaySlot >= 1 && displaySlot <= TradePage.GRID_SLOTS.length) {
            return TradePage.GRID_SLOTS[displaySlot - 1];
        }
        // Fuera del grid no hay tradeo: antes caia en otro slot y devolvia su enfriamiento.
        return -1;
    }

    private String coordinate(Trader trader, String axis) {
        org.bukkit.Location home = trader.getHome();
        if (home == null) {
            return "";
        }
        double value;
        if ("x".equals(axis)) {
            value = home.getX();
        } else if ("y".equals(axis)) {
            value = home.getY();
        } else {
            value = home.getZ();
        }
        return String.valueOf((int) Math.floor(value));
    }

    private String message(String key) {
        return plugin.messages().text(key);
    }

    private static Integer parsePositive(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 1 ? Integer.valueOf(value) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
