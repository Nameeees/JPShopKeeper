# FAQ

**Do I need Vault?**
No. Item trades work on their own. Trades that cost money show "the server has no
economy" and charge nothing when Vault is missing.

**Do the NPCs survive restarts?**
Yes. Their UUID and position are saved; on startup the plugin adopts them and, if
one is missing (crash, `/kill`), regenerates it automatically.

**Can I use player skins on the NPCs?**
Not on the NPC itself: to keep one single JAR working from 1.8 to the latest
version the NPCs are native mobs. You can use player heads as **menu icons**.

**Can I clone a trader?**
Copy its file in `traders/`, change the file name (that is the id) and run
`/trader reload`. Adjust its `location` so it does not overlap.

**How do I change the language?**
Set `settings.language: en` in `config.yml` and run `/trader reload`. See
[Languages](getting-started/languages.md).

**What happens if I uninstall the plugin?**
The NPCs stay in the world (they are persistent, invulnerable mobs). Remove them
first with `/trader remove <id>`.

**The console keeps saying the NPC is being regenerated and nothing appears.**
Something is cancelling mob spawning there: a WorldGuard region with
`mob-spawning: deny`, claims that block mobs or mob cleaners. Move the trader to
an allowed area or relax that protection; the plugin retries every 30 seconds and
logs the exact coordinates.

**The NPC appears but disappears after a while.**
Something is removing entities after creation (cleaners, mob stackers,
`kill mobs` commands). The console tells you which trader is affected: exclude
the shop NPCs from that plugin.

**Can two admins edit at the same time?**
Yes. If a trader is reloaded while a menu is open, the click is applied to the
live trader instead of overwriting the file with stale data.

**Is there a metrics or update checker?**
No. The plugin makes no network calls at all.
