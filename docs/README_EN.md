# Artifex — Complete Documentation

**Documentation version:** 1.325.0
**Minecraft:** Paper/Spigot 1.21.x
**Server Java:** Java 21+ is required for Paper 1.21.8+ and to build the project.

Artifex lets you create custom items, abilities, commands, placeholders, modules, and GUI menus. You can start with **no Java knowledge**: ordinary items are configured in YAML. Java is only needed for advanced mechanics, custom commands, and modules.

---

## 1. Where to start

If YAML and Java are new to you, read these guides in order:

1. [Installation and first steps](GETTING_STARTED_EN.md)
2. [YAML basics](YAML_ITEMS_EN.md)
3. [Plugin commands](COMMANDS_EN.md)
4. [Messages and file layout](MESSAGES_DATABASE_EN.md)
5. [GUI and modules](MODULES_GUI_EN.md)
6. [Data storage: SQLite, MySQL, and YAML](DATABASE_EN.md)
7. [Java from zero](JAVA_API_EN.md)
8. [Performance and troubleshooting](TROUBLESHOOTING_EN.md)
9. [Resource packs and models](RESOURCE_PACK_EN.md)

Working examples live in `samples/` in the repository. Nothing is copied on install — a fresh install contains exactly one file: `items/messages.java`. To enable an example, copy the file or folder you need from `samples/` into `items/` and remove the `EXAMPLE-` prefix from its name, then run `/ci reload`.

---

## 2. What the plugin can do

### Without programming

YAML can create:

- swords, axes, pickaxes, and bows;
- armor with equip and unequip abilities;
- potions, food, scrolls, and totems;
- potion effects;
- healing and damage;
- particles and sounds;
- teleportation;
- giving and removing ordinary Minecraft items;
- player and console commands;
- cooldowns;
- limited uses;
- per-item permissions;
- custom names, lore, and models.

### With Java

Java files can create:

- items using Bukkit/Paper events;
- custom commands and aliases;
- placeholders;
- periodic effects;
- RPG classes, quests, bosses, and minigames;
- modules with their own `config.yml`, `items.yml`, and GUI;
- persistent progress in SQLite or MySQL;

Important: Artifex compiles user Java files while the server is running. Only use code you trust, and back up your server before editing files.

---

## 3. Installation

### Requirements

- Paper or Spigot 1.21.x;
- Java 21 for Paper 1.21.8+;
- server administrator access;
- a backup of the world and `plugins/` directory.

### Download a release

```bash
cd ~/test/plugins
curl -fL -o dcustomitems.jar "https://github.com/animesao/dcustomitems/releases/download/v1.320.282/Artifex-1.320.282.jar"
```

Replace `~/test/plugins` with your server's `plugins/` directory when needed.

### First start

1. Put `dcustomitems.jar` in `plugins/`.
2. Start the server.
3. Wait for `CustomItems enabled!`.
4. The plugin creates `plugins/Artifex/`.
5. Check the version with `/version Artifex` or in the console.

The first run creates approximately:

```text
plugins/Artifex/
├── config.yml       # plugin settings
├── data.db          # SQLite database (when sqlite is selected)
├── storage/         # small YAML storage files
├── items/           # your YAML and Java files
├── cache/           # compiler cache
└── compiled/        # compiled classes
```

The plugin automatically creates `items/messages.java` when it does not exist. Keep a backup before deleting or replacing it.

---

## 4. Your first item without Java

Create:

```text
plugins/Artifex/items/hello-sword.yml
```

Content:

```yaml
hello_sword:
  type: TOOL
  activation-slot: HAND
  click-cooldown: 3000

  item:
    type: DIAMOND_SWORD
    title: '&aWelcome sword'
    glowing: true
    unbreakable: true

  lore:
    - ''
    - '&7This is your first custom item.'
    - '&eRight-click — receive speed.'

  trigger-actions:
    - 'on_click_right:message:&aThe sword works!'
    - 'on_click_right:effect:SPEED:10:2'
    - 'on_click_right:particle:HEART:20'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
```

After saving:

```text
/ci reload
/ci list
/ci give hello_sword
```

Hold the sword in your main hand and right-click.

### What each line means

- `hello_sword` — item ID used by `/ci give hello_sword`.
- `type` — logical item type: `TOOL`, `ARMOR`, `RUNE`, or `CONSUMABLE`.
- `activation-slot` — where the item is active.
- `click-cooldown` — delay between activations in milliseconds.
- `item.type` — the real Minecraft material.
- `item.title` — display name.
- `glowing` — visual enchantment glow.
- `unbreakable` — item cannot lose durability.
- `lore` — description lines.
- `trigger-actions` — events and actions.

YAML is indentation-sensitive. Use spaces, not tabs.

---

## 5. YAML for complete beginners

YAML uses:

```yaml
key: value
```

Lists use `-`:

```yaml
lore:
  - '&7First line'
  - '&eSecond line'
```

Nested sections use additional indentation:

```yaml
item:
  type: DIAMOND_SWORD
  title: '&bSword'
```

Correct:

```yaml
item:
  type: DIAMOND_SWORD
```

Incorrect:

```yaml
item:
 type: DIAMOND_SWORD
```

Quote text containing `:`:

```yaml
- 'on_click_right:title:&6Power!::&7Activated:10:40:10'
```

Minecraft color codes:

```text
&0 black       &1 dark blue   &2 dark green  &3 dark aqua
&4 dark red    &5 purple      &6 gold        &7 gray
&8 dark gray   &9 blue        &a green       &b aqua
&c red         &d pink        &e yellow      &f white
&l bold        &o italic      &n underline   &m strikethrough
&r reset formatting
```

---

## 6. Item events

The compact format is:

```yaml
trigger-actions:
  - 'on_click_right:message:&aRight-click'
```

A structured format is also supported:

```yaml
triggers:
  on_click_right:
    - 'message:&aRight-click'
    - 'effect:SPEED:5:1'
```

The current `trigger-actions` handler reliably connects these events:

| Event | When it runs |
|---|---|
| `on_click_left` | Left-click with the item |
| `on_click_right` | Right-click with the item |
| `on_equip` | Item is equipped |
| `on_unequip` | Item is removed |
| `on_damage_dealt` | Player deals damage |
| `on_damage_taken` | Player receives damage |
| `on_kill` | Player kills another player (current listener) |
| `on_death` | Player dies |
| `on_jump` | Player jumps |
| `on_drop` | A custom item is dropped |
| `on_pickup` | A custom item is picked up |

Names such as `on_block_break`, `on_block_place`, `on_sneak`, `on_sprint`, and `on_swim` appear in models/older examples, but should not be treated as universally active in the current `TriggerListener`. Verify the implementation before using them.

The Java API has separate methods such as `onMove`, `onBlockBreak`, and `onSwapHand`; that is a different path from YAML `trigger-actions`. `on_move` and `onMove` are frequent events and can create server load.

### Bukkit events for third-party plugins

The plugin publishes its own Bukkit events so other plugins can react to custom items:

| Event | When | Cancelable |
|---|---|---|
| `CustomItemEquipEvent` | Item equipped or removed (before FX/messages or the `onEquip`/`onUnequip` hook) | Yes |
| `CustomItemUseEvent` | Left/right-click with the item (before click actions or the `onRightClick`/`onLeftClick` hook) | Yes |
| `CustomItemCraftEvent` | Crafting — in the vanilla table AND the `/craft` GUI (before ingredients are consumed; the result can be replaced via `setResult`) | Yes |
| `CustomItemDamageDealtEvent` | Damage dealt with the item (before triggers/`onDamageDealt`) | Yes |
| `CustomItemDamageTakenEvent` | Damage taken while holding/equipped (before triggers/`onDamageTaken`) | Yes |
| `CustomItemKillEvent` | Player killed another player with the item (before `on_kill`/`onKill`) | Yes |
| `CustomItemDeathEvent` | Player died with the item (before `on_death`/`onDeath`) | Yes |
| `CustomItemPeriodicEvent` | Periodic effect of a Java item (before each `onPeriodic`) | Yes |

The events fire for both YAML items and Java API items (`AbstractCustomItem`):

```java
import me.dcplugin.artifex.events.CustomItemEquipEvent;
import me.dcplugin.artifex.events.CustomItemUseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    @EventHandler
    public void onUse(CustomItemUseEvent event) {
        if ("vampire-blade".equals(event.getItemId())) {
            event.setCancelled(true); // forbid usage
        }
    }

    @EventHandler
    public void onEquip(CustomItemEquipEvent event) {
        if (event.getJavaItem() != null) {          // Java API item
            String id = event.getJavaItem().getId();
        } else if (event.getCustomItem() != null) { // YAML item
            String id = event.getCustomItem().getId();
        }
    }
}
```

Register as usual: `getServer().getPluginManager().registerEvents(new MyListener(), plugin)`. Cancelling an event suppresses only the item's default reaction (triggers/hook); the underlying Bukkit effect (damage, death) is cancelled via `getBukkitEvent()`/`setDamage()` on the damage events.

---

## 7. YAML actions

For ordinary `trigger-actions`, use:

```text
event:action:parameter1:parameter2
```

Both `PlayerListener` and `TriggerListener` execute ALL actions through a single `ActionParser`. Available actions: `message`, `title`, `actionbar`, `announce`/`broadcast`, `effect`, `heal`, `damage` (+ `damage_nearby`/`damage_mobs`/`damage_players`), `heal_nearby`, `effect_nearby` (+ `_mobs`/`_players`), `teleport`, `teleport_relative`, `give` (material or custom item id), `remove`, `exp`, `lightning`, `lightning_forward`, `particle(s)`, `sound`, `fireworks`, `break`, `sethealth`, `setfood`, `set_xp`, `vanish`, `glow`, `speed`, `flight`, `knockback`/`launch`/`stun` (+ `_mobs`/`_players`), `command` (from console), `console_command`, plus extended actions: `particles_custom`, `sound_sequence`, `title_sequence`, `command_sequence`, `teleport_sequence`, `effect_sequence`, `damage_custom`, `heal_custom`.

Hyphens and underscores are equivalent (`damage-mobs` == `damage_mobs`). Always test a mechanic on a private server.

### Messages and UI

```yaml
- 'on_click_right:message:&aHello, %player%!'
- 'on_click_right:actionbar:&eAbility ready'
- 'on_click_right:broadcast:&6%player% activated an artifact'
- 'on_click_right:announce:&6%player% activated an artifact'
- 'on_click_right:title:&6Power!|&7For 10 seconds|10|40|10'
```

Some older examples use `::` in title values. The current Java listener is safest with `|` as the title separator.

### Effects

```yaml
# effect:TYPE:SECONDS:LEVEL
- 'on_click_right:effect:SPEED:10:2'
- 'on_click_right:effect:STRENGTH:5:1'
```

Use normal Minecraft levels: `1` is level I, `2` is level II.

Supported names include `SPEED`, `SLOWNESS`, `HASTE`, `MINING_FATIGUE`, `STRENGTH`, `JUMP_BOOST`, `REGENERATION`, `RESISTANCE`, `FIRE_RESISTANCE`, `WATER_BREATHING`, `INVISIBILITY`, `NIGHT_VISION`, `POISON`, `WITHER`, `HEALTH_BOOST`, `ABSORPTION`, `GLOWING`, `LUCK`, `UNLUCK`, `DOLPHINS_GRACE`, `CONDUIT_POWER`, `SLOW_FALLING`, `BAD_OMEN`, `HERO_OF_THE_VILLAGE`, and effects available in newer Paper versions.

### Healing and damage

```yaml
- 'on_click_right:heal:10'
- 'on_damage_dealt:damage:5'
- 'on_click_right:damage_nearby:8:4'
- 'on_click_right:damage_mobs:8:5'
- 'on_click_right:damage_players:4:3'
- 'on_click_right:heal_nearby:5:5'
```

For YAML actions, verify the amount on a test server because health units differ between older and newer handlers.

### Items and experience

```yaml
- 'on_kill:give:DIAMOND:1'
- 'on_click_right:remove:DIAMOND:1'
- 'on_click_right:exp:100:1'
```

### Teleportation

```yaml
- 'on_click_right:teleport:100:64:200'
- 'on_click_right:teleport_relative:~:2:~'
```

### Particles and sounds

```yaml
- 'on_click_right:particle:FLAME:50'
- 'on_click_right:particles_custom:FLAME:100:0:1:0:0.5:0.5:0.5'
- 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
- 'on_click_right:fireworks:1'
```

Names must exist in your Paper version. `Unknown particle` or `Unknown sound` means the name is outdated or misspelled.

### Player state

```yaml
- 'on_click_right:sethealth:20'
- 'on_click_right:setfood:20'
- 'on_click_right:vanish:10'
- 'on_click_right:glow:10'
- 'on_click_right:speed:10:2'
- 'on_click_right:flight:true'
- 'on_click_right:knockback:4'
- 'on_click_right:launch:1.5'
- 'on_click_right:stun:3:4'
```

### Commands and extended actions

The current YAML trigger path confirms the `command` action:

```yaml
# Executed by the current item action handler
- 'on_click_right:command:spawn'
```

All actions listed above, including the extended ones, work in YAML (both `trigger-actions` and `triggers:`) because both listeners call `ActionParser.execute()`.

Commands in user YAML can be dangerous. Do not let ordinary players edit these files.

See [README-ACTIONS.md](../samples/EXAMPLE-README-ACTIONS.md) for an additional action reference. It documents legacy and extended formats; if anything conflicts, follow the current handler and test the action on a private server.

---

## 8. YAML item fields

```yaml
my_item:
  type: RUNE
  activation-slot: HAND
  placeable: false
  permission: 'myplugin.item.use'
  click-cooldown: 3000
  max-uses: 5

  item:
    type: BLAZE_ROD
    amount: 1
    title: '&dStaff'
    glowing: true
    unbreakable: true
    item-model: 'myplugin:staff'
    custom-model-data: 1001
    item-flags:
      - HIDE_ATTRIBUTES
    enchantments:
      UNBREAKING: 3

  lore:
    - '&7Uses: %uses%'
    - '&7Cooldown: %cooldown% sec.'

  effects:
    - 'SPEED:1'

  trigger-actions:
    - 'on_click_right:lightning_forward:10'
```

`item-model` is intended for modern Minecraft versions. `custom-model-data` is retained for older resource packs. When both are present, `item-model` has priority.

`max-uses` is used by the limited-use handler; whether `%uses%` is rendered in lore depends on the specific handler and should be verified with a test item.

### Crafting recipes (shaped / shapeless / furnace)

Recipes live directly in the item YAML. An ingredient can be a material (`DIAMOND`) or another custom item by id. The result is the item itself, with all its mechanics. Run `/ci reload` after editing recipes.

```yaml
my-crafted-sword:
  type: TOOL
  item:
    type: DIAMOND_SWORD
    title: '&bCrafted Sword'

  recipes:
    # Shaped (space = empty cell)
    shaped:
      - pattern:
          - " A "
          - "ABA"
          - " A "
        keys:
          A: DIAMOND
          B: STICK
        amount: 1

    # Shapeless
    shapeless:
      - ingredients: [ DIAMOND, DIAMOND, my-crafted-sword ]

    # Furnace (200 ticks = 10 seconds)
    furnace:
      - ingredient: IRON_INGOT
        experience: 0.7
        cooking-time: 200
```

Each list (`shaped`, `shapeless`, `furnace`) accepts several recipes. See also `samples/EXAMPLE-crafting.yml`.

Recipes can also be crafted in a **GUI workbench**: the `items/EXAMPLE-customcraft/` sample module adds `/craft` and a 3×3 window with a result preview. To enable it, copy the folder into `items/` and remove the `EXAMPLE-` prefix (`EXAMPLE-customcraft/` → `customcraft/`). Ingredients match by PDC/NBT: custom items by their id (an item can even be its own ingredient), other plugins' items by material without touching their data. The crafted result is fully initialized (uses/duration). Delete the `customcraft/` folder to remove the command.

Java API items declare recipes the same way via `getRecipes()` (class `RecipeDef`) — they register into the vanilla crafting table and appear in `/craft` as well. See [JAVA_API_EN.md](JAVA_API_EN.md#-crafting-recipes-java).

---

## 9. Plugin commands

### YAML items

```text
/ci                         # help
/ci give <id>               # give yourself an item
/ci give <id> <player>      # give a player an item
/ci list                    # list YAML items
/ci reload                  # reload YAML, Java, and modules
/ci update                  # check for updates
```

The main command alias is `/customitems`.

### Java API items

```text
/api-item                   # help
/api-item give <id>          # give yourself a Java item
/api-item give <id> <player> # give a player a Java item
/api-item list               # list Java items
/api-item info <id>          # detailed information
```

### Permissions

```text
customitems.give
customitems.list
customitems.reload
customitems.update (checked by code but not declared in the current plugin.yml; grant it through your permissions system if needed)
customitems.admin (reserved as a general admin permission; /ci subcommands use their individual permissions)
```

If a command fails, check `/plugins`, operator status, and the console first.

---

## 10. Java for people who do not know Java

A Java file is a set of instructions for the server. Common concepts:

- `class` — a description of an object;
- `method` — an action or function;
- `String` — text;
- `int`/`double` — numbers;
- `boolean` — `true` or `false`;
- `@Override` — replacing behavior from a base class.

Minimal Java item:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class WelcomeItem extends AbstractCustomItem {
    @Override
    public String getId() {
        return "welcome_item";
    }

    @Override
    public String getDisplayName() {
        return "&aWelcome item";
    }

    @Override
    public Material getMaterial() {
        return Material.EMERALD;
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        ItemAPI.message(player, "&aYou right-clicked!");
        ItemAPI.particles(player, Particle.HEART, 20);
    }
}
```

Save it as `WelcomeItem.java` or use a lowercase/hyphenated source filename such as `welcome-item.java` for the built-in compiler. The public class name must follow the compiler's naming rules and be unique.

Then run:

```text
/ci reload
/api-item list
/api-item give welcome_item
```

If Java compilation fails, fix the first compiler error in the console, not the last line of the stack trace.

More details: [JAVA_API_EN.md](JAVA_API_EN.md).

---

## 11. Java commands

Extend `CustomCommand`:

```java
import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;

public class PingCommand extends CustomCommand {
    public PingCommand() {
        super("ping", "Ping check", "/ping", "example.ping", "p");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        msg(sender, "&aPong!");
        return true;
    }
}
```

After `/ci reload`, the console should contain:

```text
[API] Command: /ping
[API] Registered command: /ping
```

Do not use a command name already owned by Paper or another plugin. Do not register the same command in multiple Java files.

---

## 12. Placeholders

Minimal placeholder:

```java
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

public class ServerNamePlaceholder extends CustomPlaceholder {
    public ServerNamePlaceholder() {
        super("server_name");
    }

    @Override
    public String getValue(Player player) {
        return "My Server";
    }
}
```

Its value is requested as `%server_name%` by systems that call PlaceholderManager. Artifex is not automatically a complete PlaceholderAPI adapter for every third-party plugin, so verify compatibility where you intend to display the placeholder.

---

## 13. DeluxeMenuX: GUI without Java

The module lives at:

```text
plugins/Artifex/items/deluxemenux/   # after enabling EXAMPLE-deluxemenux/ → deluxemenux/
├── config.yml
├── deluxemenux.java
├── menu-command.java
├── kits-command.java
├── shop-command.java
└── menus/
    ├── main.yml
    ├── kits.yml
    └── shop.yml
```

Commands:

```text
/menu
/menu <id>
/menu list
/menu reload
/deluxemenu
/kits
/kit
/shop
```

Menus are created only when their files do not exist. Existing user YAML files are not overwritten.

Button example:

```yaml
items:
  13:
    material: DIAMOND
    name: '&bDiamond'
    lore:
      - '&7Click to receive'
    permission: 'shop.diamond'
    command: 'give %player% diamond 1'
    message: '&aDiamond granted!'
    sound: 'ENTITY_PLAYER_LEVELUP'
    close: false
```

Important:

- `command` runs as the **console**;
- `%player%` is replaced with the player's name;
- `permission` is checked when the button is clicked;
- `items.13` is the slot, from `0` to `size - 1`;
- `size` should be `9`, `18`, `27`, `36`, `45`, or `54`;
- a lore line such as `Price: 100 coins` does not charge anything; economy requires another plugin or a Java mechanic;
- `close-sound` is read from YAML, but the current click/close handler does not guarantee that it is played.

More details: [MODULES_GUI_EN.md](MODULES_GUI_EN.md).

---

## 14. Your own module inside the plugin

Every `items/` subdirectory can be a module:

```text
items/my-feature/
├── config.yml
├── items.yml
├── my-feature.java
├── feature-command.java
└── menus/
    └── main.yml
```

`config.yml`:

```yaml
name: 'My Feature'
version: '1.0'
enabled: true
commands:
  - myfeature
permissions:
  - myfeature.use
```

A Java module extends `Module` and implements `onEnable()`/`onDisable()`. Register listeners and start mechanics in `onEnable()`; cancel tasks and release resources in `onDisable()`.

After changing Java or YAML:

```text
/ci reload
```

When only the resource pack changes, the player needs `F3+T` or a resource-pack reload.

---

## 15. SQLite, MySQL, and YAML database storage

For the complete MySQL, promo-code, and asynchronous query guide, see [DATABASE_EN.md](DATABASE_EN.md).

SQLite is enabled by default. To use MySQL, set `database.type: mysql` in `config.yml`, fill in `database.mysql.*`, and restart the server. Use `YamlStorage` for small editable lists; use `DatabaseManager` async methods for high-traffic data and promo codes.

The plugin connects to:

```text
plugins/Artifex/data.db
```

Java API example:

```java
DatabaseManager db = Main.getInstance().getDatabaseManager();
db.createTable("player_stats", "uuid TEXT PRIMARY KEY, kills INTEGER DEFAULT 0");
db.execute("INSERT INTO player_stats(uuid, kills) VALUES(?, ?)", uuid, 1);
int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid);
```

Available operations include `connect`, `disconnect`, `reconnect`, `createTable`, `execute`, `insert`, `update`, `delete`, `queryInt`, `queryString`, `queryDouble`, `queryBoolean`, `queryOne`, `queryAll`, `increment`, and `add`.

Do not run heavy SQL every tick and never delete `data.db` without a backup.

---

## 16. Messages

**Every text the player sees lives in one place — `items/messages.java`.** It is the only file the plugin creates on install.

Default message fields live in `MessagesConfig.java` inside the JAR. On first start, the plugin creates `items/messages.java` as a working copy of the standard messages.

**`items/messages.java` is a working message config.** The plugin compiles it together with the other Java files and calls its static `load()` on startup and on every `/ci reload` — changes take effect without rebuilding the JAR.

The file is generated automatically on first startup (with the default values) and overrides `MessagesConfig` fields (PREFIX, NO_PERMISSION, RELOAD_* and so on). Delete it and it is recreated on the next start.

Example (roughly what the generated file looks like):

```java
MessagesConfig.PREFIX = "&8[&bMy server&8] &r";
MessagesConfig.ITEM_EXPIRED = "&cItem {item} has expired!";
MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cYou do not have permission.";
```

Only use fields that exist in the current `MessagesConfig`. Every `public static String` can be overridden inside `load()`. If a field was removed in a newer plugin version, the compiler reports it in the console and the file is simply not applied (built-in messages stay).

### Your own texts under any path

Besides the standard fields you can define **your own keys** — your module reads them by path and the administrator overrides them in the same file:

```java
MessagesConfig.set("my-module.no-money", "&cNot enough money!");
MessagesConfig.set("my-module.purchased", "&aBought {item} for {count} coins!");
```

### How a module plugs into the system

A module (a folder in `items/`) registers its texts and reads them by path:

```java
@Override
protected void onEnable() {
    registerMessage("my-module.no-money", "&cNot enough money!");
}

void onBuy(Player player) {
    player.sendMessage(getMessage("my-module.no-money", "&cNot enough money!"));
}
```

Priority: override from `items/messages.java` → the module's own text → the plugin's built-in message → the fallback passed by the caller.

The same is available to a plain command (`extends CustomCommand`):

```java
protected String message(String path, String defaultMessage) { ... }
```

---

## 17. Performance

To keep the server healthy:

- do not use `on_move` for heavy work;
- do not query SQLite or MySQL every tick; move database work to async methods;
- keep AoE radii reasonable;
- use `click-cooldown`;
- avoid hundreds of particles per player;
- do not create endless `onPeriodic` loops;
- cancel Bukkit tasks in `onDisable()`;
- do not run reload every second;
- test mechanics on a private server first;
- use `/spark profiler` and monitor TPS.

`/ci reload` is intended for development and configuration. On a large server, avoid using it during peak online time.

---

## 18. Backups and updates

Before updating:

```bash
cp -a plugins/Artifex plugins/Artifex.backup
cp server.properties server.properties.backup
```

Update the JAR:

```bash
dck stop test2
cd ~/test/plugins
curl -fL -o dcustomitems.jar "https://github.com/animesao/dcustomitems/releases/download/v1.320.282/Artifex-1.320.282.jar"
# clear cache/compiled only when the Java compiler system changed
rm -rf Artifex/cache Artifex/compiled
dck start test2
dck attach test2
```

Do not delete the entire `Artifex/` directory if you want to keep your items, menus, and database.

---

## 19. Troubleshooting

When reporting a problem, include:

- Paper version;
- Java version;
- Artifex version;
- the file you edited;
- the first complete console error;
- results of `/ci list`, `/api-item list`, and `/plugins`.

Common errors:

| Error | Cause |
|---|---|
| `Unknown particle` | Particle was removed or renamed in your version |
| `Unknown sound` | Invalid sound name |
| `Class ... is public, should be declared in ...` | Public Java class/file naming mismatch |
| `cannot find symbol` | Missing import, wrong Paper API, or typo |
| `Unknown or incomplete command` | Command failed to register due to compilation or name conflict |
| `duplicate class definition` | Old JAR/cache or a class loaded twice; update the JAR and clear cache/compiled |
| `items folder is empty` | `items/` holds only `EXAMPLE-*` samples, which are not loaded. Remove the `EXAMPLE-` prefix from a file name or create your own `.yml` |
| container `UnknownHostException` | Paper cannot resolve the container hostname; usually unrelated to the plugin |

Full solutions: [TROUBLESHOOTING_EN.md](TROUBLESHOOTING_EN.md).

---

## 20. Beginner checklist

```text
[ ] Paper and a compatible Java version are installed
[ ] The JAR is in plugins/
[ ] The server was restarted
[ ] items/my-item.yml was created
[ ] YAML uses spaces for indentation
[ ] /ci reload was executed
[ ] The console has no red errors
[ ] The item appears in /ci list
[ ] The item is given with /ci give <id>
[ ] The mechanic was tested in a safe world
[ ] A backup exists before publishing
```

If you do not know Java, stay with YAML. Once YAML feels comfortable, move to Java API. You do not need to learn all of Bukkit at once: add one mechanic, reload, test, and then expand.

---

## Links

- [GitHub repository](https://github.com/animesao/dcustomitems)
- [Releases](https://github.com/animesao/dcustomitems/releases)
- [Issues](https://github.com/animesao/dcustomitems/issues)
- [Русская документация](README_RU.md)
