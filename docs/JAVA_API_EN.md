# Java API Guide

## 📚 Complete Reference

This guide covers everything about creating custom items using the Java API.

---

## 🎯 Overview

The Java API allows you to create advanced custom items with full Java code support. This is perfect for:

- Complex item behaviors
- Custom game mechanics
- Advanced effects and animations
- Items with state management
- Multi-functional items

---

## 🚀 Quick Start

### Step 1: Create Item Class

Create a new Java file in `plugins/Artifex/items/`:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my_sword"; }

    @Override
    public String getDisplayName() { return "&6My Java Sword"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        ItemAPI.heal(player, 5);
        ItemAPI.effect(player, PotionEffectType.SPEED, 10, 1);
        ItemAPI.particles(player, Particle.FLAME, 50);
    }
}
```

### Step 2: Reload Plugin

```
/ci reload
```

### Step 3: Get Item

```
/api-item give my_sword
```

---

## 📝 Class Structure

### Required Methods

```java
public class MyItem extends AbstractCustomItem {

    // REQUIRED: Unique item ID
    @Override
    public String getId() { return "my_item"; }

    // REQUIRED: Display name
    @Override
    public String getDisplayName() { return "&6My Item"; }

    // REQUIRED: Material type
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
}
```

### Optional Methods

```java
public class MyItem extends AbstractCustomItem {

    @Override
    public String getId() { return "my_item"; }

    @Override
    public String getDisplayName() { return "&6My Item"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    // Optional: Item lore
    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Description line 1",
            "&eDescription line 2",
            ""
        );
    }

    // Optional: Custom model for resource pack
    @Override
    public String getItemModel() { return "my_model"; }

    // Optional: Unbreakable
    @Override
    public boolean isUnbreakable() { return true; }

    // Optional: Glowing effect
    @Override
    public boolean isGlowing() { return true; }

    // Optional: Click cooldown in milliseconds
    @Override
    public long getClickCooldown() { return 3000; }

    // Optional: Required permission
    @Override
    public String getPermission() { return "myplugin.use"; }

    // Optional: Item type
    @Override
    public String getType() { return "TOOL"; }

    // Optional: Activation slot
    @Override
    public String getActivationSlot() { return "HAND"; }
}
```

---

## 🎯 Event Methods

### Click Events

```java
@Override
public void onLeftClick(PlayerInteractEvent event, Player player) {
    // Left click (LKM) action
}

@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    // Right click (PKM) action
}
```

### Equipment Events

```java
@Override
public void onEquip(Player player) {
    // When item is equipped
}

@Override
public void onUnequip(Player player) {
    // When item is unequipped
}
```

`onEquip`/`onUnequip` are invoked by the global equipment checker when the item sits in its `getActivationSlot()` (HEAD/CHEST/LEGS/FEET/HAND/OFFHAND). Before the hook, the plugin fires the Bukkit `CustomItemEquipEvent` — if a third-party plugin cancels it, the hook is skipped (see “Item events” in the README).

### Combat Events

```java
@Override
public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
    // When dealing damage
}

@Override
public void onDamageTaken(EntityDamageEvent event, Player player) {
    // When taking damage
}

@Override
public void onKill(Player killer, Player victim) {
    // When killing a player
}

@Override
public void onDeath(Player player, PlayerDeathEvent event) {
    // When player dies with this item
}
```

### Movement Events

```java
@Override
public void onJump(Player player) {
    // When player jumps
}

@Override
public void onMove(PlayerMoveEvent event, Player player) {
    // When player moves
}
```

### Block Events

```java
@Override
public void onBlockBreak(Player player, BlockBreakEvent event) {
    // When breaking a block
}
```

### Item Events

```java
@Override
public void onDrop(Player player, PlayerDropItemEvent event) {
    // When dropping item
}

@Override
public void onPickup(Player player) {
    // When picking up item
}
```

### Periodic Effects

```java
@Override
public long getPeriodicInterval() {
    return 20; // Every 20 ticks (1 second)
}

@Override
public void onPeriodic(Player player) {
    // Called periodically
}
```

`onPeriodic` is also driven by the global equipment checker: it fires only while the item is in its activation slot, at most once per `getPeriodicInterval()` ticks. Keep the body lightweight.

### YAML-level mechanics for Java items

Java items now support the base mechanics that YAML items have:

```java
@Override
public int getMaxUses() { return 5; }        // 0 = unlimited (consumed on clicks)

@Override
public long getDuration() { return 3600; }   // lifetime in seconds, 0 = forever

@Override
public boolean isAllowedInWorld(String worldName) {
    return !worldName.equals("pvp_arena");   // forbid in your own worlds
}

@Override
public String getPermission() { return "items.frostblade"; } // now enforced on clicks
```

`getMaxUses()` is consumed by the click handler (the item is removed when depleted); `getDuration()` and `isAllowedInWorld()` are enforced by the global checker (violations remove the item). Messages are overridden via `getUsesDepletedMessage()`, `getDurationExpiredMessage()`, `getWorldBlockedMessage()`.

Before each hook (`onDamageDealt`, `onDamageTaken`, `onKill`, `onDeath`, `onPeriodic`) the matching cancellable `CustomItem*Event` Bukkit event now fires, so third-party plugins can suppress the item's default reaction.

---

## 🛠️ ItemAPI Utilities

### Healing

```java
// Heal player by amount (in half-hearts)
ItemAPI.heal(player, 10); // +5 hearts
```

### Effects

```java
// Apply potion effect
ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
// effect: player, type, duration_seconds, level
```

### Particles

```java
// Spawn particles
ItemAPI.particles(player, Particle.FLAME, 50);
// particles: player, type, count
```

### Sounds

```java
// Play sound
ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
// sound: player, type, volume, pitch
```

### Teleportation

```java
// Teleport to coordinates
ItemAPI.teleport(player, 100, 64, 200);

// Teleport to another player
ItemAPI.teleportToPlayer(player, targetPlayer, 2);
```

### Lightning

```java
// Strike lightning forward
ItemAPI.lightningForward(player, 10);
```

### Damage

```java
// Damage nearby entities
ItemAPI.damageNearby(player, 10, 5);
// damageNearby: player, damage, radius
```

### Healing (AoE)

```java
// Heal nearby entities
ItemAPI.healNearby(player, 10, 5);
// healNearby: player, amount, radius
```

### Titles

```java
// Show title
ItemAPI.title(player, "Title", "Subtitle");

// Show title with timing
player.sendTitle("Title", "Subtitle", 10, 40, 10);
// fadeIn, stay, fadeOut in ticks
```

### Messages

```java
// Send message
ItemAPI.message(player, "&aHello!");
```

### Give Items

```java
// Give item to player
ItemAPI.giveItem(player, Material.DIAMOND, 5);
```

---

## 📝 Complete Examples

### Fire Sword

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FireSword extends AbstractCustomItem {

    @Override
    public String getId() { return "fire_sword"; }

    @Override
    public String getDisplayName() { return "&c&lFire Sword"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Right-click for fire power!",
            "&7Sets enemies on fire on hit",
            ""
        );
    }

    @Override
    public String getItemModel() { return "fire_sword"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getClickCooldown() { return 5000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Give fire resistance
        ItemAPI.effect(player, PotionEffectType.FIRE_RESISTANCE, 30, 0);

        // Particles and sound
        ItemAPI.particles(player, Particle.FLAME, 100);
        ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // Title
        ItemAPI.title(player, "&c&lFIRE POWER!", "&7Fire resistance for 30 seconds!");
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        // Set target on fire
        if (event.getEntity() instanceof LivingEntity) {
            ((LivingEntity) event.getEntity()).setFireTicks(100);
        }
    }
}
```

### Teleport Staff

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class TeleportStaff extends AbstractCustomItem {

    @Override
    public String getId() { return "teleport_staff"; }

    @Override
    public String getDisplayName() { return "&b&lTeleport Staff"; }

    @Override
    public Material getMaterial() { return Material.BLAZE_ROD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Right-click to teleport forward",
            "&7Left-click for random teleport",
            ""
        );
    }

    @Override
    public String getItemModel() { return "teleport_staff"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public long getClickCooldown() { return 3000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Teleport forward 10 blocks
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(10));
        ItemAPI.teleport(player, loc.getX(), loc.getY(), loc.getZ());

        // Effects
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&b&lTELEPORT!", "&7Forward 10 blocks");
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // Random teleport
        double x = player.getLocation().getX() + (Math.random() * 200 - 100);
        double z = player.getLocation().getZ() + (Math.random() * 200 - 100);
        double y = player.getWorld().getHighestBlockYAt((int) x, (int) z);

        ItemAPI.teleport(player, x, y + 1, z);

        // Effects
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&d&lRANDOM TELEPORT!", "");
    }
}
```

### Healing Armor

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HealingHelmet extends AbstractCustomItem {

    @Override
    public String getId() { return "healing_helmet"; }

    @Override
    public String getDisplayName() { return "&a&lHealing Helmet"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_HELMET; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Grants regeneration",
            "&7when equipped",
            ""
        );
    }

    @Override
    public String getItemModel() { return "healing_helmet"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public String getActivationSlot() { return "HEAD"; }

    @Override
    public long getPeriodicInterval() { return 100; } // Every 5 seconds

    @Override
    public void onEquip(Player player) {
        // Give regeneration
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 999999, 0);

        // Effects
        ItemAPI.particles(player, Particle.HEART, 20);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        ItemAPI.message(player, "&aHealing helmet equipped!");
    }

    @Override
    public void onUnequip(Player player) {
        // Remove regeneration
        player.removePotionEffect(PotionEffectType.REGENERATION);

        ItemAPI.message(player, "&aHealing helmet unequipped.");
    }

    @Override
    public void onPeriodic(Player player) {
        // Heal 1 heart every 5 seconds
        ItemAPI.heal(player, 2);
    }
}
```

---

## 📦 Crafting recipes (Java)

A Java item can declare crafting recipes by overriding `getRecipes()`:

```java
import me.dcplugin.artifex.api.RecipeDef;

@Override
public List<RecipeDef> getRecipes() {
    Map<Character, String> keys = new HashMap<>();
    keys.put('N', "NETHERITE_INGOT");      // material
    keys.put('B', "vampire-blade");        // or a custom item id (YAML/Java)

    return List.of(
        // Shaped: 1-3 rows of equal width, space = empty cell
        RecipeDef.shaped(List.of("NNN", "NBN"), keys),

        // Shapeless: a plain ingredient list
        RecipeDef.shapeless("DIAMOND", "DIAMOND", "STICK"),

        // Furnace: ingredient, experience, cooking time in ticks
        RecipeDef.furnace("IRON_INGOT", 0.7f, 200)
    );
}
```

These recipes are registered into the vanilla crafting table automatically and also appear in the GUI crafting module `/craft` (sample `items/EXAMPLE-customcraft/`). Any custom item — YAML or Java — can be an ingredient by id (including the item itself). `RecipeDef.shapeless(...)` defaults to 1 result item; overloads accept `amount`, and `furnace` accepts `experience` and `cookingTime`.

---

## 💡 Tips

### Tip 1: Use ItemAPI

Always use `ItemAPI` methods instead of raw Bukkit API when possible. It handles edge cases and provides consistent behavior.

### Tip 2: Check for Null

Always check for null values:

```java
@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    if (event.getItem() == null) return;
    
    // Your code here
}
```

### Tip 3: Use Cooldowns

Prevent ability spam:

```java
@Override
public long getClickCooldown() { return 3000; }
```

### Tip 4: Handle Permissions

Check permissions before powerful abilities:

```java
@Override
public String getPermission() { return "myplugin.powerful"; }
```

---

## 📚 Related Documentation

- [Getting Started](GETTING_STARTED_EN.md)
- [YAML Items Guide](YAML_ITEMS_EN.md)
- [Resource Pack Guide](RESOURCE_PACK_EN.md)
- [Commands Reference](COMMANDS_EN.md)

---

**Next:** [Resource Pack Guide](RESOURCE_PACK_EN.md) →
