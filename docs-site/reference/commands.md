# Commands and permissions

## Commands

Aliases: `/trader`, `/traders`, `/jpsk`. Tab completion included.

| Command | Permission | What it does |
| --- | --- | --- |
| `/trader help` | - | Shows the commands you can use |
| `/trader create <id> [mob]` | `jpshopkeeper.admin.create` | Creates a trader where you stand and opens its panel |
| `/trader edit <id>` | `jpshopkeeper.admin.edit` | Opens the editor panel |
| `/trader rename <id> <name>` | `jpshopkeeper.admin.edit` | Renames the NPC |
| `/trader list` | `jpshopkeeper.admin.edit` | Lists every trader |
| `/trader remove <id>` | `jpshopkeeper.admin.remove` | Deletes the trader, its NPC and its data |
| `/trader tp <id>` | `jpshopkeeper.admin.teleport` | Teleports you to the NPC |
| `/trader reload` | `jpshopkeeper.admin.reload` | Reloads config, texts, menus and traders |

## Permissions

| Permission | Default | Meaning |
| --- | --- | --- |
| `jpshopkeeper.use` | `true` | Open the trade menus |
| `jpshopkeeper.admin` | `op` | Full access (includes the five below) |
| `jpshopkeeper.admin.create` | `op` | Create traders |
| `jpshopkeeper.admin.edit` | `op` | Edit traders (buttons, panel, rename, list) |
| `jpshopkeeper.admin.remove` | `op` | Delete traders |
| `jpshopkeeper.admin.teleport` | `op` | Teleport to a trader |
| `jpshopkeeper.admin.reload` | `op` | Reload the plugin |

Each trade can also require **its own permission** (the "Permission" field of
the trade editor), which is perfect for VIP offers.

## Examples

```
# LuckPerms: a rank that can edit shops
/lp group admin permission set jpshopkeeper.admin true

# A trade that only VIP players can use
# (trade editor -> Permission -> vip.shop.diamond)
/lp group vip permission set vip.shop.diamond true
```

!!! tip "Console"
    `/trader help`, `/trader list` and `/trader reload` work from the console;
    the rest need a player, because they use your position or open a menu.
