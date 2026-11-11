# Getting Started with Artifex

## 🎯 Welcome!

This guide will help you create your first custom item in just 5 minutes!

---

## 📋 Prerequisites

Before we start, make sure you have:

- ✅ Minecraft server running (1.20+)
- ✅ Plugin installed in `plugins/` folder
- ✅ Server restarted or `/ci reload` executed
- ✅ OP permissions or `customitems.give` permission

---

## 🚀 Step 1: Create Your First Item

### Navigate to Items Folder

Open your server folder and go to:
```
plugins/Artifex/items/
```

### Create a New File

Create a file called `hello-sword.yml` with this content:

```yaml
hello_sword:
  item:
    type: DIAMOND_SWORD
    title: '&aHello Sword'
    glowing: true
  lore:
    - ''
    - '&7This is my first custom item!'
    - ''
  type: TOOL
  activation-slot: HAND
  trigger-actions:
    - 'on_click_right:message:&aHello from my sword!'
    - 'on_click_right:particle:HEART:20'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
```

### Save and Reload

1. Save the file
2. Go to your Minecraft server console or in-game
3. Type: `/ci reload`

You should see:
```
[Artifex] Loaded 1 custom items
```

---

## 🎮 Step 2: Get Your Item

Type this command:
```
/give hello_sword
```

### Test It!

1. Hold the sword in your hand
2. Right-click (PKM)
3. You should see:
   - Message: "Hello from my sword!"
   - Heart particles
   - Level up sound

---

## 🔧 Step 3: Customize Your Item

Let's make it more interesting!

### Add Effects

```yaml
hello_sword:
  item:
    type: DIAMOND_SWORD
    title: '&6Powerful Sword'
    glowing: true
    unbreakable: true
  lore:
    - ''
    - '&7Right-click for speed boost!'
    - ''
  type: TOOL
  activation-slot: HAND
  click-cooldown: 3000
  trigger-actions:
    - 'on_click_right:effect:SPEED:10:2'
    - 'on_click_right:effect:STRENGTH:10:1'
    - 'on_click_right:particle:FLAME:50'
    - 'on_click_right:sound:ENTITY_BLAZE_SHOOT:1:1'
    - 'on_click_right:message:&6Power activated!'
    - 'on_click_right:title:&6&lPOWER!::&7Speed and Strength for 10 seconds!'
```

### Reload and Test

```
/ci reload
/give hello_sword
```

Now right-click and you'll get speed and strength!

---

## 📝 Understanding the Config

Let's break down what each part means:

### Item Properties

```yaml
item:
  type: DIAMOND_SWORD     # What material to use
  title: '&6Name'         # Display name (& = color code)
  glowing: true           # Enchantment glow effect
  unbreakable: true       # Item can't be broken
```

### Lore (Description)

```yaml
lore:
  - ''                   # Empty line
  - '&7Description'      # Gray text
  - '&eMore info'        # Yellow text
```

### Actions

```yaml
trigger-actions:
  - 'on_event:action'    # Format: event:action
```

---

## 🎨 Common Actions

### Messages

```yaml
- 'on_click_right:message:&aHello!'
```

### Effects

```yaml
- 'on_click_right:effect:SPEED:10:2'
# effect:TYPE:DURATION_SECONDS:LEVEL
```

### Particles

```yaml
- 'on_click_right:particle:FLAME:50'
# particle:TYPE:COUNT
```

### Sounds

```yaml
- 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
# sound:TYPE:VOLUME:PITCH
```

### Healing

```yaml
- 'on_click_right:heal:10'
# heal:AMOUNT (in half-hearts)
```

### Damage

```yaml
- 'on_click_right:damage:5'
# damage:AMOUNT
```

### Teleport

```yaml
- 'on_click_right:teleport:100:64:200'
# teleport:X:Y:Z
```

### Titles

```yaml
- 'on_click_right:title:Title Text:Subtitle Text:10:40:10'
# title:TITLE:SUBTITLE:FADE_IN:STAY:FADE_OUT
```

---

## 📚 Next Steps

Now that you've created your first item, here's what to explore:

1. **[YAML Items Guide](YAML_ITEMS_EN.md)** - Complete YAML reference
2. **[Java API Guide](JAVA_API_EN.md)** - Create items with Java code
3. **[Resource Pack Guide](RESOURCE_PACK_EN.md)** - Custom models and textures
4. **[Commands Reference](COMMANDS_EN.md)** - All available commands

---

## 💡 Tips for Beginners

### Tip 1: Start Simple

Don't try to create complex items right away. Start with:
- A sword with a message
- Then add particles
- Then add effects
- Build from there

### Tip 2: Use Comments

Add comments to remember what each action does:

```yaml
my_sword:
  # Basic properties
  item:
    type: DIAMOND_SWORD
  
  # When right-clicked
  trigger-actions:
    - 'on_click_right:message:Hello!'  # Show message
    - 'on_click_right:effect:SPEED:5:1'  # Speed boost
```

### Tip 3: Test Often

After each change:
1. Save the file
2. Run `/ci reload`
3. Test the item

### Tip 4: Check Console

If something doesn't work, check the server console for errors!

---

## 🎉 Congratulations!

You've learned the basics of Artifex!

### What You Can Do Now:

- ✅ Create custom items with YAML
- ✅ Add effects, particles, and sounds
- ✅ Set cooldowns and restrictions
- ✅ Give items to players

### What's Next:

- Learn about [Java API](JAVA_API_EN.md) for advanced items
- Create [resource packs](RESOURCE_PACK_EN.md) for custom models
- Explore all [commands](COMMANDS_EN.md) and features

---

## ❓ Need Help?

- Read the [Troubleshooting Guide](TROUBLESHOOTING_EN.md)
- Check the [Full Documentation](README_EN.md)
- Join our community

---

**Next:** [YAML Items Guide](YAML_ITEMS_EN.md) →
