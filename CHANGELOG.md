# Changelog

All Artifex (formerly DC-CustomItems) changes by version.

Section format: `## v<version>` — the release script (`.github/workflows/build.yml`)
extracts the section of the needed version for the GitHub Release notes.

---

## v2.3.0

### 🧱 Custom Block Engine (Paper 26.3 NoteBlock Matrix)
- **Zero-Entity Solid Blocks:** Implemented `CustomBlockManager` utilizing 800+ distinct NoteBlock states (`instrument + note + powered`) for solid collision, ambient occlusion, and 0 entity tick lag.
- **Realistic Mining Mechanics:** Hardness and required pickaxe tier checks (Wooden to Netherite), Silk Touch block retention, and Fortune I–III scaling drop multipliers.
- **Audio & Interaction Guards:** Suppresses default note block pitch tuning on right-click and mutes vanilla instrument sounds for custom blocks.
- **Configurable Settings:** Added `custom-blocks:` section to `config.yml` (`enabled`, `prevent-tuning`, `mute-notes`, `enable-fortune`).

### 🎨 High-Res 64x64 Textures & Blockbench 3D Models
- **Blockbench Tooling Integration:** Integrated `blockbench-3d-models` and `blockbench-pixelart-textures` skills with geometry validation (`model_validator.py`).
- **15 Procedural Textures:** 64x64 multi-zone pixel art textures with specular highlights and glowing ore veins for Ember, Frost, Storm, and Void branches.
- **13 .bbmodel Source Files:** Complete 3D projects for blocks and items in `blockbench_models/`.

### 🌌 Resource Forge Ecosystem & Iris Worldgen
- **Master Resource Catalog:** Complete specification of 150 materials across 9 RPG branches (`docs/resources/RESOURCE_MASTER_CATALOG.md`).
- **First Prototype Tier:** 10 custom materials (raw ores, shards, ingots) and 5 custom blocks (ores and storage block) registered in `items/resource_forge_prototype.yml` with furnace and crafting table recipes.
- **Iris Dimension Engine Worldgen:** Blueprint for custom ore vein distribution across Iris biomes (`docs/resources/IRIS_WORLDGEN_INTEGRATION.md`).

### 💬 Localized Hex Messages & Chat Formatting
- **MiniMessage & Hex Formatting:** Fixed `MessagesConfig.format()` and `colorize()` to natively parse `&#RRGGBB`, `<#RRGGBB>`, and MiniMessage tags via `ColorUtils`.
- **Informative Reload Output:** Localized reload messages (`/artifex reload`) with clear counts for YAML items, Java items, commands, placeholders, and modules.

---

## v2.2.0

### 🧭 Interactive GUI Catalog (`/ci menu` / `/ci gui`)
- **Chest GUI Item Browser:** Added an interactive 54-slot chest interface allowing players and administrators to browse, inspect, and retrieve any custom item by category (`ALL`, `TOOL`, `ARMOR`, `CONSUMABLE`, `RUNE`) with paginated navigation (36 items per page).
- **One-Click Item Delivery:** Supports giving 1 piece (Left-Click), giving a full stack of 64 (Right-Click), or dropping at player feet (Shift-Click).
- **New Permission:** `artifex.menu` (default: op).

### 💎 Gem Sockets & Rune Inlaying System
- **In-Game Drag & Drop Inlaying:** Players can drag a gem/rune from the cursor onto an item with open sockets in their inventory to permanently inlay it, consuming the gem and unlocking its stats.
- **Visual Socket Lore:** Sockets are displayed in item lore as graphical indicators (`&7Сокеты: &8[ &a● &7○ &8]`).
- **Core Engine & API:** Added `GemSocketManager`, `CustomItemSocketEvent` (cancellable), and static methods in `ItemAPI` (`setSockets`, `makeGem`, `insertGem`, `getMaxSockets`, `getUsedSockets`).

### ⏳ Live Action Bar Cooldown HUD
- **Graphical Cooldown Progress Meter:** When an item is on cooldown, clicking it renders a live progress bar on the Action Bar (`⏳ [Item] (1.4s) [■■■■■■□□□□]`) with tactile audio feedback.
- **Developer API:** Exposed `ItemAPI.sendCooldownBar(player, name, remainingMs, totalMs)`.

### 🧠 Action Conditions & Control Flow
- **Expression Evaluation:** Added condition syntax to `ActionParser`:
  - `condition: <expr> -> <action>` and `if: <expr> -> <action>` supporting `>`, `<`, `>=`, `<=`, `==`, `!=` for both numbers and strings.
  - Automatic placeholder resolution (`%player%`, `%health%`, `%food%`, `%level%`, `%world%`, `%biome%` and full PlaceholderAPI support).
- **Condition Action Prefixes:**
  - `chance:<0.0-1.0|X%>:<action>` — percentage or fractional chance execution.
  - `biome:<BIOME>:<action>` — execution only in specified biomes.
  - `world:<WORLD>:<action>` — execution restricted to a target world.
  - `permission:<PERM>:<action>` — execution gated by player permissions.
  - `sneaking:<action>` — execution triggered only while shifting/sneaking.

### 🎲 Mob & Block Drop Tables
- **Custom Drop Tables:** Configurable `drops.mobs` and `drops.blocks` sections for custom items.
- **Looting & Fortune Synergy:** Drops dynamically scale with vanilla Looting (for mob kills) and Fortune (for block mining) enchantment levels.

### 🎨 Modern 1.20.5+ / 1.21.x Item Components
- **Armor Trims:** Added support for `trim: { pattern: ..., material: ... }` via Bukkit/Paper `ArmorMeta` registry.
- **Glint Override:** Added `glint: true/false` directly controlling enchantment glint without dummy enchantments.
- **Custom Dye Colors:** Added `color: "#rrggbb"` parsing for leather armor and potions.
- **Item Exporter Support:** `/saveitem` automatically extracts trims, glint overrides, and colors into YAML.

### 📦 New Sample Java Classes in `samples/`
- `EXAMPLE-gui-browser.java`: Interactive catalog `/itembrowser` command.
- `EXAMPLE-socket-gem.java`: Socketable ruby gem (`ruby_gem`).
- `EXAMPLE-socketable-blade.java`: Runebound greatsword with 3 open sockets.
- `EXAMPLE-custom-drops.java`: Echo shard with Warden and Sculk drop tables.
- `EXAMPLE-cooldown-hud.java`: Mjolnir hammer with live Action Bar cooldown HUD.
- `EXAMPLE-conditioned-blade.java`: Celestial blade with day/night and low-health conditions.

---

## v2.1.0

### 🚀 In-Game Item Exporter Component
- **Export Held Item to Config (`/saveitem` & `/ci save`):** Added a component allowing administrators to hold any configured Minecraft item in hand and instantly serialize all of its properties into an editable YAML file under `plugins/Artifex/items/<id>.yml`.
  - Supports `/artifex save <id> [--force]`, `/ci save <id>`, `/ci export <id>`, `/ci create <id>`, and `/saveitem <id> [--force]`.
  - Guarded against accidental file overwriting with required `--force` flag.
  - Automatically invokes `itemHandler.reloadItems()` upon export for zero-downtime hot-reloading.
- **Deep Metadata Serialization (`ItemExporter`):**
  - **Color & HEX Restoration:** Introduced `ColorUtils.decolorize(String)` which converts Minecraft section symbol colors (`§c`, `§l`) and BungeeCord RGB hex strings (`§x§r§r§g§g§b§b`) back into clean configuration markup (`&c` and `&#rrggbb`).
  - **Full Enchantments & Flags:** Preserves all enchantments and `ItemFlag` attributes (`HIDE_ENCHANTS`, `HIDE_ATTRIBUTES`, etc.).
  - **Custom Model Data & Modern Item Models:** Preserves both legacy `custom-model-data` and Paper 1.21.4+ `item-model` resource pack definitions.
  - **Player Skull Textures:** Automatically extracts Base64 texture URLs from `SkullMeta` into `texture: "..."`.
  - **Attribute Modifiers:** Exports custom attribute amounts (`GENERIC_ATTACK_DAMAGE`, `GENERIC_ARMOR`, etc.).
  - **Smart Type & Slot Inference:** Automatically detects item category (`ARMOR`, `CONSUMABLE`, `TOOL`) and activation slot (`HEAD`, `CHEST`, `LEGS`, `FEET`, `OFFHAND`, `HAND`).
  - **Action & Trigger Templates:** Appends commented-out action blocks (`right-click-actions`, `effects`, `sounds`) and binds JSON Schema `# yaml-language-server: $schema=...` for instant autocompletion in VS Code and IntelliJ IDEA.
- **Permissions & Registration:**
  - Registered `artifex.save` permission (defaults to OP).
  - Registered `/saveitem` command with aliases `[ci-save, artifex-save]`.
- **Automated Tests:**
  - Added unit test suite `ItemExporterTest` and `ColorUtilsTest.testDecolorize`.

---

## v2.0.0

### 🛡️ Security & Strict Mode
- **Compiler Strict Security Mode (`security.strict-mode`):** Added configurable strict mode to `JavaItemCompiler`. When enabled, files containing dangerous calls (`System.exit`, `ProcessBuilder`, `Runtime.exec`, `.setAccessible`) are automatically rejected and barred from compilation.
- **Dangerous Token Inspection:** Introduced `JavaItemCompiler.findDangerousToken` with unit testing in `JavaItemCompilerTest`.

### ✨ Core Improvements & Bug Fixes
- **Player Context in Lore:** `LoreManager` and `CustomItemHandler` now support passing `Player` context to dynamically resolve PlaceholderAPI placeholders (`%player_name%`, stats, balance) in item lore upon cooldown and usage updates.
- **Decimal Formatting Fix:** Enforced `Locale.US` for all cooldown and timer string formatting in `LoreManager` and `PlayerListener`, preventing locale-dependent comma decimal separators (`3,5` vs `3.5`) on European and Russian server environments.
- **Configuration Parsing Fix:** Registered new root settings (`file-logging`, `crafting`, `items`, `cooldowns`, `security`) in `CONFIG_SKIP_KEYS` and `ConfigMigrator`, completely removing false-positive migration warnings and `Section 'item' not found` errors on startup.

### 📚 Documentation & Developer Experience
- **Complete Wiki & Docs Rebranding:** Updated all 19 articles in `wiki/` and `docs/` to reflect Artifex paths (`plugins/Artifex/`), commands (`/artifex`), permissions (`artifex.reload`), and database identifiers.
- **JSON Schema IDE Integration:** Documented `# yaml-language-server: $schema=...` in `README.md` and `wiki/03-yaml-items.md` for real-time validation and autocompletion in VS Code and IntelliJ IDEA.

---

## v1.340.0

### 🏛️ Complete Rebranding to Artifex
- **Plugin Rebranding:** Rebranded the entire project from **DC-CustomItems** (`dcustomitems`) to **Artifex** (`artifex`).
- **Java Package Migration:** Moved all classes from `me.dcplugin.dcustomitems` to `me.dcplugin.artifex`.
- **Runtime Script Backward Compatibility:** `JavaItemCompiler` automatically rewrites legacy `import me.dcplugin.dcustomitems` statements to `me.dcplugin.artifex` so existing custom `.java` scripts in `items/` continue to compile seamlessly.
- **PDC & NBT Backward Compatibility:** Custom item identifiers and counters (`custom_item_id`, `item_uses`, `acquired_time`) transparently read legacy namespaces (`dcustomitems:`, `dc-customitems:`) ensuring that existing items on servers remain 100% functional.
- **Command & Permission Compatibility:**
  - Primary command is `/artifex` (with aliases `/af`, `/art`, `/ci`, `/customitems`).
  - Commands check both `artifex.reload` and `dcustomitems.reload`.
- **PlaceholderAPI Multi-Identifier Support:** Registered `%artifex_...%` as the primary placeholder prefix while preserving `%dcustomitems_...%` and `%dci_...%` aliases.
- **Database Reconfiguration:** Default database name updated to `artifex`, connection pool to `Artifex-MySQL`, and environment variable support for `ARTIFEX_MYSQL_PASSWORD` (with fallback to `DCI_MYSQL_PASSWORD`).
- **Build & CI Artifacts:** Maven artifact and JAR renamed to `Artifex-${version}.jar`. Updated `build.sh` and GitHub Actions workflow accordingly.

---

## v1.339.0

### 🛡️ Bug Fixes & Exploits
- **SimpleModule Item Theft Fix:** `SimpleModule` now implements `InventoryHolder` and registers `InventoryClickEvent` and `InventoryDragEvent` handlers to prevent players from taking GUI items from the menu, and correctly dispatches `handleMenuClick` when slots are clicked.
- **Vanilla Crafting Protection:** Added `PrepareItemCraftEvent` listener to prevent vanilla recipes (e.g. torches, blocks, sticks) from accidentally or maliciously consuming custom items as raw ingredients. Configurable via `crafting.protect-custom-items-in-vanilla-recipes: true`.
- **Command Sequence Colon Parsing:** Fixed `ActionParser` command parsing in `command_sequence` to split on the last colon (`lastIndexOf(':')`), preventing commands with namespaces (`minecraft:give`, NBT tags, etc.) from being truncated.
- **Shutdown Task Safety:** Guarded `ActionParser.scheduleLater` with `plugin.isEnabled()` check to eliminate `IllegalPluginAccessException` when triggers fire during server shutdown or reload.

### ✨ Features & Modern Paper 1.21.4+ Upgrades
- **MiniMessage & Adventure Support:** `ColorUtils` now supports modern Adventure MiniMessage formatting (`<gradient:...>`, `<rainbow>`, `<hover:...>`, `<click:...>`) while preserving backward compatibility with legacy `&` color codes and HEX formats.
- **Modern Item Model Components:** Added native support for Mojang's 1.21.4+ `minecraft:item_model` data component via `meta.setItemModel(NamespacedKey)` across `ItemLoader`, `ItemAPI`, and `ItemBuilder`. Configurable via `item-model` or `model` in YAML.
- **Visual Hotbar Cooldown Animation:** Player client now displays native Minecraft cooldown overlay swipe (`player.setCooldown(material, ticks)`) when using items with click cooldowns. Configurable via `items.visual-cooldown-animation: true`.
- **Persistent Cooldowns (`CooldownStore`):** Integrated `CooldownStore` into `PlayerListener`, `TriggerListener`, and `PluginBootstrap` to persist player click and trigger cooldowns to `cooldowns.yml` across server restarts and reloads (`cooldowns.persistent: true`).
- **Maven Release 21:** Configured `<release>21</release>` in `maven-compiler-plugin` to eliminate Java 21 system module warnings during build.

---

## v1.338.0

### ⚡ Performance & Runtime Java Compiler
- **Batch Compilation:** `JavaItemCompiler` now compiles all changed source files in a single batch `javac` task, greatly reducing compilation overhead during server startup and reload. If any script contains syntax errors, the compiler automatically falls back to isolated single-file compilation to report specific diagnostics without breaking valid items.
- **PlayerMove Hot-Path Optimization:** `ApiEventListener.onPlayerMove` now checks whether Java items exist and whether position coordinates actually changed before reading metadata, eliminating unnecessary PDC reads on camera rotations.

### 🛡️ Bug Fixes & Stability
- **Attribute Stacking on Reload:** `AttributeManager.cleanup()` now strips attribute modifiers from all online players on plugin disable or reload, preventing attributes (e.g. max health, attack damage) from permanently stacking in player save files (`.dat`).
- **Jar ClassLoader Lifecycle:** `ItemRegistry` now safely manages `URLClassLoader` lifecycles, keeping classloaders active for runtime class loading and properly closing them upon shutdown and reload.
- **Full Equipment Triggers for Java Items:** `ApiEventListener` now checks both hands and all armor pieces on damage and death events (`onDamageTaken`, `onDeath`), so custom armor effects are no longer skipped when holding custom weapons.
- **Pickup Item Sync:** In `PlayerListener.onEntityPickupItem`, updated item metadata (such as acquisition time for expiring items) is now synchronized to the ground entity before pickup.
- **Click Anti-Spam:** Reduced `MIN_CLICK_DELAY` from 200ms to 50ms to allow normal player CPS for items without custom click cooldowns.
- **Sequenced Actions & Titles:** Fixed timing in `ActionParser` so sequential sounds and titles observe specified delays.
- **Safe Number Parsing:** Added defensive parsing in `ActionParser` to prevent unhandled `NumberFormatException` crashes.
- **Logger Restoration:** `PluginLogManager.stop()` now restores `useParentHandlers(true)`, preventing lost logs after reload.

### ✨ Features & Improvements
- **Configurable Armor Set Sizes:** `ArmorSetManager` now supports `required-pieces` (or `pieces`) in config, allowing 2-piece, 3-piece, or 4-piece set bonuses.
- **HEX Color Support:** `ColorUtils` now supports RGB hex formats (`&#RRGGBB`, `<#RRGGBB>`, and `{#RRGGBB}`).
- **Legacy Enchantment Fallback:** `EnumCache.getEnchantment` now falls back to `Enchantment.getByName()`, correctly resolving legacy Bukkit names (such as `DAMAGE_ALL`).
- **Dynamic Command & Brigadier Sync:** Dynamic commands and aliases are thoroughly cleaned up from Bukkit `CommandMap` on unload and synchronized with clients via `syncCommands()`.

---

## v1.337.0

### 📖 English release notes

- **The changelog is now in English.** The release script copies the section
  of the released version into the GitHub Release notes, so the notes of
  every future release will be in English — the same language the plugin
  itself now uses by default.
- The notes of the already published releases (v1.330.0 – v1.336.0) were
  rewritten in English as well, so the whole release history reads the
  same way.

### ⚠️ Update notes

- No code changes in this release — only documentation.

---

## v1.336.0

### 🔒 MySQL: server certificate verification

- New option `database.mysql.verify-server-certificate` (defaults to
  `false` — so the existing connections are not broken).
  With `use-ssl: true` + `verify-server-certificate: true` the
  `sslMode=VERIFY_IDENTITY` is used: both the certificate and the hostname are checked.
- Previously `use-ssl: true` only encrypted the connection without checking the certificate —
  MITM was still possible.
- The plugin writes a warning to the log if the connection is not encrypted or
  the certificate is not verified, and if the password lies in `config.yml` in plain
  text (a hint about `DCI_MYSQL_PASSWORD`).

---

## v1.335.0

### 🔒 Security

Full security review of the plugin (attack-surface mapping, input-to-sink
tracing, authorization, concurrency, data exposure, dependencies).

- **Module commands did not check their permission at all.**
  `registerDynamicCommand(name, executor, permission)` only *set* the
  permission — `Command.setPermission()` just fills a field, and
  `SimpleCommandMap.dispatch()` never calls `testPermission()` (only
  `PluginCommand` does, and the plugin registers a raw `Command`).
  **Any player could run every module command**, including the admin ones
  from the samples: `/eco give|take|set|reset <player> <amount>` gave full
  control over the economy. The permission is now checked before the
  executor runs, and tab-completion respects it too.
- **`java-compilation: false` did not stop `.jar` files from being loaded.**
  A jar in `items/` was loaded and its classes instantiated — the same
  arbitrary-code-execution path the flag is supposed to close. Jar loading
  is now behind the same switch, so the mitigation promised in `SECURITY.md`
  actually works.
- **An item's `permission:` was only checked on click.** A player without
  the right still got the item's passive effects, attributes, armour-set
  bonus and every trigger (`on_kill`, `on_pickup`, …). The permission now
  gates all of those paths as well.
- **The `break:` action bypassed region protection.** It called
  `block.breakNaturally()` directly, which never fires `BlockBreakEvent` —
  WorldGuard, GriefPrevention and Lands never saw the block being broken.
  The action now goes through a real `BlockBreakEvent` and respects its
  cancellation.
- **Raw exception messages were sent to players** (a failed custom command
  could leak SQL text or a file path). Players now get a generic message;
  the details stay in the log.
- **MySQL: the server certificate was never verified.** `use-ssl: true` only
  encrypted the connection — `sslMode=REQUIRED` does not check the
  certificate, so a MITM stayed possible. There is now a
  `database.mysql.verify-server-certificate` option (uses
  `sslMode=VERIFY_IDENTITY`, which also checks the hostname), and the
  plugin warns in the log when the connection is unencrypted or
  unverified.

### 🧯 Concurrency (PlaceholderAPI runs on its own thread)
- `EffectManager.playerAppliedEffects` and `ItemRegistry.registeredItems`
  were plain `HashMap`/`LinkedHashMap` written by the main thread and read
  by the PlaceholderAPI thread — a data race that can corrupt the hash
  table and hang the request thread. Both are `ConcurrentHashMap` now.

### 🐛 Correctness

- **Effects and attributes were not re-applied after `/ci reload`.** The
  equipment hash is built from item IDs, and the IDs did not change after a
  reload — so editing an item's `effects:` or `attributes:` had no effect
  until the player took the item off and put it back on. The cache is now
  invalidated on reload.
- **Items with `duration:` never expired.** The acquisition time was written
  into a *copy* of the item stack (`Inventory#getContents()` returns
  copies), so the timestamp was lost every second and the item was never
  considered expired. The stack is now written back.
- **The SQLite connection proxy closed the shared connection.**
  `try (Connection c = db.getConnection())` closed the one shared
  connection for the whole plugin; every later query failed with
  "SQLite is not connected". `close()` is a no-op for SQLite now (MySQL
  still returns a pooled connection that has to be closed).
- The Java API item cooldown was shared by all items of a player; it is
  per item now, like the YAML path.
- `toLowerCase()` without `Locale.ROOT` in the placeholder expansion broke
  every ID in a Turkish locale.

### ⚡ Performance

- **The equipment check re-read the PDC of all six slots on every tick**
  whenever inventory events kept firing (auto-clickers, an open chest):
  `markForRecalculation` reset the 150 ms gate. The gate is no longer reset
  — the hash detects changes on its own, so a change is still noticed
  within 150 ms at a third of the cost.
- **The equipment hash was rebuilt every tick** for every player (20
  `StringBuilder`s per second each) although the slots only change every
  150 ms. It is now built together with the slot snapshot.
- **`ApiEventListener.onPlayerMove` was the most expensive line in the
  plugin**: 8 metadata clones per move event (~160/s per player) even when
  no Java item existed. It now exits immediately when the Java API is
  empty and reads the metadata once instead of twice.
- `CustomItem.getParsedEffects()` copied an already immutable map on every
  call (up to 12 copies per equipment recalculation).
- The placeholder expansion no longer copies the whole placeholder set or
  the whole effect map just to call `.size()`.

### 📖 Documentation

- `SECURITY.md` and `wiki/15-permissions.md` now describe the real
  semantics of `permission:` (it gates clicking, wearing and triggers) and
  the `java-compilation` switch (it covers `.jar` files too).

### ⚠️ Update notes

- If you distribute items to players, add `permission:` to them — without
  it every holder gets the effects and triggers.
- If you have a module whose command should work without permissions,
  pass `null` as the permission explicitly.

---

## v1.334.0

### 📖 Wiki

A complete A-to-Z guide for beginners now lives in the `wiki/` folder of the
repository — 19 pages, ~8300 lines, everything in Russian.

- **Start here:** `wiki/README.md` (index), `wiki/01-Установка.md`,
  `wiki/02-Как-это-работает.md`
- **What you can build:** YAML items, Java items, all 54 actions, all 12
  triggers, all five recipe types, custom commands, placeholders, modules,
  the central message system, the database API and the events for
  third-party plugins
- **Reference:** the full `config.yml`, every permission node, a copy-paste
  example for every feature, a troubleshooting guide and a complete Java API
  reference
- All cross-links and table-of-contents anchors were verified; the pages are
  valid UTF-8 without BOM.

### 🐛 Fixed

- **`armor-set`, `has-set-bonus` and `attributes` were ignored** for every
  YAML item that declared them at the top level — which is how all the
  samples and the README do it. The loader only looked inside the `item:`
  section, so **no armor set and no attribute bonus from a YAML item ever
  worked**. Both places are now supported, the top level takes priority.
- `samples/EXAMPLE-messages.java` did not compile (it referenced about fifty
  fields that do not exist in `MessagesConfig`), so copying it to
  `items/messages.java` never applied the messages.

### ⚠️ Update notes

- If you use `armor-set` or `attributes` in a YAML item **inside** the `item:`
  section, nothing changes. If you declared them at the top level, they
  start working after this update — check that the bonus is what you want.

---

## v1.333.0

### 💬 One central message system

Every text the player sees now lives in one place and can be overridden by
the administrator.

- **All hard-coded player-facing strings were moved to the message system**:
  item expiry and world bans, the permission and "uses depleted" texts, the
  `/ci` output (help, validation, debug, backup), the custom-command texts
  (`Command not found`, `Player not found`, `Specify a player`), the
  permission message of `CustomCommand` and the update-checker notification.
- **`MessagesConfig` is now addressed by path** — `items.expired`,
  `items.world-blocked`, `commands.not-found`, `ci.reload.success`,
  `update.title` and so on. The old constants still work.
- **Modules can plug their own texts into the same system**:
  - `registerMessage("my-module.no-money", "...")` in a module
  - `getMessage("my-module.no-money", "...")` to read it
  - `MessageManager#registerMessage(path, value)` for third-party plugins
  - `CustomCommand#message(path, default)` for custom commands
- **Any path works**, including your own: `MessagesConfig.set("my.text", "...")`
  in `items/messages.java` and the module reads it back by the same path.
- **Priority:** override from `items/messages.java` → the module's own text →
  the plugin's built-in message → the fallback default.
- Overrides that were removed from `items/messages.java` no longer survive a
  reload.

### 🐛 Fixed

- **`samples/EXAMPLE-messages.java` did not compile**: it referenced about
  fifty fields that do not exist in `MessagesConfig` (`CI_HELP_GIVE`,
  `LIST_HEADER`, `API_HEADER`, `BERSERK_ENABLED`, …). Copying it to
  `items/messages.java` produced a compile error, so the messages were never
  applied. The sample now only uses existing fields and demonstrates the
  custom-path API. Verified by compiling the sample the same way the plugin
  does.

### ⚠️ Update notes

- Nothing changes for existing servers: `items/messages.java` keeps working
  as before.
- If you use a custom command that relied on the old default permission
  message, it is now resolved through the message system — the text is the
  same unless you override it.

---

## v1.332.0

### 📦 The plugin installs exactly one file

A fresh install now contains **only** `plugins/DC-CustomItems/items/messages.java`
— one file, one compiled class. Nothing else is created or copied.

- **The samples are no longer shipped inside the jar.** They moved to the
  `samples/` folder in the repository, so the jar contains only `config.yml`
  and `plugin.yml` from the plugin's own resources.
- **`DefaultContentExtractor` was removed** together with the
  `extract-default-modules` option — there is nothing left to extract.
  The key is still ignored when reading an old `config.yml`, so existing
  servers keep working.
- **How to use a sample now:** copy the file or folder you need from
  `samples/` into `items/`, remove the `EXAMPLE-` prefix from its name and
  run `/ci reload`.
  - `samples/EXAMPLE-runes.yml` → `items/runes.yml`
  - `samples/EXAMPLE-vault/` → `items/vault/`
- `samples/README.md` documents what every sample shows.
- Docs and `config.yml` were updated to describe the new layout.

### ⚠️ Update notes

- Nothing is removed from an existing server: `items/` keeps whatever you
  already have.
- The `extract-default-modules` option no longer exists. Remove it from
  your `config.yml` if you want — it is simply ignored.

---

## v1.331.0

### 🌍 English by default

The plugin now ships with **English** as the default language instead of Russian.

- **All player-facing messages** are English: `MessagesConfig`, the built-in
  message fallbacks (`items.no-permission`, `items.uses-depleted`,
  `plugin.update-*`), inline messages in the listeners and the equipment
  checker, `/ci` command output, the update-checker messages and the
  `CustomCommand` base-class messages.
- **`items/messages.java`**, generated on first start, is now English.
  Existing servers keep their own file, so nothing changes for them.
- **All log output** is English (compiler, recipe loader, item loader,
  extractor, command registrar, module manager).
- **`config.yml`** comments are English.
- **All samples in `items/`** (`EXAMPLE-*.yml`, `EXAMPLE-*.java`, module
  folders, `_template/`, `EXAMPLES/`) are English: item names, lore,
  messages and documentation.
- **All source comments and javadoc** are English.

### ⚠️ Update notes

- No configuration keys were renamed and no behaviour changed — only text.
- Servers that want Russian back can simply edit `items/messages.java` or
  set the `MessagesConfig.*` fields again.

---

## v1.330.0

### 💥 Fixed

- **Item attributes were not applied at all**: the modifiers were created via `NamespacedKey.fromString(...)` without a namespace — the key fell back to `minecraft:` (or was `null` for an upper-case ID), and Bukkit rejected it. Added `EnumCache.getAttribute()`: it understands the modern names (`attack_damage`), the legacy ones (`GENERIC_ATTACK_DAMAGE`, `PLAYER_*`) and the registry keys.
- **Potion levels were added up instead of taking the maximum**: two items with `SPEED:1` gave Speed II. The maximum is taken now (`Math::max`).
- **`cooldown:MS` in triggers blocked nothing**: the value was written but never checked. The pseudo-action now really postpones the next trigger activation.
- **The click cooldown was shared per player**: the cooldown of one item blocked another. The key is now player + item.
- **NPE on an empty action**: `ActionParser.execute(player, " ")` threw a NullPointerException outside the try block.
- **`remove:`/`ItemAPI.removeItem` removed nothing**: `Inventory#getContents()` returns copies of the stacks, the changes never reached the inventory.
- **The use counter key in the lore did not match the one used for writing**: it read `dcustomitems:item_uses`, while the key written was in the plugin namespace (`dc-customitems:item_uses`). Because of that `%uses%` always showed the maximum instead of the remaining uses.
- **`onRegister()` of the commands and placeholders was not called on the server start** — only on `/ci reload`. The Java placeholders appeared only after the first reload.
- **A consumable with `max-uses` did not apply the effect on the last use** — the item disappeared before the effect was applied.
- **The slot was not freed after the consumption**: `setAmount(0)` left an "empty" stack in the inventory.
- **`type` and `activation-slot` were compared case-sensitively**: `type: consumable` and `activation-slot: hand` did not work. The values are normalized to upper case.
- **The Java API items were not checked for `placeable` and for the world ban** when placed and picked up.
- **The module and Java API commands were not registered**: the reflection looked for the `commandMap` field in the `Bukkit` class (in Paper it is in `CraftServer`). Added `CommandMapAccessor` with three lookup strategies and proper command removal on reload.
- **The commands stayed in the `CommandMap` after a reload**: when `knownCommands` was inaccessible an empty copy of the map was returned — the commands were "removed" only formally.
- **NPE when loading the YAML items**: the Java API registry was created after the YAML items, and a recipe could reference a Java item (`getApiItemRegistry()` returned `null`).
- **A `.java` file with its own `package`** did not compile: the imports were inserted at the top of the file, so the `package` declaration stayed second. The imports are now inserted after `package`, and the own declaration is removed (otherwise there would be two `package` declarations).
- **A file with any import from `api` did not get the other imports** → did not compile. The imports are checked by name now.
- **The generated class name could be invalid** (`1abc.java` → `1abcItem`, `my.item.java` → `my.itemItem`) — such a source file did not build.
- **A head texture from base64 was not applied** (an offset error while parsing the JSON) — fixed.
- **`teleportToNearest` crashed for the players in other worlds** (`Location#distance` throws an exception).
- **Pushing away the nearby players produced a NaN velocity** with a zero vector.
- **The Java items glowed because of `SHARPNESS`** (+1 damage) — the harmless `UNBREAKING` is used now.
- **`UpdateChecker` failed on the versions like `1.2.3-beta`** and left the HTTP connection open on error.
- **The messages from `messages.java` were written in the system encoding** (on Windows — mojibake). It is always UTF-8 now.
- **The YAML modules with an upper-case folder name could not be found** — the ID is normalized.

### 🧯 Memory and resource leaks

- `ApiEventListener` did not remove the player cooldowns on quit — the map grew without bound.
- `EffectManager` did not clean up the player state on quit (`stopPeriodicEffects` was a stub) — the effects stayed on the player for up to 60 s, the entries stayed in memory.
- `ItemRegistry` did not close the `URLClassLoader` of the loaded jars — open file handles were left behind.
- The Java API placeholders were not unregistered on reload — the old classes (and the classloader) stayed in the map.
- The static `Main.instance` reference was not cleared on shutdown — the old classloader was not unloaded.
- `JavaItemCompiler` did not close the `StandardJavaFileManager` when an exception occurred during the compilation.
- The `CooldownStore` counters were saved as YAML — the IDs with a dot broke the write/read. The new format is a list of entries (the old one is still read for compatibility).

### ⚡ Optimization

- `EquippedItemsChecker`: the PDC of each slot is read once per 150 ms (it was every tick — 20 times/sec per player), the two passes over the online players are merged, the particle trails and the periodic hooks are skipped when there are no such items.
- `ApiEventListener.onMove` no longer fires on a camera rotation (the event fired ~20 times/sec per player).
- `EffectManager`: the parsing of the set bonuses is cached (it ran on every equipment recalculation).
- `JavaItemCompiler`: the name replacement templates are compiled once, not per file × alias.
- `EnumCache` was moved to a `ConcurrentHashMap` (thread-safe) and caches the negative answers.
- `DCIPlaceholderExpansion`: the list of item IDs is cached instead of copied for every placeholder request.
- `ItemRegistry.getAll*()` return snapshots — the external code iterating over them can no longer get a `ConcurrentModificationException` during a reload.
- `CustomItemHandler.getAllCustomItems()` no longer copies the map on every call; the map is replaced atomically (without a "half-empty" window during a reload) and is safe to iterate from the PlaceholderAPI thread.
- `reloadAsync()` uses a dedicated thread instead of blocking a thread of the shared ForkJoin pool.

### 🔒 Security

- The table and column names in `DatabaseManager` are validated as SQL identifiers — arbitrary strings are no longer substituted into SQL.
- The access to the shared SQLite connection is serialized (otherwise "database is locked" and a risk of data corruption with parallel queries).
- `getConnection()` for SQLite returns a thread-safe wrapper that passes `SQLException` through as is.

### 🆕 New

- **`file-logging: false`** (in `config.yml`) — the plugin logs to `plugins/DC-CustomItems/logs/latest.log` instead of the console. Previously `PluginLogManager` was implemented but not wired up.
- `/ci validate` and `/ci debug` now also see the Java API items.

### ⚠️ Update notes

- `config.yml` is not overwritten automatically: the `file-logging` key will be added manually on the next update (off by default, the behaviour does not change).
- The potion levels of several items now take the maximum instead of the sum — if the balance relied on the sum, set the required level explicitly.

---

## v1.329.0

### 🔧 Fixed

- **The `EXAMPLE-*` samples no longer flood `items/` by default**: `extract-default-modules` is now `false` out of the box (it was `true`). On a clean server the `items/` folder stays empty — only the working `items/messages.java` is there. The auto-copying is enabled only explicitly (`extract-default-modules: true`), and only the missing files are copied.
- **Enabling a sample — by copying it without the prefix**: `EXAMPLE-runes.yml` → `runes.yml`, `EXAMPLE-vault/` → `vault/`, then `/ci reload` (the comments in `config.yml` and the javadoc were updated).
- **`config.yml`: the keys `java-compilation`, `backup`, `extract-default-modules` are no longer counted as items** — they were added to the skip lists `ItemLoader.CONFIG_SKIP_KEYS` and `ConfigMigrator` (this removes the false warnings and the false migration prompt).

### ⚠️ Update notes

- On an existing server `config.yml` is not overwritten automatically: delete `items/EXAMPLE-*` manually and set `extract-default-modules: false` if you do not want the auto-copying.

---

## v1.328.0

### 💥 Removed (breaking)

- **`buy-price` / `sell-price` removed**: the fields were cut out of `CustomItem`, `ItemLoader` ignores them with a warning. The shop has been dead since v1.326 — the economy works only through Vault/shop modules.
- **`trigger-actions:` removed**: only the canonical `triggers:` remains (the section since v1.324). The old key is ignored with a SEVERE log. The samples `EXAMPLE-tools.yml`, `EXAMPLE-advanced.yml` were converted to `triggers:` by a script.

### ✨ New (from the last sprint, released here)

- **`/ci validate`** — the recipe validation (`ItemValidator`): shaped/shapeless/smithing/stonecutting, unknown ingredients.
- **`/ci debug <id>`** — the item dump (type/slot/uses/cd/recipes/PDC).
- **`/ci backup` + auto-backup** before `/ci reload` (`backup.on-reload/keep`, `BackupManager`).
- **The `smithing` / `stonecutting` recipes** — YAML + Java API (`RecipeDef.smithing()/stonecutting()`), `schema/items.schema.json` for the IDE.
- **`CooldownStore`** — persistent cooldowns (`cooldowns.yml`).
- **Security**: the compiler limits (300 files / 512KB), the warnings on `System.exit/Runtime.exec/ProcessBuilder/setAccessible`.
- **DB**: `shutdownAsync()` with `awaitTermination(5s)` — no more races on disable.
- **`CustomItem.parsedEffects`** — `volatile` + defensive copy.
- **Docs**: `docs.md` became a redirect to `docs/`.

---

## v1.327.0

### 📦 Content — only the samples by default

- **Everything in `items/` was moved into the `EXAMPLE-*` samples**: the items
  (`EXAMPLE-vampire-blade.yml`, `EXAMPLE-runes.yml`, ...), the commands
  (`EXAMPLE-give-command.java`, ...) and the modules (`EXAMPLE-vault/`,
  `EXAMPLE-shop/`, `EXAMPLE-deluxemenux/`, `EXAMPLE-customcraft/`).
- **By default only the `EXAMPLE-*` files are copied on the first start**
  (the compiler and the loaders ignore them), plus a working
  `items/messages.java` is created. No item/command/module is activated
  automatically — the server starts "clean".
- **How to enable a sample**: remove the `EXAMPLE-` prefix from the file/folder name
  (`EXAMPLE-vampire-blade.yml` → `vampire-blade.yml`, `EXAMPLE-vault/` →
  `vault/`) and run `/ci reload`.
- `extract-default-modules` in `config.yml` still controls
  the auto-copying of the samples from the jar into `items/`.
- The documentation was updated: README, docs/, the comments in the configs and the
  headers of the samples describe the new rule.

---

## v1.326.0

### ✨ Modules

- **The `vault/` module was simplified to a pure economy**:
  - **The custom item shop was removed**: the commands `/buy`, `/sell`,
    `/sellhand`, `/worth` and all the `shop:` settings were removed from `config.yml`. The
    buying/selling of the items now lives in separate plugins that work with the
    currency via Vault or the public module API (`getBalance`, `withdraw`, `deposit`, `format`).
  - **The richest players top was removed**: the `/baltop` command, the `baltop-size` /
    `baltop-refresh-seconds` settings and the `%vault_top_name_N%` / `%vault_top_value_N%` placeholders were removed.
  - Remaining: `/balance` (the aliases `/bal`, `/money`), `/pay`,
    `/eco give|take|set|reset` (admin, the permission `dci.eco`), the starting balance,
    its own DB (`economy.storage: own`), the placeholders `%vault_balance%` /
    `%vault_currency%` — the currency for the players as is.

---

## v1.325.0

### 🔒 Security

- **The `java-compilation: true/false` flag in config.yml** — the runtime compilation
  of the `.java` files from `items/` can now be disabled (this is an arbitrary code
  execution; with an untrusted access to the plugin files it should be off). With `false`
  all the `.java` files in `items/` are ignored, the YAML items work as usual.
- **`SECURITY.md`** — the vulnerability reporting policy.
- **CodeQL** (security-extended) — a separate workflow, weekly and on the PRs.
- **OWASP dependency-check** — the `-Psecurity` profile in Maven (the dependency audit,
  `failBuildOnCVSS=9`).

### 🧪 Tests and CI

- **JaCoCo** — the test coverage report, an artifact in GitHub Actions.
- **JDK 21** in CI and in `maven.compiler` (Paper 1.21.x requires Java 21 at runtime).
- Reliable version stamping in CI: `mvn versions:set` instead of the fragile `sed`.
- `build.sh` rewritten: the version is read from `VERSION` (it was hardcoded
  to `1.6.5` and the path to the jar was wrong).

### 🛠 Fixes and cleanup

- Removed the junk line at the end of `.gitignore` (an artifact of an earlier operation).
- Removed `items/test.yml` from the default resources — the test items are no longer
  copied to the users' servers.
- Disabled the Maven resource filtering (`filtering: false`) — the substitution
  of `${...}` could silently break the YAML items with the placeholders.
- Compilation against **paper-api 1.21.8** (instead of spigot-api) — it matches the
  stated support for Paper/Folia/Purpur; the Minecraft version is unified across
  the documentation (1.21.8+).

---

## v1.324.0

### 🏗️ Modularity — for real now

- **The default modules/items are copied from the jar into `items/` on the first start**
  (`DefaultContentExtractor`). Previously the `items/` folder was created empty and the
  plugin was "bare" — now the modules, commands and items appear as regular files.
- All the functionality is files: delete `items/give-command.java` — `/give` is gone;
  delete `items/vault/` — `/buy /sell` are gone; delete any `.yml` — the item is gone.
- The core stays a "compiler": the file notation + the basic infrastructure (the
  registry, the events, the DB). With an empty `items/` only `/ci reload` works.
- The reference folders `EXAMPLES/`, `_template/` and the `EXAMPLE-*` files are not
  copied to the server and not compiled.
- The extraction rules: `vault/` is copied only when Vault is installed
  (otherwise its Java does not compile). Disabling: `extract-default-modules: false`.

### ⚡ One action parser

- One `ActionParser` instead of the three duplicated implementations: the clicks
  (YAML) and the triggers are executed the same way.
- Hyphens and underscores are equivalent: `damage-mobs` = `damage_mobs`.
- Added the missing actions: `effect_nearby*`, `knockback/launch/stun_mobs/players`,
  `break`; `title` understands all the formats; `teleport` — the `~` coordinates and the yaw;
  `give` can give the custom items by ID; `command` — from the console.
- Fixed the broken `set_xp` and the trigger cooldown (the keys did not match —
  `cooldown:X` now really works).

### 🔔 Events for third-party plugins (YAML and Java items)

| Event | When |
|---|---|
| `CustomItemEquipEvent` | Item equipped/unequipped (before the FX/hook) — cancellable |
| `CustomItemUseEvent` | LMB/RMB with an item (before the actions) — cancellable |
| `CustomItemCraftEvent` | Crafting in a normal crafting table and in the `/craft` GUI — cancellable, `setResult()` |
| `CustomItemDamageDealtEvent` / `DamageTakenEvent` | Damage with an item / to a player with an item — the damage is changed |
| `CustomItemKillEvent` / `DeathEvent` | Killing a player / death with an item — cancellable |
| `CustomItemPeriodicEvent` | Before every `onPeriodic` of a Java item — cancellable |

### 🛠 Crafting recipes

- The recipes directly in the YAML item section: `shaped`, `shapeless`, `furnace`
  (example — `items/EXAMPLE-crafting.yml`). Re-registration on `/ci reload`.
- **The Java items** declare the recipes via `getRecipes()` + the `RecipeDef` class.
- **GUI crafting**: the `items/customcraft/` module — the `/craft` command, a 3×3 window,
  with a preview of the result. The ingredient identity via PDC/NBT: a custom
  item matches by ID (it can match "itself"), the foreign items — by material,
  their NBT is not damaged.
- `/give` now gives the Java items too (the resolution by ID, the list, the tab-complete,
  `all`/`<player>`/`<amount>`).

### ☕ Java API — "you can write anything"

- The YAML-level mechanics of the Java items: `getMaxUses()`, `getDuration()`,
  `isAllowedInWorld()`, the custom messages `get*Message()`.
- `getPermission()` is now checked on the clicks (like in YAML).
- The `onEquip` / `onUnequip` / `onPeriodic` hooks are really called by the global
  equipment checker (they were dead before).
- **Compiler**: the classpath includes the server jar (Paper / Folia / Purpur /
  Spigot), all the `plugins/*.jar` and the `plugins/DC-CustomItems/libs/` folder — any
  third-party library is connected by dropping the jar into `libs/` and `/ci reload`.
- The dynamic module commands are properly removed on the disable/reload
  (before they stayed in the CommandMap).
- `items/messages.java` — a working message config: `load()` is called on
  the start and on `/ci reload`, the edits without a rebuild of the JAR.

### 🐛 Fixes

- `SHADOW-dashblade.java` did not compile on 1.21.8 (the outdated particle /
  rayTrace / sendActionBar) — fixed.
- The outdated API in the samples (`EXAMPLE-royal-helmet`) was brought up to date.
- Removed the dead `TriggerConfig`, the unused `loadJavaModule` and the duplicates of the FX code.
- `EnumCache`/`ActionParser` are initialized lazily — more reliable on the start.

### 🧪 Tests and documentation

- 16 JUnit tests: the action normalization of `ActionParser`, `EnumCache`.
- The reference samples: `items/EXAMPLES/DragonboneArmor.java` (armour: the recipes,
  the equipping, the periodic) and `items/EXAMPLES/Frostblade.java` (weapon: the clicks,
  the cooldown, the combat).
- The documentation was brought up to date (the events, the recipes, the GUI crafting, the Java API,
  messages, `libs/`).

---

## Earlier versions

- **v1.323.0** — the stabilization of the modules and the compiler.
- **v1.322.0** — duration, the trail particles, the world bans, the `on_sprint` trigger,
  the animated models, the CI releases from `VERSION`.
- **v1.321.1** — a full documentation update and a modular architecture.