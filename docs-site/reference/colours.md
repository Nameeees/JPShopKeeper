# Colours and format

All texts of the plugin use the same formats, in `messages.yml`, `menus.yml` and
the trader files.

## Legacy codes

Compatible with every version:

```
&aGreen   &cRed   &lBold   &oItalic   &rReset
```

## Hex colours (1.16+)

```
&#ff0000Red text
&#00ff88Green text
```

On servers older than 1.16 the code is **approximated to the closest legacy
colour**, so the same file works everywhere instead of showing raw codes.

## MiniMessage (Paper 1.16+, optional)

Enable `settings.mini-message: true` and you can use Adventure tags:

```
<red>Red text</red>
<gradient:#00ff00:#0088ff>Gradient</gradient>
<bold>Bold</bold>
```

Legacy `&` codes and `&#RRGGBB` keep working at the same time, so you can mix
both in the same line. On Spigot the option has no effect (the console warns
about it) and the text is shown as it is.

!!! warning
    MiniMessage is parsed before the legacy codes. If a text is not valid
    MiniMessage the plugin shows it unchanged instead of losing it, but a stray
    `<` in a text can be interpreted as a tag: keep `settings.mini-message`
    disabled if you do not need it.

## Where colours apply

| File | Applies to |
| --- | --- |
| `messages.yml` | Chat messages of the plugin |
| `menus.yml` | Menu titles, button names and lore |
| `traders/<id>.yml` | NPC display name (the `display-name` field) |
