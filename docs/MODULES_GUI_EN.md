# Modules and GUI — English Guide

## 1. What is a module?

A module is a separate feature inside Artifex. It can contain Java sources, `config.yml`, `items.yml`, commands, and menus.

Example layout:

```text
plugins/Artifex/items/my-feature/
├── config.yml
├── items.yml
├── my-feature.java
├── feature-command.java
└── menus/
    └── main.yml
```

A Java module extends `me.dcplugin.artifex.api.modules.Module` and has this constructor shape:

```java
public MyFeatureModule(Main plugin, String id, File folder) {
    super(plugin, id, folder);
}
```

Minimal lifecycle methods:

```java
@Override
protected void onEnable() {
    // enable the feature
}

@Override
protected void onDisable() {
    // cancel tasks and release resources
}
```

Always cancel repeating tasks and release listeners/resources in `onDisable()`.

## 2. Module configuration

```yaml
name: 'My module'
version: '1.0'
enabled: true
commands:
  - myfeature
permissions:
  - myfeature.use
```

The module loads `config.yml` and `items.yml` as YAML. `items.yml` may contain an `items` section, available through the base class `getItem()` and `getAllItemIds()` helpers.

After changing files, run:

```text
/ci reload
```

## 3. DeluxeMenuX

The module ships as the `EXAMPLE-deluxemenux/` sample in the jar/repository.
Enable it by copying the folder into `plugins/Artifex/items/` and
removing the `EXAMPLE-` prefix from its name (`EXAMPLE-deluxemenux/` →
`deluxemenux/`), then run `/ci reload`. After that it lives at:

```text
plugins/Artifex/items/deluxemenux/
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

On load, it scans `menus/` and creates default `main.yml`, `kits.yml`, and `shop.yml` only when they do not exist. Existing files are not overwritten.

Available commands:

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

`/kits`, `/kit`, and `/shop` open their corresponding menus. `/menu` without an argument opens `main`.

## 4. Menu format

Basic configuration:

```yaml
title: '&8Shop'
size: 27
open-sound: 'BLOCK_CHEST_OPEN'
close-sound: 'BLOCK_CHEST_CLOSE'
fill:
  material: BLACK_STAINED_GLASS_PANE
  name: ' '
items:
  13:
    material: DIAMOND
    name: '&bDiamond'
    lore:
      - '&7Click to receive'
    permission: 'deluxemenux.shop.diamond'
    command: 'give %player% diamond 1'
    message: '&aDiamond granted!'
    sound: 'ENTITY_PLAYER_LEVELUP'
    close: true
```

Rules:

- `size` is normally `9`, `18`, `27`, `36`, `45`, or `54`;
- slots start at `0`, so the last slot in a 27-slot menu is `26`;
- `material` must be a valid Bukkit Material;
- `permission` is checked on click;
- `command` runs as the console;
- `%player%` is replaced with the player's name;
- `message` is sent to the player;
- `close: true` closes the menu after clicking;
- a price written in lore does not charge currency by itself.

The current implementation reads `close-sound`, but does not guarantee playing it on every close. Real economy requires another plugin or a Java mechanic.

## 5. Add your own menu

1. Create `plugins/Artifex/items/deluxemenux/menus/quests.yml`.
2. Add `title`, `size`, and `items`.
3. Run `/ci reload`.
4. Open `/menu quests`.

If it does not open, check the filename, YAML indentation, inventory size, and console output.

## 6. Add a command button

```yaml
items:
  11:
    material: EMERALD
    name: '&aTeleport'
    permission: 'myserver.warp.spawn'
    command: 'warp spawn %player%'
    close: true
```

The command runs with console permissions. This is useful but powerful: do not allow ordinary players to edit menu YAML files.

## 7. Reload and update

After changing Java, YAML, or menu files:

```text
/ci reload
```

Updating the plugin JAR requires a full server restart. Back up `items/` before making changes.
