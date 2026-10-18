package com.jp.shopkeeper.command;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.menu.AdminMenu;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Entities;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * /trader — gestion completa: crear, editar, eliminar, renombrar,
 * listar, teletransportar y recargar. El panel de edicion visual se
 * abre con /trader edit o con shift + clic derecho sobre el NPC.
 */
public final class TraderCommand implements CommandExecutor, TabCompleter {

    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    private final JPShopKeeper plugin;

    public TraderCommand(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("help".equals(sub) || "ayuda".equals(sub)) {
            help(sender);
            return true;
        }
        if ("reload".equals(sub) || "recargar".equals(sub)) {
            reload(sender);
            return true;
        }
        if ("list".equals(sub) || "lista".equals(sub)) {
            list(sender);
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;

        if ("create".equals(sub) || "crear".equals(sub)) {
            create(player, args);
            return true;
        }
        if ("remove".equals(sub) || "eliminar".equals(sub)) {
            remove(player, args);
            return true;
        }
        if ("edit".equals(sub) || "editar".equals(sub)) {
            edit(player, args);
            return true;
        }
        if ("rename".equals(sub) || "renombrar".equals(sub)) {
            rename(player, args);
            return true;
        }
        if ("tp".equals(sub) || "ir".equals(sub)) {
            teleport(player, args);
            return true;
        }

        plugin.messages().send(sender, "general.unknown-subcommand", Ph.of("sub", args[0]));
        return true;
    }

    // ------------------------------------------------------------------
    //  Subcomandos
    // ------------------------------------------------------------------

    private void help(CommandSender sender) {
        plugin.messages().send(sender, "help.header");
        if (sender.hasPermission("jpshopkeeper.admin.create")) {
            plugin.messages().send(sender, "help.create");
        }
        if (sender.hasPermission("jpshopkeeper.admin.edit")) {
            plugin.messages().send(sender, "help.edit");
            plugin.messages().send(sender, "help.rename");
            plugin.messages().send(sender, "help.list");
        }
        if (sender.hasPermission("jpshopkeeper.admin.remove")) {
            plugin.messages().send(sender, "help.remove");
        }
        if (sender.hasPermission("jpshopkeeper.admin.teleport")) {
            plugin.messages().send(sender, "help.tp");
        }
        if (sender.hasPermission("jpshopkeeper.admin.reload")) {
            plugin.messages().send(sender, "help.reload");
        }
        plugin.messages().send(sender, "help.tip");
    }

    private void create(Player player, String[] args) {
        if (!player.hasPermission("jpshopkeeper.admin.create")) {
            noPermission(player, "jpshopkeeper.admin.create");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "help.create");
            return;
        }
        String id = args[1];
        if (!ID_PATTERN.matcher(id).matches()) {
            plugin.messages().send(player, "trader.invalid-id", Ph.of("id", id));
            plugin.sounds().play(player, "error");
            return;
        }
        if (plugin.traderManager().byId(id) != null || plugin.storage().exists(id)) {
            plugin.messages().send(player, "trader.already-exists", Ph.of("id", id));
            plugin.sounds().play(player, "error");
            return;
        }
        String typeName = args.length >= 3 ? args[2] : plugin.configManager().defaultType();
        if (!isAllowedType(typeName)) {
            plugin.messages().send(player, "trader.invalid-type", Ph.of("type", typeName));
            plugin.sounds().play(player, "error");
            return;
        }
        Trader trader = plugin.traderManager().create(id, player.getLocation(), typeName);
        if (trader == null) {
            plugin.messages().send(player, "trader.already-exists", Ph.of("id", id));
            return;
        }
        plugin.messages().send(player, "trader.created", Ph.of(
                "trader", trader.getId(),
                "type", trader.getEntityTypeName(),
                "world", trader.getHomeWorldName(),
                "coords", trader.homeString()
        ));
        plugin.sounds().play(player, "success");
        new AdminMenu(plugin, player, trader).open();
    }

    private void remove(Player player, String[] args) {
        if (!player.hasPermission("jpshopkeeper.admin.remove")) {
            noPermission(player, "jpshopkeeper.admin.remove");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "help.remove");
            return;
        }
        Trader trader = plugin.traderManager().byId(args[1]);
        if (trader == null) {
            plugin.messages().send(player, "trader.not-found", Ph.of("id", args[1]));
            plugin.sounds().play(player, "error");
            return;
        }
        plugin.traderManager().remove(trader.getId());
        plugin.messages().send(player, "trader.removed", Ph.of("trader", trader.getId()));
        plugin.sounds().play(player, "remove");
    }

    private void edit(Player player, String[] args) {
        if (!player.hasPermission("jpshopkeeper.admin.edit")) {
            noPermission(player, "jpshopkeeper.admin.edit");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "help.edit");
            return;
        }
        Trader trader = plugin.traderManager().byId(args[1]);
        if (trader == null) {
            plugin.messages().send(player, "trader.not-found", Ph.of("id", args[1]));
            plugin.sounds().play(player, "error");
            return;
        }
        new AdminMenu(plugin, player, trader).open();
    }

    private void rename(Player player, String[] args) {
        if (!player.hasPermission("jpshopkeeper.admin.edit")) {
            noPermission(player, "jpshopkeeper.admin.edit");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(player, "help.rename");
            return;
        }
        Trader trader = plugin.traderManager().byId(args[1]);
        if (trader == null) {
            plugin.messages().send(player, "trader.not-found", Ph.of("id", args[1]));
            plugin.sounds().play(player, "error");
            return;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(args[i]);
        }
        String displayName = Text.truncate(name.toString(), 32);
        plugin.traderManager().rename(trader, displayName);
        plugin.messages().send(player, "trader.renamed", Ph.of("trader", trader.getId(), "name", displayName));
        plugin.sounds().play(player, "set");
    }

    private void list(CommandSender sender) {
        if (!sender.hasPermission("jpshopkeeper.admin.edit")) {
            noPermission(sender, "jpshopkeeper.admin.edit");
            return;
        }
        Collection<Trader> traders = plugin.traderManager().all();
        if (traders.isEmpty()) {
            plugin.messages().send(sender, "trader.list-empty");
            return;
        }
        plugin.messages().send(sender, "trader.list-header", Ph.of("count", traders.size()));
        for (Trader trader : traders) {
            plugin.messages().send(sender, "trader.list-line", Ph.of(
                    "id", trader.getId(),
                    "world", trader.getHomeWorldName(),
                    "coords", trader.homeString(),
                    "type", trader.getEntityTypeName()
            ));
        }
    }

    private void teleport(Player player, String[] args) {
        if (!player.hasPermission("jpshopkeeper.admin.teleport")) {
            noPermission(player, "jpshopkeeper.admin.teleport");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "help.tp");
            return;
        }
        Trader trader = plugin.traderManager().byId(args[1]);
        if (trader == null) {
            plugin.messages().send(player, "trader.not-found", Ph.of("id", args[1]));
            plugin.sounds().play(player, "error");
            return;
        }
        Location home = plugin.traderManager().resolvedHome(trader);
        if (home == null) {
            plugin.messages().send(player, "trader.world-missing", Ph.of("world", trader.getHomeWorldName()));
            plugin.sounds().play(player, "error");
            return;
        }
        player.teleport(home);
        plugin.messages().send(player, "trader.teleported", Ph.of("trader", trader.getId()));
        plugin.sounds().play(player, "success");
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("jpshopkeeper.admin.reload")) {
            noPermission(sender, "jpshopkeeper.admin.reload");
            return;
        }
        plugin.configManager().load();
        plugin.messages().load();
        plugin.menus().load();
        plugin.sounds().reload();
        plugin.traderManager().reload();
        if (plugin.configManager().placeholderApi()) {
            // /trader reload tambien reevalua el hook (si ya estaba, no se duplica).
            com.jp.shopkeeper.hook.PlaceholderHook.register(plugin);
        }
        plugin.messages().send(sender, "general.reload-done", Ph.of("traders", plugin.traderManager().all().size()));
    }

    // ------------------------------------------------------------------
    //  Autocompletado
    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            String[] subs = {"help", "create", "edit", "remove", "rename", "list", "tp", "reload"};
            for (String sub : subs) {
                if (sub.startsWith(typed) && hasPermissionFor(sender, sub)) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("remove".equals(sub) || "edit".equals(sub) || "rename".equals(sub) || "tp".equals(sub)) {
                String typed = args[1].toLowerCase(Locale.ROOT);
                for (Trader trader : plugin.traderManager().all()) {
                    if (trader.getId().toLowerCase(Locale.ROOT).startsWith(typed)) {
                        out.add(trader.getId());
                    }
                }
            }
            return out;
        }
        if (args.length == 3 && "create".equals(args[0].toLowerCase(Locale.ROOT))) {
            String typed = args[2].toUpperCase(Locale.ROOT);
            for (String type : plugin.configManager().allowedTypes()) {
                // Solo se sugieren los mobs que existen en esta version.
                if (Entities.type(type) != null && type.toUpperCase(Locale.ROOT).startsWith(typed)) {
                    out.add(type);
                }
            }
        }
        return out;
    }

    private boolean hasPermissionFor(CommandSender sender, String sub) {
        if ("create".equals(sub)) {
            return sender.hasPermission("jpshopkeeper.admin.create");
        }
        if ("edit".equals(sub) || "rename".equals(sub) || "list".equals(sub)) {
            return sender.hasPermission("jpshopkeeper.admin.edit");
        }
        if ("remove".equals(sub)) {
            return sender.hasPermission("jpshopkeeper.admin.remove");
        }
        if ("tp".equals(sub)) {
            return sender.hasPermission("jpshopkeeper.admin.teleport");
        }
        if ("reload".equals(sub)) {
            return sender.hasPermission("jpshopkeeper.admin.reload");
        }
        return true;
    }

    private boolean isAllowedType(String typeName) {
        EntityType wanted = Entities.type(typeName);
        if (wanted == null) {
            return false;
        }
        List<String> allowed = plugin.configManager().allowedTypes();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        for (String name : allowed) {
            if (name.equalsIgnoreCase(typeName)) {
                return true;
            }
            EntityType allowedType = Entities.type(name);
            if (allowedType != null && allowedType == wanted) {
                return true;
            }
        }
        return false;
    }

    private void noPermission(CommandSender sender, String permission) {
        plugin.messages().send(sender, "general.no-permission", Ph.of("permission", permission));
        if (sender instanceof Player) {
            plugin.sounds().play((Player) sender, "error");
        }
    }
}
