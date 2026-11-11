# YAML Items Configuration Guide

## 📚 Complete Reference

This guide covers everything you need to know about configuring items using YAML files.

---

## 📁 File Structure

### Where to Place Files

```
plugins/Artifex/
├── items/
│   ├── my-sword.yml
│   ├── my-armor.yml
│   └── my-potion.yml
├── config.yml
└── messages.yml
```

### File Naming

- Use lowercase with hyphens: `my-sword.yml`
- Avoid spaces and special characters
- One item per file (recommended)

---

## 🎯 Basic Item Structure

```yaml
item_id:
  item:
    type: MATERIAL
    title: '&6Display Name'
    glowing: true
    unbreakable: true
  lore:
    - ''
    - '&7Description'
    - ''
  type: TOOL
  activation-slot: HAND
  trigger-actions:
    - 'on_event:action'
```

---

## ⚔️ Item Properties

### Material Types

| Category | Materials |
|----------|-----------|
| **Swords** | WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD |
| **Axes** | WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE |
| **Pickaxes** | WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE |
| **Shovels** | WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL |
| **Bows** | BOW, CROSSBOW |
| **Armor** | LEATHER_HELMET, CHAINMAIL_HELMET, IRON_HELMET, DIAMOND_HELMET, NETHERITE_HELMET |
| **Tools** | FLINT_AND_STEEL, FISHING_ROD, SHEARS, TRIDENT |
| **Food** | APPLE, GOLDEN_APPLE, BREAD, COOKED_BEEF, CARROT, GOLDEN_CARROT |
| **Potions** | POTION, SPLASH_POTION, LINGERING_POTION |
| **Special** | TOTEM_OF_UNDYING, ENDER_PEARL, ELYTRA, SHIELD |

### Color Codes

| Code | Color | Code | Color |
|------|-------|------|-------|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Light Purple |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |

### Formatting Codes

| Code | Effect |
|------|--------|
| `&l` | Bold |
| `&n` | Underline |
| `&o` | Italic |
| `&m` | Strikethrough |
| `&k` | Obfuscated |
| `&r` | Reset |

---

## 📝 Complete Example

```yaml
legendary_sword:
  item:
    type: NETHERITE_SWORD
    title: '&6&lLegendary Sword'
    glowing: true
    unbreakable: true
    custom-model-data: 1001
    item-model: "legendary_sword"
    enchantments:
      SHARPNESS: 5
      UNBREAKING: 3
      SWEEPING_EDGE: 3
      FIRE_ASPECT: 2
      LOOTING: 3
  lore:
    - ''
    - '&7A sword of legendary power'
    - '&7Forged in the fires of Nether'
    - ''
    - '&eRight-click for special ability!'
    - ''
    - '&6Stats:'
    - '&c+10 Attack Damage'
    - '&b+5% Critical Hit'
    - ''
  type: TOOL
  activation-slot: HAND
  placeable: false
  click-cooldown: 5000
  permission: "legendary.sword"
  trigger-actions:
    - 'on_click_right:effect:STRENGTH:10:2'
    - 'on_click_right:effect:SPEED:10:1'
    - 'on_click_right:particle:FLAME:100'
    - 'on_click_right:particle:CRIT:50'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1.5'
    - 'on_click_right:message:&6Legendary power activated!'
    - 'on_click_right:title:&6&lLEGENDARY POWER!::&7Strength and Speed for 10 seconds!'
    - 'on_equip:message:&6Legendary Sword equipped!'
    - 'on_unequip:message:&6Legendary Sword unequipped.'
    - 'on_damage_dealt:damage:5'
    - 'on_damage_taken:effect:DAMAGE_RESISTANCE:5:1'
    - 'on_kill:heal:10'
    - 'on_kill:effect:REGENERATION:5:2'
    - 'on_block_break:particle:BLOCK_BREAK:20'
```

---

## 🎯 Event Types

### Click Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_click_left` | Left click (LKM) | Attack with item |
| `on_click_right` | Right click (PKM) | Use item ability |

### Equipment Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_equip` | Item equipped | Put on armor |
| `on_unequip` | Item unequipped | Remove armor |
| `on_swap_hand` | Switch hands (F) | Swap main/offhand |

### Combat Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_damage_dealt` | Deal damage | Hit entity |
| `on_damage_taken` | Take damage | Get hit |
| `on_kill` | Kill entity | Kill mob/player |
| `on_death` | Player death | Die with item |

### Movement Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_jump` | Player jump | Jump with item |
| `on_move` | Player move | Walk with item |

### Block Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_block_break` | Break block | Mine with pickaxe |
| `on_block_place` | Place block | Build with item |

### Item Events

| Event | Description | Example |
|-------|-------------|---------|
| `on_drop` | Drop item | Throw item |
| `on_pickup` | Pick up item | Collect item |

---

## 🔧 Action Types

### Message Actions

```yaml
- 'on_click_right:message:Hello World!'
- 'on_click_right:message:&aGreen message!'
- 'on_click_right:message:&cRed &lbold message!'
```

### Effect Actions (Potion Effects)

```yaml
# Format: effect:TYPE:DURATION_SECONDS:LEVEL
- 'on_click_right:effect:SPEED:10:2'
- 'on_click_right:effect:STRENGTH:30:1'
- 'on_click_right:effect:REGENERATION:5:3'
```

**Available Effects:**

| Effect | Description |
|--------|-------------|
| SPEED | Movement speed |
| SLOWNESS | Reduced speed |
| HASTE | Mining speed |
| MINING_FATIGUE | Reduced mining |
| STRENGTH | Attack damage |
| JUMP_BOOST | Jump height |
| NAUSEA | Screen wobble |
| REGENERATION | Health regen |
| RESISTANCE | Damage reduction |
| FIRE_RESISTANCE | Fire immunity |
| WATER_BREATHING | Breathe underwater |
| INVISIBILITY | Turn invisible |
| NIGHT_VISION | See in dark |
| WEAKNESS | Reduced damage |
| POISON | Damage over time |
| WITHER | Wither effect |
| HEALTH_BOOST | Extra hearts |
| ABSORPTION | Yellow hearts |
| SATURATION | Food saturation |
| GLOWING | See through walls |
| LUCK | Better loot |
| UNLUCK | Worse loot |
| DOLPHINS_GRACE | Swim speed |
| CONDUIT_POWER | Underwater power |
| SLOW_FALLING | Fall slowly |
| BAD_OMEN | Raid omen |
| HERO_OF_THE_VILLAGE | Village hero |

### Particle Actions

```yaml
# Format: particle:TYPE:COUNT
- 'on_click_right:particle:FLAME:50'
- 'on_click_right:particle:HEART:20'
- 'on_click_right:particle:CRIT:30'
```

**Available Particles:**

| Particle | Description |
|----------|-------------|
| FLAME | Fire particles |
| SOUL_FIRE_FLAME | Blue fire |
| HEART | Red hearts |
| CRIT | Critical hit |
| CRIT_MAGIC | Enchanted crit |
| SPELL | Magic particles |
| INSTANT_SPELL | Instant magic |
| MOB_SPELL | Mob magic |
| NOTE | Music note |
| PORTAL | Nether portal |
| ENCHANTMENT_TABLE | Enchanting |
| EXPLOSION_NORMAL | Small explosion |
| EXPLOSION_HUGE | Large explosion |
| FIREWORKS_SPARK | Firework |
| WATER_SPLASH | Water splash |
| WATER_WAKE | Water wake |
| SUSPENDED_DEPTH | Underwater |
| CLOUD | Cloud |
| REDSTONE | Redstone dust |
| SNOWBALL | Snowball |
| SNOW_SHOVEL | Snow |
| SLIME | Slime |
| HEART | Hearts |
| BARRIER | Barrier block |
| ITEM_CRACK | Item break |
| BLOCK_CRACK | Block break |
| BLOCK_DUST | Block dust |
| WATER_DROP | Water drop |
| ITEM_TAKE | Item pickup |
| MOB_APPEARANCE | Guardian |

### Sound Actions

```yaml
# Format: sound:TYPE:VOLUME:PITCH
- 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
- 'on_click_right:sound:ENTITY_BLAZE_SHOOT:2:0.5'
```

**Common Sounds:**

| Sound | Description |
|-------|-------------|
| ENTITY_PLAYER_LEVELUP | Level up |
| ENTITY_PLAYER_BURP | Burp |
| ENTITY_PLAYER_DEATH | Death |
| ENTITY_BLAZE_SHOOT | Blaze shoot |
| ENTITY_CREEPER_HISS | Creeper |
| ENTITY_ENDERMAN_TELEPORT | Enderman |
| ENTITY_EXPLODE | Explosion |
| ENTITY_LIGHTNING_THUNDER | Thunder |
| BLOCK_GLASS_BREAK | Glass break |
| BLOCK_STONE_BREAK | Stone break |
| ITEM_ARMOR_EQUIP_DIAMOND | Diamond equip |
| ITEM_ARMOR_EQUIP_NETHERITE | Netherite equip |

### Healing Actions

```yaml
# Format: heal:AMOUNT (in half-hearts)
- 'on_click_right:heal:10'  # +5 hearts
- 'on_kill:heal:20'  # Full heal
```

### Damage Actions

```yaml
# Format: damage:AMOUNT
- 'on_click_right:damage:5'
- 'on_damage_dealt:damage:10'
```

### Teleport Actions

```yaml
# Format: teleport:X:Y:Z
- 'on_click_right:teleport:100:64:200'
```

### Title Actions

```yaml
# Format: title:TITLE:SUBTITLE:FADE_IN:STAY:FADE_OUT
- 'on_click_right:title:POWER!::Activated:10:40:10'
- 'on_click_right:title:&6&lPOWER!::&7Ability activated!:10:60:20'
```

### Give/Remove Actions

```yaml
# Format: give:MATERIAL:AMOUNT
- 'on_kill:give:DIAMOND:1'

# Format: remove:MATERIAL:AMOUNT
- 'on_click_right:remove:DIAMOND:1'
```

---

## 🎭 Item Types

| Type | Description | Example |
|------|-------------|---------|
| `TOOL` | Tools (swords, pickaxes) | Weapons, tools |
| `ARMOR` | Wearable items | Helmets, chestplates |
| `RUNE` | Special items | Teleport items, abilities |
| `CONSUMABLE` | Consumable items | Potions, food |
| `PLACEABLE` | Can be placed | Blocks, decorations |

---

## 🎯 Activation Slots

| Slot | Description | When Active |
|------|-------------|-------------|
| `HAND` | Main hand | Holding in right hand |
| `OFFHAND` | Off hand | Holding in left hand |
| `HEAD` | Helmet slot | Wearing on head |
| `CHEST` | Chestplate slot | Wearing on chest |
| `LEGS` | Leggings slot | Wearing on legs |
| `FEET` | Boots slot | Wearing on feet |

---

## 📊 Complete Examples

### Fire Sword

```yaml
fire_sword:
  item:
    type: DIAMOND_SWORD
    title: '&c&lFire Sword'
    glowing: true
    unbreakable: true
  lore:
    - ''
    - '&7Burns enemies on hit'
    - '&7Right-click for fire aura'
    - ''
  type: TOOL
  activation-slot: HAND
  click-cooldown: 3000
  trigger-actions:
    - 'on_damage_dealt:fire:100'
    - 'on_click_right:particle:FLAME:100'
    - 'on_click_right:effect:FIRE_RESISTANCE:30:0'
    - 'on_click_right:message:&cFire aura activated!'
```

### Healing Potion

```yaml
healing_potion:
  item:
    type: EXPERIENCE_BOTTLE
    title: '&a&lHealing Potion'
    glowing: true
  lore:
    - ''
    - '&7Restores 10 hearts'
    - '&7Grants regeneration'
    - ''
  type: CONSUMABLE
  activation-slot: HAND
  trigger-actions:
    - 'on_click_right:heal:20'
    - 'on_click_right:effect:REGENERATION:10:2'
    - 'on_click_right:particle:HEART:50'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
    - 'on_click_right:message:&aHealth restored!'
    - 'on_click_right:remove:SELF:1'
```

### Protective Armor

```yaml
protective_helmet:
  item:
    type: DIAMOND_HELMET
    title: '&b&lProtective Helmet'
    glowing: true
    unbreakable: true
    enchantments:
      PROTECTION: 4
      UNBREAKING: 3
  lore:
    - ''
    - '&7Grants night vision'
    - '&7Regenerates health'
    - ''
  type: ARMOR
  activation-slot: HEAD
  trigger-actions:
    - 'on_equip:effect:NIGHT_VISION:999999:0'
    - 'on_equip:effect:REGENERATION:999999:0'
    - 'on_equip:message:&bHelmet equipped!'
    - 'on_unequip:message:&bHelmet unequipped.'
    - 'on_unequip:clear_effects'
```

---

## 💡 Tips

### Tip 1: Use Comments

Add comments to explain what each action does:

```yaml
my_item:
  trigger-actions:
    # When right-clicked
    - 'on_click_right:message:Hello!'  # Show greeting
    - 'on_click_right:effect:SPEED:5:1'  # Speed boost
```

### Tip 2: Organize by Event

Group actions by event type for clarity:

```yaml
my_item:
  trigger-actions:
    # Click actions
    - 'on_click_right:message:Used!'
    - 'on_click_right:effect:SPEED:5:1'
    
    # Equip actions
    - 'on_equip:message:Equipped!'
    - 'on_unequip:message:Unequipped.'
    
    # Combat actions
    - 'on_damage_dealt:damage:5'
    - 'on_kill:heal:10'
```

### Tip 3: Test Each Action

Test items after adding each action to catch errors early.

### Tip 4: Use Cooldowns

Prevent spam with cooldowns:

```yaml
my_item:
  click-cooldown: 3000  # 3 seconds
```

---

## 📚 Related Documentation

- [Getting Started](GETTING_STARTED_EN.md)
- [Commands Reference](COMMANDS_EN.md)
- [Java API Guide](JAVA_API_EN.md)
- [Resource Pack Guide](RESOURCE_PACK_EN.md)

---

**Next:** [Commands Reference](COMMANDS_EN.md) →
