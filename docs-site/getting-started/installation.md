# Installation

## Requirements

| Item | Value |
| --- | --- |
| Server | Spigot, Paper or any fork that implements the Bukkit API |
| Minecraft | 1.8 up to the latest version |
| Java | 8 or newer (the JAR is Java 8 bytecode) |
| Optional | Vault + an economy plugin, for trades that cost money |
| Optional | PlaceholderAPI, for the `%jpsk_...%` placeholders |

## Steps

1. Download `JPShopKeeper-1.1.0.jar` and drop it in the server's **plugins/** folder.
2. Start (or restart) the server. These files are created:

   ```
   plugins/JPShopKeeper/
   |-- config.yml        general options
   |-- messages.yml      every visible text (Spanish by default)
   |-- menus.yml         the look of the 6 menus
   |-- cooldowns.yml     created when a cooldown has to survive a restart
   \-- traders/          one file per trader
   ```

3. Optional: install **Vault** + an economy plugin for coin prices, and
   **PlaceholderAPI** if you want to use the placeholders.

## Your first trader

```
/trader create my_shop
```

An NPC is created where you are standing and its admin panel opens. From there:

- **Rename NPC** - the visible name (colours allowed).
- **Mob type** - cycles through `npc.allowed-types` in config.yml.
- **Pages and trades** - add a page, then click an empty slot to build a trade.
- **Preview as player** - see exactly what players will see.
- **Bring NPC here** - move it to your current position.

Inside a trade: click a **cost** or **result** slot, then click an item in your
inventory. Right click a slot to set the amount by chat, shift + right click to
clear it.

!!! tip "Players open the shop with a right click"
    The trade menu only opens for traders that have at least one page with a
    complete trade (cost and result).

## Updating

1. Replace the old JAR with the new one.
2. Restart the server.

Your traders, messages and menus are kept: the plugin only writes the files that
are missing. New options are added to the packaged defaults, so compare
`config.yml` with the release notes if you want to use them.

## Uninstalling

The NPCs are persistent mobs, so remove them **before** deleting the plugin:

```
/trader remove <id>      (for every trader)
```

Then delete the JAR and the `plugins/JPShopKeeper` folder.
