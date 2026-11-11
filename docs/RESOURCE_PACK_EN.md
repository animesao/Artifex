# Resource Pack Guide

## 📚 Complete Reference

This guide covers everything about creating and using resource packs with Artifex.

---

## 🎯 Overview

Resource packs allow you to add custom models and textures to your items. This makes your custom items look unique and professional.

---

## 🚀 Quick Start

### Step 1: Create Resource Pack Folder

```
my-resource-pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       └── my_model.json
        └── textures/
            └── item/
                └── my_model.png
```

### Step 2: Create pack.mcmeta

```json
{
  "pack": {
    "description": "My Custom Items",
    "pack_format": 34
  }
}
```

### Step 3: Create Model File

Create `my_model.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_model"
  }
}
```

### Step 4: Add Texture

Create `my_model.png` (16x16 or 32x32 pixels).

### Step 5: Use in Item Config

```yaml
my_item:
  item:
    item-model: "my_model"
```

### Step 6: Reload Plugin

```
/ci reload
```

---

## 📁 Folder Structure

### Basic Structure

```
resource-pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       ├── sword_model.json
        │       ├── helmet_model.json
        │       └── potion_model.json
        └── textures/
            └── item/
                ├── sword_model.png
                ├── helmet_model.png
                └── potion_model.png
```

### Advanced Structure

```
resource-pack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   ├── models/
    │   │   └── item/
    │   │       └── my_sword.json
    │   └── textures/
    │       └── item/
    │           └── my_sword.png
    └── mynamespace/
        ├── models/
        │   └── item/
        │       └── custom_sword.json
        └── textures/
            └── item/
                └── custom_sword.png
```

---

## 📝 pack.mcmeta

### Basic pack.mcmeta

```json
{
  "pack": {
    "description": "My Custom Items Resource Pack",
    "pack_format": 34
  }
}
```

### Pack Formats

| Version | Format |
|---------|--------|
| 1.20.5+ | 34 |
| 1.20.3-1.20.4 | 22 |
| 1.20-1.20.2 | 15 |
| 1.19.4 | 13 |
| 1.19-1.19.3 | 9 |
| 1.18.2 | 8 |
| 1.18-1.18.1 | 7 |
| 1.17-1.17.1 | 7 |
| 1.16.2-1.16.5 | 6 |
| 1.16-1.16.1 | 6 |
| 1.15-1.15.2 | 5 |
| 1.14-1.14.4 | 4 |
| 1.13-1.13.2 | 4 |
| 1.12-1.12.2 | 3 |

---

## 🎨 Model Files

### Handheld Items (Swords, Tools)

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  }
}
```

### Generated Items (Food, Potions)

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_potion"
  }
}
```

### Armor Items

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_helmet"
  }
}
```

### With Custom Model Data

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  },
  "overrides": [
    {
      "predicate": {
        "custom_model_data": 1001
      },
      "model": "minecraft:item/my_sword_model"
    }
  ]
}
```

### With Multiple Models

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  },
  "overrides": [
    {
      "predicate": {
        "custom_model_data": 1001
      },
      "model": "minecraft:item/my_sword_1"
    },
    {
      "predicate": {
        "custom_model_data": 1002
      },
      "model": "minecraft:item/my_sword_2"
    }
  ]
}
```

---

## 🖼️ Textures

### Texture Sizes

| Size | Use Case |
|------|----------|
| 16x16 | Default Minecraft |
| 32x32 | Higher quality |
| 64x64 | Very high quality |
| 128x128 | Ultra quality (not recommended) |

### Texture Format

- Use PNG format
- Transparent backgrounds work
- Use alpha channel for transparency

### Creating Textures

1. **Use an image editor** (GIMP, Photoshop, Paint.NET)
2. **Create 16x16 or 32x32 pixel image**
3. **Save as PNG**
4. **Name it same as model** (e.g., `my_sword.png`)

---

## 🔗 Connecting Models to Items

### In YAML Config

```yaml
my_sword:
  item:
    type: NETHERITE_SWORD
    item-model: "my_sword"
```

### In Java API

```java
@Override
public String getItemModel() { return "my_sword"; }
```

### Model Resolution

The plugin looks for models in this order:

1. `assets/minecraft/models/item/{model_name}.json`
2. `assets/{namespace}/models/item/{model_name}.json`

---

## 📋 Complete Examples

### Example 1: Custom Sword

**Model (my_sword.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  }
}
```

**Config:**
```yaml
my_sword:
  item:
    type: NETHERITE_SWORD
    title: '&6My Custom Sword'
    item-model: "my_sword"
```

**Java:**
```java
@Override
public String getItemModel() { return "my_sword"; }
```

### Example 2: Custom Helmet

**Model (my_helmet.json):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_helmet"
  }
}
```

**Config:**
```yaml
my_helmet:
  item:
    type: DIAMOND_HELMET
    title: '&bMy Custom Helmet'
    item-model: "my_helmet"
```

### Example 3: Custom Potion

**Model (my_potion.json):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_potion"
  }
}
```

**Config:**
```yaml
my_potion:
  item:
    type: EXPERIENCE_BOTTLE
    title: '&aMy Custom Potion'
    item-model: "my_potion"
```

---

## 🎯 Namespaced Models

### Using Custom Namespace

**Model (myplugin/models/item/custom_sword.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "myplugin:item/custom_sword"
  }
}
```

**Config:**
```yaml
my_sword:
  item:
    type: NETHERITE_SWORD
    item-model: "myplugin:item/custom_sword"
```

**Java:**
```java
@Override
public String getItemModel() { return "myplugin:item/custom_sword"; }
```

---

## 🔧 Troubleshooting

### Model Not Appearing

**Problem:** Item shows as default texture

**Solutions:**
1. Check model file name matches `item-model` value
2. Verify pack.mcmeta format is correct
3. Make sure texture PNG exists
4. Check pack format matches server version

### Texture Not Loading

**Problem:** Purple/black missing texture

**Solutions:**
1. Verify PNG file exists
2. Check file name matches model reference
3. Make sure image is valid PNG
4. Check for typos in file paths

### Resource Pack Not Applied

**Problem:** Server doesn't load resource pack

**Solutions:**
1. Check `server.properties` for resource pack settings
2. Verify pack URL is accessible
3. Check SHA1 hash if using URL
4. Test with local pack first

---

## 💡 Tips

### Tip 1: Start Simple

Begin with basic models before complex ones:
1. Use `minecraft:item/handheld` or `minecraft:item/generated`
2. Add textures
3. Test
4. Add complexity

### Tip 2: Use Consistent Naming

Use the same name for model and texture:
- Model: `my_sword.json`
- Texture: `my_sword.png`

### Tip 3: Test Often

Reload plugin and test after each change:
```
/ci reload
```

### Tip 4: Use Tools

Use tools like Blockbench for creating models:
- https://www.blockbench.net/

---

## 📚 Related Documentation

- [Getting Started](GETTING_STARTED_EN.md)
- [YAML Items Guide](YAML_ITEMS_EN.md)
- [Java API Guide](JAVA_API_EN.md)
- [Commands Reference](COMMANDS_EN.md)

---

**Next:** [Commands Reference](COMMANDS_EN.md) →
