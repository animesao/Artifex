# Troubleshooting Guide

## 📚 Common Issues and Solutions

This guide helps you solve common problems with Artifex.

---

## 🚀 Installation Issues

### Plugin Not Loading

**Problem:** Plugin doesn't appear in `/pl`

**Symptoms:**
- Plugin not in plugin list
- No commands working

**Solutions:**

1. **Check Java Version:**
   ```bash
   java -version
   ```
   Need Java 21 or higher.

2. **Check File Location:**
   ```
   server/plugins/dcustomitems.jar
   ```
   Must be in `plugins/` folder.

3. **Check File Name:**
   - Should be `dcustomitems.jar`
   - No spaces or special characters

4. **Check Console for Errors:**
   Look for error messages on startup.

5. **Verify Server Version:**
   - Need Spigot/Paper 1.20+

---

### Plugin Crashes on Load

**Problem:** Server crashes when loading plugin

**Symptoms:**
- Error in console
- Server stops

**Solutions:**

1. **Check Error Message:**
   Read the full error in console.

2. **Verify Dependencies:**
   Some features need Paper.

3. **Check Plugin Version:**
   Make sure version matches server.

4. **Test with Minimal Setup:**
   Remove other plugins temporarily.

---

## 🎯 Item Issues

### Item Not Appearing

**Problem:** Item doesn't show after `/ci give`

**Symptoms:**
- Command works but no item
- Item not in inventory

**Solutions:**

1. **Check Item ID:**
   ```bash
   /ci list
   ```
   Verify item exists.

2. **Check Permissions:**
   ```yaml
   permission: "myplugin.use"
   ```
   Make sure you have the permission.

3. **Reload Plugin:**
   ```bash
   /ci reload
   ```

4. **Check YAML Syntax:**
   - Proper indentation
   - No tabs (use spaces)
   - Correct quotes

5. **Check Console:**
   Look for error messages.

---

### Item Not Working

**Problem:** Item exists but effects don't work

**Symptoms:**
- Item in inventory
- No effects on use

**Solutions:**

1. **Check Activation Slot:**
   ```yaml
   activation-slot: HAND
   ```
   Must match where item is held.

2. **Check Trigger Actions:**
   ```yaml
   trigger-actions:
     - 'on_click_right:message:Hello!'
   ```
   Verify event type is correct.

3. **Check Cooldown:**
   ```yaml
   click-cooldown: 3000
   ```
   Wait for cooldown or set to 0.

4. **Check Permissions:**
   Some actions need specific permissions.

5. **Test in Creative:**
   Try in creative mode first.

---

### Effects Not Showing

**Problem:** Particles/effects don't appear

**Symptoms:**
- Item works
- No visual effects

**Solutions:**

1. **Check Particle Name:**
   ```yaml
   - 'on_click_right:particle:FLAME:50'
   ```
   Use correct particle names.

2. **Check Client Settings:**
   - Particles must be enabled
   - Graphics quality matters

3. **Check Distance:**
   Effects may not show from far away.

4. **Test Different Particles:**
   Try `FLAME`, `HEART`, `CRIT`.

---

## 🎨 Resource Pack Issues

### Model Not Showing

**Problem:** Item shows default texture

**Symptoms:**
- Item works
- Wrong texture

**Solutions:**

1. **Check Model Name:**
   ```yaml
   item-model: "my_model"
   ```
   Must match file name.

2. **Check File Path:**
   ```
   assets/minecraft/models/item/my_model.json
   ```

3. **Check pack.mcmeta:**
   ```json
   {
     "pack": {
       "description": "My Pack",
       "pack_format": 34
     }
   }
   ```

4. **Check Pack Format:**
   Must match server version.

5. **Reload Resource Pack:**
   - F3 + T in game
   - Or restart game

---

### Texture Missing

**Problem:** Purple/black texture

**Symptoms:**
- Wrong texture colors

**Solutions:**

1. **Check PNG Exists:**
   ```
   assets/minecraft/textures/item/my_model.png
   ```

2. **Check File Name:**
   Must match model reference.

3. **Check Image Format:**
   - Use PNG format
   - Valid image file

4. **Check Transparency:**
   Alpha channel may cause issues.

---

## ☕ Java API Issues

### Java Item Not Compiling

**Problem:** .java file fails to compile

**Symptoms:**
- Error in console
- Item not loaded

**Solutions:**

1. **Check Class Name:**
   ```java
   public class MySword extends AbstractCustomItem
   ```
   Must match filename.

2. **Check Imports:**
   ```java
   import me.dcplugin.artifex.api.AbstractCustomItem;
   import me.dcplugin.artifex.api.ItemAPI;
   ```

3. **Check Methods:**
   - Must override required methods
   - Correct method signatures

4. **Check Syntax:**
   - Semicolons
   - Braces
   - No typos

---

### Java Item Not Working

**Problem:** Compiled but doesn't work

**Symptoms:**
- Item appears
- No effects

**Solutions:**

1. **Check Item ID:**
   ```java
   @Override
   public String getId() { return "my_sword"; }
   ```

2. **Check Event Methods:**
   ```java
   @Override
   public void onRightClick(PlayerInteractEvent event, Player player) {
       // Your code
   }
   ```

3. **Check ItemAPI Usage:**
   ```java
   ItemAPI.heal(player, 5);
   ItemAPI.effect(player, PotionEffectType.SPEED, 10, 1);
   ```

4. **Reload and Test:**
   ```bash
   /ci reload
   /api-item give my_sword
   ```

---

## 🔧 Command Issues

### Command Not Found

**Problem:** Command doesn't work

**Symptoms:**
- "Unknown command" message

**Solutions:**

1. **Check Plugin Loaded:**
   ```bash
   /pl
   ```

2. **Check Command Spelling:**
   ```bash
   /ci give my_sword
   ```

3. **Check Permissions:**
   ```yaml
   customitems.give: op
   ```

4. **Try Alias:**
   ```bash
   /customitems give my_sword
   ```

---

### No Permission

**Problem:** "No permission" message

**Symptoms:**
- Command blocked

**Solutions:**

1. **Check Your Permissions:**
   - You need OP or specific permission

2. **Add Permission:**
   ```bash
   /op your_name
   ```

3. **Use Permission Plugin:**
   - LuckPerms
   - PermissionsEx
   - GroupManager

---

## 💡 General Tips

### Tip 1: Check Console First

Always check server console for errors:
- Startup messages
- Error messages
- Warnings

### Tip 2: Test Simple Setup

Start with minimal config:
```yaml
test_item:
  item:
    type: DIAMOND_SWORD
    title: 'Test'
  trigger-actions:
    - 'on_click_right:message:Hello!'
```

### Tip 3: Reload After Changes

Always reload after config changes:
```bash
/ci reload
```

### Tip 4: Check File Permissions

Make sure files are readable:
```bash
chmod 644 items/*.yml
```

### Tip 5: Backup Configs

Keep backups of working configs.

---

## 📞 Getting More Help

### Check Documentation

1. Read relevant guides
2. Search for similar issues
3. Check examples

### Create Issue

If you find a bug:
1. Go to GitHub Issues
2. Describe the problem
3. Include error messages
4. Include server version

### Join Community

- Discord server
- GitHub Discussions
- Reddit

---

## 📚 Related Documentation

- [Getting Started](GETTING_STARTED_EN.md)
- [YAML Items Guide](YAML_ITEMS_EN.md)
- [Java API Guide](JAVA_API_EN.md)
- [Commands Reference](COMMANDS_EN.md)

---

**Back to:** [README](README_EN.md)
