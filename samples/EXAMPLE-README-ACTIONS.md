# Artifex - Complete documentation on actions and triggers

## 📋 YAML format (v1.320.225+)

```yaml
item-id:
  triggers:
    on_click_right:
      - 'action1:param1:param2'
      - 'action2:param1:param2'
```

---

## Available triggers

| Trigger | Description |
|---------|-------------|
| `on_equip` | When the item is equipped |
| `on_unequip` | When the item is removed |
| `on_click_right` | Right Click with the item |
| `on_click_left` | Left Click with the item |
| `on_kill` | When a mob/player is killed |
| `on_death` | When the player dies |
| `on_damage_taken` | When damage is taken |
| `on_damage_dealt` | When damage is dealt |
| `on_jump` | When jumping |
| `on_sprint` | When sprinting starts |
| `on_pickup` | When the item is picked up |
| `on_drop` | When the item is dropped |

---

## All available actions

### 📢 Communication

| Action | Format | Description |
|----------|--------|----------|
| `message` | `message:TEXT` | A message to the player |
| `title` | `title:HEADER:SUBTITLE:IN:STAY:OUT` | A title on the screen |
| `actionbar` | `actionbar:TEXT` | Text above the hotbar |
| `broadcast` | `broadcast:TEXT` | An announcement to all the players |

---

### ⚡ Effects

| Action | Format | Description |
|----------|--------|----------|
| `effect` | `effect:TYPE:SECONDS:LEVEL` | A potion effect |
| `speed` | `speed:SECONDS:LEVEL` | Speed (short) |
| `vanish` | `vanish:SECONDS` | Invisibility |
| `glow` | `glow:SECONDS` | Glowing |
| `flight` | `flight:true/false` | Flight |

---

### 💥 Combat actions

| Action | Format | Description |
|----------|--------|----------|
| `heal` | `heal:AMOUNT` | Heal yourself |
| `heal_nearby` | `heal_nearby:AMOUNT:RADIUS` | Heal the nearest players |
| `damage` | `damage:AMOUNT` | Damage yourself |
| `damage_nearby` | `damage_nearby:DAMAGE:RADIUS` | AoE damage |
| `damage_mobs` | `damage_mobs:DAMAGE:RADIUS` | Damage to mobs only |
| `damage_players` | `damage_players:DAMAGE:RADIUS` | Damage to players only |
| `lightning` | `lightning:COUNT` | Lightning in place |
| `lightning_forward` | `lightning_forward:DISTANCE` | Lightning far forward |
| `knockback` | `knockback:RADIUS` | Knock back the nearby ones |
| `launch` | `launch:STRENGTH` | Launch the nearby ones |
| `stun` | `stun:SECONDS:RADIUS` | Stun the nearby ones |

---

### 🌍 Teleportation

| Action | Format | Description |
|----------|--------|----------|
| `teleport` | `teleport:X:Y:Z` | Teleport to a point |
| `teleport_relative` | `teleport_relative:X:Y:Z` | Relative teleport |

---

### 🎁 Items

| Action | Format | Description |
|----------|--------|----------|
| `give` | `give:MATERIAL:COUNT` | Give an item |
| `remove` | `remove:MATERIAL:COUNT` | Take an item away |
| `exp` | `exp:XP:LEVELS` | Give experience |

---

### 🌎 World

| Action | Format | Description |
|----------|--------|----------|
| `sethealth` | `sethealth:VALUE` | Set the health |
| `setfood` | `setfood:VALUE` | Set the food |

---

### 🎆 Effects

| Action | Format | Description |
|----------|--------|----------|
| `particles` | `particles:TYPE:COUNT` | Particles |
| `sound` | `sound:TYPE:VOLUME:PITCH` | Sound |
| `fireworks` | `fireworks:1` | A firework |

---

### 🔧 Commands

| Action | Format | Description |
|----------|--------|----------|
| `command` | `command:COMMAND` | A command on behalf of the player |
| `console_command` | `console_command:COMMAND` | A command from the console |

---

## 🚀 Advanced actions (NO RESTRICTIONS!)

### 🎨 Custom particles

```yaml
# Format: particles_custom:TYPE:COUNT:X:Y:Z:OFF_X:OFF_Y:OFF_Z
- 'particles_custom:FLAME:100:0:1:0:2:2:2'
```

**Examples:**
```yaml
- 'particles_custom:FLAME:200:0:1:0:3:1:3'           # A fiery ring
- 'particles_custom:DRAGON_BREATH:100:0:2:0:2:2:2'    # Dragon breath
- 'particles_custom:PORTAL:150:0:1:0:2:2:2'           # Portal particles
- 'particles_custom:HEART:50:0:2:0:1:1:1'             # Little hearts
- 'particles_custom:EXPLOSION_HUGE:10:0:1:0:2:2:2'    # An explosion
```

---

### 🔊 Sound sequence

```yaml
# Format: sound_sequence:SOUND1:VOLUME1:PITCH1;SOUND2:VOLUME2:PITCH2;...
- 'sound_sequence:ENTITY_PLAYER_LEVELUP:1:1;ENTITY_ORB_PICKUP:0.5:1.5'
```

**Examples:**
```yaml
- 'sound_sequence:ENTITY_BLAZE_SHOOT:1:1;ENTITY_GENERIC_EXPLODE:1:0.8'
- 'sound_sequence:ENTITY_ENDERMAN_TELEPORT:1:1;ENTITY_PLAYER_LEVELUP:0.5:1.5'
```

---

### 🎬 Title sequence (an animation!)

```yaml
# Format: title_sequence:TITLE:SUB:IN:STAY:OUT;...
- 'title_sequence:🔥 PREPARATION!:::10:20:10;🔥 FIRE!:::10:30:10;🔥 STORM!:::10:40:10'
```

**Examples:**
```yaml
# An animation before the hit
- 'title_sequence:⚡ 3!:::5:10:5;⚡ 2!:::5:10:5;⚡ 1!:::5:10:5;💥 STRIKE!:::10:40:10'

# Combat phases
- 'title_sequence:🔥 PHASE 1!:::10:30:10;🔥 PHASE 2!:::10:30:10;🔥 PHASE 3!:::10:40:10'
```

---

### 📜 Command sequence with delays

```yaml
# Format: command_sequence:COMMAND1:DELAY1;COMMAND2:DELAY2;...
- 'command_sequence:give %player% diamond 1:20;say Hello!:40;give %player% emerald 1:60'
```

**Examples:**
```yaml
# A staged reward
- 'command_sequence:give %player% diamond 5:100;give %player% emerald 3:200;give %player% gold 10:300'
```

---

### 🌀 Teleportation sequence

```yaml
# Format: teleport_sequence:X1:Y1:Z1:DELAY1;X2:Y2:Z2:DELAY2;...
- 'teleport_sequence:100:64:200:200;200:64:300:200;300:64:400'
```

**Examples:**
```yaml
# A teleport series
- 'teleport_sequence:100:64:200:100;200:64:300:100;300:64:400:100;400:64:500'
```

---

### 💫 Effect sequence

```yaml
# Format: effect_sequence:TYPE1:SECONDS1:LEVEL1:DELAY1;...
- 'effect_sequence:SPEED:10:2:0;STRENGTH:10:3:1000;REGENERATION:10:2:2000'
```

**Examples:**
```yaml
# Building up the strength
- 'effect_sequence:SPEED:10:1:0;SPEED:10:2:2000;SPEED:10:3:4000;SPEED:10:4:6000'
```

---

### 💥 Custom damage (exact dimensions)

```yaml
# Format: damage_custom:DAMAGE:WIDTH:HEIGHT:DEPTH
- 'damage_custom:20:5:3:5'  # 20 damage in a 5x3x5 block area
```

**Examples:**
```yaml
- 'damage_custom:30:6:4:6'  # A powerful AoE strike
- 'damage_custom:10:3:2:3'  # A small AoE strike
- 'damage_custom:50:8:6:8'  # A huge AoE strike
```

---

### ❤️ Custom healing (with particles)

```yaml
# Format: heal_custom:HEAL:WIDTH:HEIGHT:DEPTH:PARTICLES
- 'heal_custom:10:5:5:5:HEART'  # Heals 10 HP in a 5x5x5 area with hearts
```

**Examples:**
```yaml
- 'heal_custom:15:8:5:8:HEART'      # Healing for the allies
- 'heal_custom:20:10:8:10:VILLAGER_HAPPY'  # Mass healing
```

---

## 🎮 A full example (all the actions)

```yaml
ultimate-fire-sword:
  type: TOOL
  activation-slot: HAND
  
  item:
    type: NETHERITE_SWORD
    title: '&c🔥 Fiery Blade'
    glowing: true
    unbreakable: true
    
  effects:
    - 'INCREASE_DAMAGE:5'
    - 'SPEED:3'
    - 'REGENERATION:3'
    
  triggers:
    on_equip:
      - 'particles_custom:FLAME:100:0:1:0:2:2:2'
      - 'sound_sequence:ENTITY_BLAZE_AMBIENT:1:1;ENTITY_PLAYER_LEVELUP:0.5:2'
      - 'title:&c🔥 BLADE DRAWN!::&7The fire element!:10:40:10'
      
    on_click_right:
      - 'title_sequence:🔥 PREPARATION!:::10:20:10;🔥 FIRE!:::10:30:10;🔥 STORM!:::10:40:10'
      - 'particles_custom:FLAME:200:0:1:0:3:1:3'
      - 'damage_custom:30:6:4:6'
      - 'sound_sequence:ENTITY_BLAZE_SHOOT:1:1;ENTITY_GENERIC_EXPLODE:1:0.8'
      - 'effect_sequence:STRENGTH:15:5:0;SPEED:15:3:500'
      
    on_kill:
      - 'heal:50'
      - 'effect_sequence:REGENERATION:20:4:0;STRENGTH:20:5:1000'
      - 'particles_custom:HEART:100:0:2:0:2:2:2'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:2'
      - 'fireworks:1'
```

---

## 🌍 World restrictions

An item can work only in certain worlds or be forbidden in some of them.

```yaml
my-item:
  # Only in these worlds (empty = all the worlds)
  allowed-worlds:
    - world
    - world_nether

  # Forbidden in these worlds
  disabled-worlds:
    - world_the_end
```

**Priority:** `allowed-worlds` is checked first. If both lists are empty — the item works everywhere.

---

## ⏱️ Item timer (auto-destroy)

The item is automatically removed after the specified time (in seconds).

```yaml
my-item:
  # The lifetime in seconds (-1 = forever)
  duration: 30

  # The message when the time runs out
  max-duration-message: '&cThe item evaporated!'
```

**Examples:**
```yaml
# A grenade — it disappears after 10 seconds
bomb:
  type: TOOL
  duration: 10
  max-duration-message: '&c💣 The grenade exploded!'
  right-click-actions:
    - 'damage_nearby:30:5'
    - 'particles_custom:EXPLOSION_HUGE:50:0:1:0:3:3:3'
    - 'sound:ENTITY_GENERIC_EXPLODE:1:1'
```

---

## ✨ Trail particles

The item leaves particles behind while the player moves.

```yaml
my-item:
  # Trail particles (the same format as in equip-particles)
  trail-particles:
    - 'FLAME:5'
    - 'SMOKE:3'

  # The particle spawn interval (in ticks, 20 ticks = 1 second)
  trail-particle-interval: 3
```

**Examples:**
```yaml
# A fiery blade — a fiery trail
fire-sword:
  type: TOOL
  trail-particles:
    - 'FLAME:8'
    - 'LAVA:3'
  trail-particle-interval: 2

# An icy blade — an icy trail
ice-sword:
  type: TOOL
  trail-particles:
    - 'SNOWFLAKE:5'
    - 'CLOUD:3'
  trail-particle-interval: 3

# A shadow blade — a shadow trail
shadow-sword:
  type: TOOL
  trail-particles:
    - 'SMOKE:4'
    - 'PORTAL:6'
  trail-particle-interval: 2
```

---

## 🎨 Animated models (item-model-variant)

For using animated textures in a Resource Pack.

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # The model variant (for animated textures)
    # Must match the name in item_model_definition
    item-model-variant: "animated_sword"
```

**An example of the Resource Pack structure:**
```
assets/minecraft/
├── textures/
│   └── item/
│       └── animated_sword.png          # The animation spritesheet
├── items/
│   └── netherite_sword.json            # The model definition
└── materials/
    └── animated_sword.png.mcmeta       # The animation metadata
```

**animated_sword.png.mcmeta:**
```json
{
  "animation": {
    "interpolate": true,
    "frametime": 2,
    "frames": [
      0, 1, 2, 3, 4, 5, 6, 7
    ]
  }
}
```

**netherite_sword.json:**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "minecraft:item/animated_sword"
  },
  "overrides": [
    {"predicate": {"custom_model_data": 100}, "model": "minecraft:item/animated_sword_frame1"}
  ]
}
```

---

## 🔧 A full example with the new features

```yaml
ultimate-flame-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: flame.sword

  # Timer: it lives for 60 seconds
  duration: 60
  max-duration-message: '&c🔥 The blade melted!'

  # World restrictions
  allowed-worlds:
    - world
    - world_nether
  disabled-worlds:
    - world_the_end

  # Trail particles
  trail-particles:
    - 'FLAME:8'
    - 'LAVA:3'
  trail-particle-interval: 2

  item:
    type: NETHERITE_SWORD
    title: '&c🔥 Fiery Blade'
    glowing: true
    unbreakable: true
    item-model: "flame_sword"
    item-model-variant: "flame_sword_animated"
    enchantments:
      SHARPNESS: 5
      FIRE_ASPECT: 2

  effects:
    - 'INCREASE_DAMAGE:3'
    - 'FIRE_RESISTANCE:1'

  click-cooldown: 2000

  triggers:
    on_equip:
      - 'particles_custom:FLAME:50:0:1:0:2:2:2'
      - 'sound:ENTITY_BLAZE_AMBIENT:1:1'
      - 'title:&c🔥 Fiery Blade drawn!|||10:40:10'

    on_sprint:
      - 'trail-particles:FLAME:3'
      - 'effect:SPEED:3:1'

    on_kill:
      - 'heal:20'
      - 'effect:REGENERATION:10:2'
      - 'particles_custom:HEART:30:0:2:0:1:1:1'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:2'

    on_damage_dealt:
      - 'particles_custom:CRIT:15:0:1:0:1:1:1'
      - 'sound:ENTITY_BLAZE_SHOOT:0.5:2'
```
