# Configuration

Three files hold everything: `config.yml`, `messages.yml` and `menus.yml`. All of
them are reloaded with `/trader reload` (no restart needed).

## config.yml - general

| Key | Default | Meaning |
| --- | --- | --- |
| `settings.language` | `es` | Language of every text: `es`, `en` or your own file |
| `settings.mini-message` | `false` | Enables MiniMessage tags on Paper 1.16+ |
| `settings.debug` | `false` | Extra console information (interactions, loaded files...) |
| `settings.log-npc-respawn` | `true` | Warns when an NPC had to be regenerated |

## config.yml - NPCs

| Key | Default | Meaning |
| --- | --- | --- |
| `npc.default-type` | `VILLAGER` | Mob used by `/trader create` when none is given |
| `npc.allowed-types` | villager + 14 more | Mobs allowed for traders; types that do not exist on your version are skipped with a warning |
| `npc.look-at-players` | `true` | NPCs turn their head towards the nearest player |
| `npc.look-radius` | `6.0` | Radius (blocks) for that look |
| `npc.maintenance-interval-ticks` | `40` | How often positions and looks are checked |
| `npc.anchor-interval-ticks` | `1` | Anti-push anchoring; `1` means completely immovable |
| `npc.snap-distance` | `0.35` | How far an NPC may drift before being put back |
| `npc.protect` | `true` | Cancels damage, fire, portals and leads on the NPCs |
| `npc.force-persistent` | `true` | The NPC does not despawn with distance |

## config.yml - trades

| Key | Default | Meaning |
| --- | --- | --- |
| `trades.match.name` | `true` | Cost items must have the same custom name |
| `trades.match.lore` | `false` | ...and the same lore |
| `trades.match.enchantments` | `true` | ...and the same enchantments |
| `trades.match.durability` | `true` | ...and the same durability |
| `trades.match.nbt` | `true` | ...and the same custom NBT (tokens, vouchers...) |
| `trades.drop-result-if-full` | `true` | With a full inventory the result drops on the ground instead of cancelling |
| `trades.max-pages` | `45` | Maximum pages per trader |
| `trades.max-cost-slots` | `3` | Cost slots used by the editor (1-3) |

### Sounds and hooks

```yaml
sounds:
  enabled: true
  volume: 0.7
  pitch: 1.0
  open:    [UI_BUTTON_CLICK, CLICK]      # first name available on your version wins
  success: [ENTITY_PLAYER_LEVELUP, LEVEL_UP]
  error:   [ENTITY_VILLAGER_NO, VILLAGER_NO]

hooks:
  placeholderapi: true                    # only if PlaceholderAPI is installed
```

## menus.yml

Every menu (trade menu, admin panel, page list, page editor, trade editor and
the confirmation menu) is described here: title, rows, buttons, filler and lore
templates. Buttons can be moved by editing their `slot`.

Every icon accepts:

| Key | Version | Meaning |
| --- | --- | --- |
| `material` | all | Item of the icon (modern or legacy names) |
| `name`, `lore` | all | Text of the icon |
| `glow` | all | Enchantment glint |
| `custom-model-data` | 1.14+ | Resource pack texture |
| `head` | all | Player head (the player must be online or in the server cache) |

## Trader files

`plugins/JPShopKeeper/traders/<id>.yml` - one file per trader, editable by hand
(run `/trader reload` afterwards):

```yaml
display-name: "&aMerchant"
entity-type: VILLAGER
profession: ""
entity-uuid: 00000000-0000-0000-0000-000000000000
look-at-players: true
location:
  world: world
  x: 100.5
  y: 64.0
  z: -20.5
  yaw: 180.0
pages:
  '0':
    name: "Basics"
    trades:
      '10':                 # visible slot of the grid
        cost:
          '0':
            item: <item>    # standard Bukkit item map
            nbt: "<base64>" # only when the item carries custom NBT
        result:
          item: <item>
        money: 500.0        # Vault price, optional
        permission: ""      # required permission, optional
        cooldown: 30        # seconds per player, optional
```

!!! warning "Trader file names"
    The file name is the trader id and must be lowercase. If the plugin finds a
    file with uppercase letters it renames it (and warns if both exist), because
    saves always go to the lowercase file.
