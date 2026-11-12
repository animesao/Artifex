# Artifex — Samples

Ready-made examples for the plugin. **They are not installed and are not
shipped inside the plugin jar** — a fresh install contains exactly one file:
`plugins/Artifex/items/messages.java`.

Everything here is meant to be copied into your server's `items/` folder.

## How to use a sample

1. Copy the file (or the whole folder) you need into
   `plugins/Artifex/items/`
2. Remove the `EXAMPLE-` prefix from the name
   - `EXAMPLE-runes.yml` → `runes.yml`
   - `EXAMPLE-vault/` → `vault/`
3. Run `/ci reload`

The prefix exists so that a sample can never be activated by accident.
Files and folders that still carry it are ignored by the loaders.

## What is inside

| Path | What it shows |
|------|---------------|
| `EXAMPLE-*.yml` | YAML items: effects, attributes, armor sets, uses, duration, recipes, triggers |
| `EXAMPLE-*.java` | Java API items, commands and placeholders |
| `EXAMPLE-gui-browser.java` | Interactive chest GUI catalog `/itembrowser` (categories, pagination, item giving) |
| `EXAMPLE-socket-gem.java` | Socketable gem (`ruby_gem`) for drag & drop socketing onto items |
| `EXAMPLE-socketable-blade.java` | Runebound greatsword initialized with 3 open gem sockets |
| `EXAMPLE-custom-drops.java` | Drop tables artifact (`echo_shard`) dropping from Warden, Enderman and Sculk |
| `EXAMPLE-cooldown-hud.java` | Mjolnir with visual Action Bar Cooldown HUD progress meter |
| `EXAMPLE-conditioned-blade.java` | Celestial Blade with day/night, storm and emergency health condition checks |
| `EXAMPLE-README-ACTIONS.md` | Full list of the actions you can use in `left-click-actions` / `right-click-actions` / `triggers` |
| `EXAMPLE-customcraft/` | GUI crafting module (`/craft`) |
| `EXAMPLE-deluxemenux/` | Menu module (kits, shop) |
| `EXAMPLE-shop/` | Simple shop module |
| `EXAMPLE-vault/` | Economy module with its own database |
| `EXAMPLES/` | Extra reference items |
| `_template/` | Starting point for your own module |
| `_template/player-diary/` | Example of a module that stores player data |

## Notes

- The samples are translated to English. Item names and lore are yours to
  change — they are ordinary YAML/Java values.
- `EXAMPLE-messages.java` documents every message the plugin can show.
  The plugin already creates a working `items/messages.java` for you.
- A sample can depend on another one (for example a recipe that uses a
  custom item). Copy both files.

## Messages

`items/messages.java` is the single place where the texts the player sees
are defined — the plugin creates it on install and you edit it directly.

A module plugs its own texts into the same system:

```java
@Override
protected void onEnable() {
    registerMessage("my-module.no-money", "&cNot enough money!");
}

void onBuy(Player player) {
    player.sendMessage(getMessage("my-module.no-money", "&cNot enough money!"));
}
```

An administrator then overrides it in `items/messages.java`:

```java
MessagesConfig.set("my-module.no-money", "&aПока не хватает монет!");
```

Priority: override from `items/messages.java` → the module's own text →
the plugin's built-in message → the fallback default.

