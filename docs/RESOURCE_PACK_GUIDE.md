# 🎨 Artifex — Руководство по Resource Pack

## Описание

Artifex поддерживает три способа кастомизации внешнего вида предметов:

1. **`item-model`** — имя модели (1.21.4+, Unicode textures)
2. **`custom-model-data`** — числовая модель (legacy, работает во всех версиях)
3. **`item-model-variant`** — анимированная/альтернативная модель (1.21.4+)

---

## 1. item-model (Рекомендуется для 1.21.4+)

### YAML-конфиг

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    item-model: 'artifex:flame_sword'
```

### Структура Resource Pack

```
assets/artifex/
└── items/
    └── flame_sword.json
```

### Пример flame_sword.json

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "artifex:item/flame_sword"
  }
}
```

### Текстура

```
assets/artifex/textures/item/flame_sword.png
```

---

## 2. custom-model-data (Legacy)

### YAML-конфиг

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    custom-model-data: 1001
```

### Структура Resource Pack

```
assets/minecraft/
└── items/
    └── netherite_sword.json
```

### Пример netherite_sword.json

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "minecraft:item/netherite_sword"
  },
  "overrides": [
    { "predicate": { "custom_model_data": 1001 }, "model": "artifex:item/flame_sword" }
  ]
}
```

---

## 3. item-model-variant (Анимированные текстуры)

### YAML-конфиг

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    item-model: 'artifex:flame_sword'
    item-model-variant: 'flame_sword_animated'
```

### Структура Resource Pack

```
assets/artifex/
├── items/
│   ├── flame_sword.json
│   └── flame_sword_animated.json
└── textures/
    └── item/
        ├── flame_sword.png          # Статичная текстура
        └── flame_sword_animated.png  # Спрайтшит анимации
```

### Анимированная текстура

Текстура должна быть **спрайтшитом**: все кадры анимации расположены горизонтально в одном файле.

```
Кадр1 | Кадр2 | Кадр3 | Кадр4 | Кадр5 | Кадр6
```

### Метаданные анимации (.mcmeta)

Создайте файл `flame_sword_animated.png.mcmeta` рядом с текстурой:

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

| Поле | Описание |
|------|----------|
| `interpolate` | Сглаживание между кадрами (`true`/`false`) |
| `frametime` | Время показа каждого кадра (в тиках, 20 = 1 сек) |
| `frames` | Порядок кадров (нумерация с 0) |

### Примеры анимаций

**Быстрое пламя (60 FPS):**
```json
{
  "animation": {
    "interpolate": false,
    "frametime": 1,
    "frames": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
  }
}
```

**Медленное свечение (аниме-стиль):**
```json
{
  "animation": {
    "interpolate": true,
    "frametime": 4,
    "frames": [0, 1, 2, 1, 0]
  }
}
```

---

## 4. Ресурспак-пакеты (3D модели, Display Entities)

### YAML-конфиг для 3D-предмета

```yaml
my-3d-item:
  item:
    type: NETHERITE_SWORD
    item-model: 'artifex:3d_sword'
    # Для 3D-моделей через Blockbench
```

### Создание 3D-модели (Blockbench)

1. Откройте [Blockbench](https://www.blockbench.net/)
2. Создайте новый проект: **Java Block/Item**
3. Нарисуйте модель
4. Экспортируйте в формат **Java Block/Item**
5. Поместите файлы в Resource Pack

### Структура

```
assets/artifex/
├── items/
│   └── 3d_sword.json
├── models/
│   └── item/
│       └── 3d_sword.json       # Детальная модель
└── textures/
    └── item/
        └── 3d_sword.png        # Текстура модели
```

### Пример модели 3d_sword.json

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "artifex:item/3d_sword",
    "particle": "artifex:item/3d_sword"
  },
  "elements": [
    {
      "from": [7, 0, 8],
      "to": [9, 16, 8],
      "faces": {
        "north": { "uv": [0, 0, 2, 16], "texture": "#0" },
        "south": { "uv": [0, 0, 2, 16], "texture": "#0" },
        "east":  { "uv": [0, 0, 0, 16], "texture": "#0" },
        "west":  { "uv": [0, 0, 0, 16], "texture": "#0" }
      }
    }
  ]
}
```

---

## 5. Полный пример Resource Pack

### Структура пакета

```
artifex-resourcepack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   └── items/
    │       └── netherite_sword.json    # Legacy overrides
    └── artifex/
        ├── items/
        │   ├── flame_sword.json
        │   ├── flame_sword_animated.json
        │   └── 3d_sword.json
        └── textures/
            └── item/
                ├── flame_sword.png
                ├── flame_sword_animated.png
                ├── flame_sword_animated.png.mcmeta
                └── 3d_sword.png
```

### pack.mcmeta

```json
{
  "pack": {
    "pack_format": 46,
    "description": "Artifex Resource Pack"
  }
}
```

| pack_format | Minecraft Version |
|-------------|-------------------|
| 34 | 1.20.2 |
| 41 | 1.20.3-1.20.4 |
| 46 | 1.21+ |

---

## 6. Примеры в YAML

### Анимированный клинок

```yaml
animated-sword:
  type: TOOL
  activation-slot: HAND
  item:
    type: NETHERITE_SWORD
    title: '&c🔥 Анимированный Клинок'
    item-model: 'artifex:animated_sword'
    item-model-variant: 'animated_sword_glow'
```

### Legacy custom-model-data

```yaml
legacy-sword:
  type: TOOL
  activation-slot: HAND
  item:
    type: NETHERITE_SWORD
    title: '&6Кастомный Меч'
    custom-model-data: 1001
```

### 3D-модель

```yaml
3d-axe:
  type: TOOL
  activation-slot: HAND
  item:
    type: NETHERITE_AXE
    title: '&43D Топор'
    item-model: 'artifex:3d_axe'
```

---

## 7. Советы

1. **Используйте `item-model` вместо `custom-model-data`** — это современный способ для 1.21.4+
2. **Анимации через `.mcmeta`** — создавайте отдельные файлы для каждой анимации
3. **Для 3D используйте Blockbench** — это самый простой инструмент
4. **Тестирование** — используйте `/give` для быстрой проверки
5. **Оптимизация** — не создавайте слишком много текстур, используйте sprite sheets

---

## Ссылки

- [Minecraft Wiki — Resource Pack](https://minecraft.wiki/w/Resource_pack)
- [Blockbench](https://www.blockbench.net/)
- [Minecraft Model Creator](https://www.minecraftmodels.com/)
- [Vanilla Tweaks](https://vanillatweaks.net/)
