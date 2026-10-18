# Placeholders

Available with **PlaceholderAPI** installed and `hooks.placeholderapi: true`.

| Placeholder | Returns |
| --- | --- |
| `%jpsk_traders%` | Number of traders |
| `%jpsk_maxpages%` | Maximum pages per trader (config.yml) |
| `%jpsk_exists_<id>%` | yes / no |
| `%jpsk_name_<id>%` | Visible NPC name (with colours) |
| `%jpsk_world_<id>%` | World of the NPC |
| `%jpsk_x_<id>%` · `%jpsk_y_<id>%` · `%jpsk_z_<id>%` | Coordinates of the NPC |
| `%jpsk_pages_<id>%` | Pages of the trader |
| `%jpsk_trades_<id>%` | Total trades of the trader |
| `%jpsk_cooldown_<id>_<page>_<slot>%` | Seconds left for that player (0 = ready) |
| `%jpsk_ready_<id>_<page>_<slot>%` | yes / no |

The page number starts at 1 (like in the menu) and the slot is the visible
number shown in the editor (1-28). If a trader does not exist the placeholder
returns an empty string, and the "yes"/"no" texts come from
`messages.yml` (`placeholder.yes` / `placeholder.no`).

## Example: a scoreboard (DeluxeScoreboard, TAB...)

```
Lines:
  - '&7Trades available: &a%jpsk_traders%'
  - '&7Diamond offer: &f%jpsk_cooldown_my_shop_1_3%s'
```

## Checking them in game

```
/papi parse me %jpsk_traders%
```
