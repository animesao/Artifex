# Руководство по Ресурс-Паку

## 📚 Полный Справочник

Это руководство описывает всё о создании и использовании ресурс-паков с Artifex.

---

## 🎯 Обзор

Ресурс-паки позволяют добавлять кастомные модели и текстуры к вашим предметам. Это делает ваши кастомные предметы уникальными и профессиональными.

---

## 🚀 Быстрый Старт

### Шаг 1: Создайте Папку Ресурс-Пака

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

### Шаг 2: Создайте pack.mcmeta

```json
{
  "pack": {
    "description": "Мои Кастомные Предметы",
    "pack_format": 34
  }
}
```

### Шаг 3: Создайте Файл Модели

Создайте `my_model.json`:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_model"
  }
}
```

### Шаг 4: Добавьте Текстуру

Создайте `my_model.png` (16x16 или 32x32 пикселей).

### Шаг 5: Используйте в Конфиге Предмета

```yaml
my_item:
  item:
    item-model: "my_model"
```

### Шаг 6: Перезагрузите Плагин

```
/ci reload
```

---

## 📁 Структура Папок

### Базовая Структура

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

### Продвинутая Структура

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

### Базовый pack.mcmeta

```json
{
  "pack": {
    "description": "Ресурс-Пак Моих Кастомных Предметов",
    "pack_format": 34
  }
}
```

### Форматы Паков

| Версия | Формат |
|--------|--------|
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

## 🎨 Файлы Моделей

### Рукодержащиеся Предметы (Мечи, Инструменты)

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  }
}
```

### Генерируемые Предметы (Еда, Зелья)

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_potion"
  }
}
```

### Предметы Брони

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_helmet"
  }
}
```

### С Кастомными Данными Модели

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

### С Несколькими Моделями

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

## 🖼️ Текстуры

### Размеры Текстур

| Размер | Использование |
|--------|---------------|
| 16x16 | Стандартный Minecraft |
| 32x32 | Повышенное качество |
| 64x64 | Очень высокое качество |
| 128x128 | Ультра качество (не рекомендуется) |

### Формат Текстур

- Используйте формат PNG
- Прозрачные фоны работают
- Используйте альфа-канал для прозрачности

### Создание Текстур

1. **Используйте редактор изображений** (GIMP, Photoshop, Paint.NET)
2. **Создайте изображение 16x16 или 32x32 пикселей**
3. **Сохраните как PNG**
4. **Назовите как модель** (например, `my_sword.png`)

---

## 🔗 Связь Моделей с Предметами

### В YAML Конфиге

```yaml
my_sword:
  item:
    type: NETHERITE_SWORD
    item-model: "my_sword"
```

### В Java API

```java
@Override
public String getItemModel() { return "my_sword"; }
```

### Разрешение Моделей

Плагин ищет модели в таком порядке:

1. `assets/minecraft/models/item/{имя_модели}.json`
2. `assets/{пространство_имён}/models/item/{имя_модели}.json`

---

## 📋 Полные Примеры

### Пример 1: Кастомный Меч

**Модель (my_sword.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_sword"
  }
}
```

**Конфиг:**
```yaml
my_sword:
  item:
    type: NETHERITE_SWORD
    title: '&6Мой Кастомный Меч'
    item-model: "my_sword"
```

**Java:**
```java
@Override
public String getItemModel() { return "my_sword"; }
```

### Пример 2: Кастомный Шлем

**Модель (my_helmet.json):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_helmet"
  }
}
```

**Конфиг:**
```yaml
my_helmet:
  item:
    type: DIAMOND_HELMET
    title: '&bМой Кастомный Шлем'
    item-model: "my_helmet"
```

### Пример 3: Кастомное Зелье

**Модель (my_potion.json):**
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_potion"
  }
}
```

**Конфиг:**
```yaml
my_potion:
  item:
    type: EXPERIENCE_BOTTLE
    title: '&aМой Кастомный Эликсир'
    item-model: "my_potion"
```

---

## 🎯 Модели с Пространством Имён

### Использование Кастомного Пространства Имён

**Модель (myplugin/models/item/custom_sword.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "myplugin:item/custom_sword"
  }
}
```

**Конфиг:**
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

## 🔧 Решение Проблем

### Модель Не Появляется

**Проблема:** Предмет отображает стандартную текстуру

**Решения:**
1. Проверьте что имя файла модели совпадает со значением `item-model`
2. Убедитесь что pack.mcmeta имеет правильный формат
3. Проверьте что файл текстуры существует
4. Проверьте что формат пака соответствует версии сервера

### Текстура Не Загружается

**Проблема:** Фиолетовые/чёрные пропущенные текстуры

**Решения:**
1. Проверьте что файл PNG существует
2. Проверьте имя файла совпадает с ссылкой модели
3. Убедитесь что изображение является валидным PNG
4. Проверьте опечатки в путях файлов

### Ресурс-Пак Не Применяется

**Проблема:** Сервер не загружает ресурс-пак

**Решения:**
1. Проверьте `server.properties` для настроек ресурс-пака
2. Убедитесь что URL пака доступен
3. Проверьте SHA1 хеш если используете URL
4. Сначала протестируйте с локальным паком

---

## 💡 Советы

### Совет 1: Начинайте Просто

Начните с простых моделей перед сложными:
1. Используйте `minecraft:item/handheld` или `minecraft:item/generated`
2. Добавьте текстуры
3. Протестируйте
4. Добавьте сложность

### Совет 2: Используйте Единообразное Именование

Используйте одно имя для модели и текстуры:
- Модель: `my_sword.json`
- Текстура: `my_sword.png`

### Совет 3: Тестируйте Часто

Перезагружайте плагин и тестируйте после каждого изменения:
```
/ci reload
```

### Совет 4: Используйте Инструменты

Используйте инструменты вроде Blockbench для создания моделей:
- https://www.blockbench.net/

---

## 📚 Связанная Документация

- [Начало Работы](GETTING_STARTED_RU.md)
- [Руководство по YAML](YAML_ITEMS_RU.md)
- [Руководство по Java API](JAVA_API_RU.md)
- [Справочник Команд](COMMANDS_RU.md)

---

**Далее:** [Справочник Команд](COMMANDS_RU.md) →
