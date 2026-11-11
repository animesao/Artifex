# 📦 Module Template

A template for creating Artifex modules.

## 🚀 Quick start

### 1. Copy the template

```bash
cd plugins/Artifex/items/
cp -r _template/ my-module/
```

### 2. Rename the file

```bash
cd my-module/
mv template.java my-module.java
```

### 3. Replace the names in the file

Open `my-module.java` and replace:

| Was | Becomes |
|------|--------|
| `templateModule` | `myModule` |
| `"template"` | `"my-module"` |
| `"Template"` | `"My Module"` |
| `mymodule_` | `mymodule_` (in the placeholders) |

### 4. Set up config.yml

```yaml
name: "My Module"
version: "1.0"
enabled: true
description: "My first module"
author: "YourName"

commands:
  - mycommand

permissions:
  - mymodule.use
  - mymodule.admin
```

### 5. Add the command to plugin.yml (optional)

```yaml
commands:
  mycommand:
    description: My module command
    usage: /mycommand <args>
    permission: mymodule.use
```

### 6. Reload

```bash
/ci reload
```

## 📁 Module structure

```
items/my-module/
├── config.yml        — Settings
├── my-module.java    — Main class
├── items.yml         — Items (optional)
└── README.md         — Documentation (optional)
```

## 🔧 Available hooks

### Main ones

| Method | Description |
|-------|-------------|
| `onEnable()` | Called when the module is enabled |
| `onDisable()` | Called when the module is disabled |

### Fields

| Field | Type | Description |
|-------|-----|-------------|
| `plugin` | `Main` | The plugin's main class |
| `id` | `String` | The module ID (the folder name) |
| `folder` | `File` | The module folder |
| `config` | `YamlConfiguration` | The module config |
| `itemsConfig` | `YamlConfiguration` | The module items |

## 📝 Examples

### Command registration

```java
@Override
protected void onEnable() {
    registerCommand("mycommand", this);
}

@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    sender.sendMessage("Hello!");
    return true;
}
```

### Placeholder

```java
private void registerPlaceholders() {
    plugin.getPlaceholderManager().register("mymodule_stat", (player) -> {
        return "42"; // %dci_mymodule_stat%
    });
}
```

### Event listener

```java
@Override
protected void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
}

@EventHandler
public void onJoin(PlayerJoinEvent event) {
    event.getPlayer().sendMessage("Hello!");
}
```

### GUI menu

```java
private void openMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 27, "§6Menu");
    // ... filling the menu
    player.openInventory(menu);
}
```

### Working with the database

```java
plugin.getDatabaseManager().execute(
    "CREATE TABLE IF NOT EXISTS my_data (uuid TEXT, key TEXT, value TEXT)"
);
```

## 🗑️ Removing the module

Just delete the folder:

```bash
rm -rf plugins/Artifex/items/my-module/
/ci reload
```

## 📚 Ready-made modules

In the jar/repository the modules are stored as samples with the `EXAMPLE-*` prefix —
copy the folder into `items/` and remove the prefix from the name:

| Sample | Description |
|---------|-------------|
| `EXAMPLE-vault/` | 💰 Economy (Vault) |
| `EXAMPLE-deluxemenux/` | 🎨 GUI menus |
| `EXAMPLE-shop/` | 🛒 Shop |
