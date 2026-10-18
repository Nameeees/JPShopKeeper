# Changelog

## 1.1.0 - first public release

**Trade NPCs with pages and offers, one JAR from 1.8 to the latest version.**

### Features

- Native-mob trade NPCs: invulnerable, no AI, silent, anchored, looking at nearby
  players and regenerated automatically.
- Trades with up to 3 cost items, coin price (Vault), result, per-trade
  permission and per-player cooldown.
- Full NBT support and configurable item matching.
- Up to 45 pages per trader, 28 trades per page, all edited from chest menus.
- Spanish and English included; any other language is one file.

### Modern extras

- `&#RRGGBB` hex colours, native on 1.16+ and approximated on older versions.
- Optional MiniMessage on Paper 1.16+.
- `custom-model-data` (1.14+), player heads and long titles.
- PlaceholderAPI placeholders and cooldowns that survive restarts.

### Fixed before release

- Menus no longer break on the newest Paper builds (material names).
- A cost can no longer be paid with the armour you are wearing (1.9+).
- 1.8: NPCs are really AI-free and silent, and enchanted items can be traded.
- Safer trader files (atomic writes, lowercase names), no orphan NPCs, no lost
  chat input.
- Menus render about 7 times faster.

The full, always up to date list lives in
[CHANGELOG.md](https://github.com/Nameeees/JPShopKeeper/blob/main/CHANGELOG.md).
