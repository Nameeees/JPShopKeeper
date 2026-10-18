# Languages

## How it works

`settings.language` in `config.yml` decides which files are loaded:

| Value | Files | Notes |
| --- | --- | --- |
| `es` | `messages.yml` + `menus.yml` | Default. Spanish. |
| `en` | `messages_en.yml` + `menus_en.yml` | Extracted from the JAR on first use. |
| `xx` | `messages_xx.yml` + `menus_xx.yml` | Copy the base files and translate. |

The plugin always extracts the **base** file too, and uses it as a per-key
fallback: anything missing in the translation is taken from the base file
instead of showing an error to the player. The console tells you which file is
in use:

```
[JPShopKeeper] Textos: messages_en.yml (lo que falte se toma de messages.yml)
```

If the file of the selected language does not exist, the plugin warns and uses
the base file. The `/trader reload` command applies a language change without
restarting.

## Adding a language

1. Copy `messages.yml` to `messages_xx.yml` and translate the values.
2. Copy `menus.yml` to `menus_xx.yml` and translate the titles and lore.
3. Set `settings.language: xx` in `config.yml`.
4. Run `/trader reload`.

!!! tip
    Do not translate the keys, only the values. Colours and placeholders like
    `{trader}` or `{money}` must stay as they are.

## Placeholders inside the texts

| Placeholder | Meaning |
| --- | --- |
| `{prefix}` | The prefix defined at the top of the file |
| `{trader}`, `{id}` | Trader name and id |
| `{page}`, `{pages}` | Current page and total |
| `{money}`, `{balance}` | Price and the player's balance |
| `{items}` | Missing items |
| `{time}` | Cooldown seconds |
| `{permission}` | Required permission |
| `{count}`, `{slot}`, `{type}`, `{world}`, `{coords}` | Menu counters and trader data |
