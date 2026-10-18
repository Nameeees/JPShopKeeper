# Compatibility

## Versions

| Item | Value |
| --- | --- |
| Minecraft | 1.8 up to the latest version |
| Server | Spigot, Paper and forks that implement the Bukkit API |
| Java | 8 or newer for the plugin |
| Built against | Spigot API 1.8.8, Java 8 bytecode |
| api-version | 1.13 (ignored by older servers, enables modern material handling on 1.13+) |
| Tested on | Paper 1.8.8 and Paper 26.3 (startup, NPCs, trades, menus, commands, reload and shutdown) |

## How the same JAR works everywhere

| Piece | 1.8 | 1.9 - 1.12 | 1.13+ | Latest |
| --- | --- | --- | --- | --- |
| Materials | modern names translated to `STAINED_GLASS_PANE:7` | modern names | modern names | modern names |
| Sounds | `CLICK`, `LEVEL_UP`... | `UI_BUTTON_CLICK`... | same | enum, field or resource key |
| No AI | `NoAI` tag in the mob NBT | `setAI(false)` | same | same |
| Silent | `Silent` tag in the mob NBT | `setSilent(true)` | same | same |
| Item NBT | raw NBT | raw NBT | raw NBT | item components (`serializeAsBytes`) |
| Colours | `&` legacy | `&` legacy | `&` legacy + hex | + MiniMessage |
| Menu titles | 32 visible characters | 32 | longer | longer |

Materials, sounds and entities are resolved **by name at runtime**, and anything
that does not exist on a version is simply skipped. The plugin never calls
`Material.name()` blindly: on some modern builds that method returns truncated
names or throws, so the registry key is used first.

## Things that are NOT supported

| Missing | Why |
| --- | --- |
| **Folia** | The plugin uses the Bukkit scheduler (single thread); Folia needs region schedulers. It is detected, a clear message is logged and the plugin disables itself instead of misbehaving. |
| **Brigadier commands** | The legacy executor with tab completion works on every version; a Brigadier tree would not add any feature. |
| **Native dialogs (1.21.6+)** | They do not exist before 1.21.6 and the plugin is built on chest menus to support 1.8+. |
| **Display entities** | NPCs are native mobs by design; display entities would break the 1.8 support. |
