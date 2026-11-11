# Commands Reference

## 📚 Complete Command List

This guide covers all available commands in Artifex.

---

## 🎯 Main Commands

### /ci (customitems)

The main command for the plugin.

**Usage:**
```
/ci <subcommand> [args]
```

**Aliases:**
- `/customitems`
- `/ci`

---

## 📝 Item Commands

### /give

Give an item (custom or vanilla).

**Syntax:**
```
/give <id|material> [player] [amount]
```

**Examples:**
```bash
/give vampire-blade                    # Custom item to yourself
/give diamond                          # Diamond to yourself
/give minecraft:netherite_sword Steve  # Netherite sword to player
/give cooked_beef 64                   # 64 cooked beef
/give diamond all 16                   # 16 diamonds to all online
/give list                             # List custom items
/give materials                        # All vanilla materials
/give materials diamond                # Search materials
```

**Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| item_id | Yes | ID of the item to give |
| player | No | Player to give to (default: yourself) |

**Examples:**
```bash
# Give item to yourself
/ci give my_sword

# Give item to another player
/ci give my_sword Steve

# Give item to nearest player
/ci give my_sword @p
```

**Permissions:**
- `customitems.give` (default: op)

---

### /give list

List all available custom items.

**Syntax:**
```
/give list
```

**Examples:**
```bash
# List all items
/give list
```

**Output:**
```
[Artifex] Available items: my_sword, my_armor, fire_sword
```

**Permissions:**
- `customitems.list` (default: true)

---

### /ci reload

Reload all items and configurations.

**Syntax:**
```
/ci reload
```

**Examples:**
```bash
# Reload plugin
/ci reload
```

**Output:**
```
[Artifex] Reloaded 53 custom items
[Artifex] [API] Loaded 2 Java API items
```

**Permissions:**
- `customitems.reload` (default: op)

---

## 🎯 API Item Commands

### /api-item give

Give a Java API item to a player.

**Syntax:**
```
/api-item give <item_id> [player]
```

**Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| item_id | Yes | ID of the Java API item |
| player | No | Player to give to (default: yourself) |

**Examples:**
```bash
# Give API item to yourself
/api-item give fire_sword

# Give API item to another player
/api-item give fire_sword Steve
```

**Permissions:**
- `customitems.give` (default: op)

---

### /api-item list

List all available Java API items.

**Syntax:**
```
/api-item list
```

**Examples:**
```bash
# List all API items
/api-item list
```

**Output:**
```
[Artifex] Java API items: fire_sword, teleport_staff
```

**Permissions:**
- `customitems.list` (default: true)

---

### /api-item info

Get information about a Java API item.

**Syntax:**
```
/api-item info <item_id>
```

**Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| item_id | Yes | ID of the Java API item |

**Examples:**
```bash
# Get item info
/api-item info fire_sword
```

**Output:**
```
[Artifex] Item: fire_sword
[Artifex] Name: Fire Sword
[Artifex] Material: NETHERITE_SWORD
[Artifex] Type: TOOL
```

**Permissions:**
- `customitems.list` (default: true)

---

## 🔧 Utility Commands

### /ci help

Show help message.

**Syntax:**
```
/ci help
```

**Examples:**
```bash
# Show help
/ci help
```

**Output:**
```
[Artifex] Available commands:
[Artifex] /ci give <item> [player] - Give custom item
[Artifex] /give list - List all items
[Artifex] /ci reload - Reload plugin
[Artifex] /api-item give <id> [player] - Give API item
[Artifex] /api-item list - List API items
[Artifex] /api-item info <id> - Item information
```

**Permissions:**
- `customitems.list` (default: true)

---

## 📋 Complete Examples

### Example 1: Give Multiple Items

```bash
# Give yourself multiple items
/ci give my_sword
/ci give my_armor
/ci give fire_sword
```

### Example 2: Give Items to Player

```bash
# Give items to Steve
/ci give my_sword Steve
/ci give my_armor Steve
/ci give fire_sword Steve
```

### Example 3: Reload and Verify

```bash
# Reload and check items
/ci reload
/give list
```

---

## 🔐 Permissions

### Default Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `customitems.give` | Give custom items | op |
| `customitems.list` | List items | true |
| `customitems.reload` | Reload plugin | op |
| `customitems.admin` | Admin permissions | op |

### Per-Item Permissions

You can set custom permissions for each item:

```yaml
my_sword:
  permission: "myplugin.sword"
```

Players need this permission to use the item.

---

## 💡 Tips

### Tip 1: Use Tab Completion

All commands support tab completion:
- Type `/ci give ` and press Tab
- Type `/give list` and press Enter

### Tip 2: Check Console Output

After commands, check console for:
- Success messages
- Error messages
- Warnings

### Tip 3: Use @ selectors

Use Minecraft selectors:
- `@p` - Nearest player
- `@a` - All players
- `@s` - Yourself

### Tip 4: Permission Nodes

Use permission nodes in your permission plugin:
- GroupManager
- LuckPerms
- PermissionsEx

---

## 📚 Related Documentation

- [Getting Started](GETTING_STARTED_EN.md)
- [YAML Items Guide](YAML_ITEMS_EN.md)
- [Java API Guide](JAVA_API_EN.md)
- [Troubleshooting](TROUBLESHOOTING_EN.md)

---

**Next:** [Troubleshooting](TROUBLESHOOTING_EN.md) →
