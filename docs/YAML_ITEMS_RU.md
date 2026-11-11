# Руководство по YAML Конфигурации Предметов

## 📚 Полный Справочник

Это руководство описывает всё что нужно знать о конфигурации предметов через YAML файлы.

---

## 📁 Структура Файлов

### Куда Размещать Файлы

```
plugins/Artifex/
├── items/
│   ├── my-sword.yml
│   ├── my-armor.yml
│   └── my-potion.yml
├── config.yml
└── messages.yml
```

### Именование Файлов

- Используйте строчные буквы через дефис: `my-sword.yml`
- Избегайте пробелов и специальных символов
- Рекомендуется один предмет на файл

---

## 🎯 Базовая Структура Предмета

```yaml
item_id:
  item:
    type: MATERIAL
    title: '&6Название'
    glowing: true
    unbreakable: true
  lore:
    - ''
    - '&7Описание'
    - ''
  type: TOOL
  activation-slot: HAND
  trigger-actions:
    - 'on_event:action'
```

---

## ⚔️ Свойства Предмета

### Типы Материалов

| Категория | Материалы |
|-----------|-----------|
| **Мечи** | WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD |
| **Топоры** | WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE |
| **Кирки** | WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE |
| **Лопаты** | WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL |
| **Луки** | BOW, CROSSBOW |
| **Броня** | LEATHER_HELMET, CHAINMAIL_HELMET, IRON_HELMET, DIAMOND_HELMET, NETHERITE_HELMET |
| **Инструменты** | FLINT_AND_STEEL, FISHING_ROD, SHEARS, TRIDENT |
| **Еда** | APPLE, GOLDEN_APPLE, BREAD, COOKED_BEEF, CARROT, GOLDEN_CARROT |
| **Зелья** | POTION, SPLASH_POTION, LINGERING_POTION |
| **Особые** | TOTEM_OF_UNDYING, ENDER_PEARL, ELYTRA, SHIELD |

### Коды Цветов

| Код | Цвет | Код | Цвет |
|-----|------|-----|------|
| `&0` | Чёрный | `&8` | Тёмно-Серый |
| `&1` | Тёмно-Синий | `&9` | Синий |
| `&2` | Тёмно-Зелёный | `&a` | Зелёный |
| `&3` | Тёмно-Бирюзовый | `&b` | Бирюзовый |
| `&4` | Тёмно-Красный | `&c` | Красный |
| `&5` | Тёмно-Фиолетовый | `&d` | Светло-Фиолетовый |
| `&6` | Золотой | `&e` | Жёлтый |
| `&7` | Серый | `&f` | Белый |

### Коды Форматирования

| Код | Эффект |
|-----|--------|
| `&l` | Жирный |
| `&n` | Подчёркнутый |
| `&o` | Курсив |
| `&m` | Зачёркнутый |
| `&k` | Обфусцированный |
| `&r` | Сброс |

---

## 📝 Полный Пример

```yaml
legendary_sword:
  item:
    type: NETHERITE_SWORD
    title: '&6&lЛегендарный Меч'
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
    - '&7Меч легендарной мощи'
    - '&7Выкован в огнях Незера'
    - ''
    - '&eПКМ для особой способности!'
    - ''
    - '&6Характеристики:'
    - '&c+10 к Урону'
    - '&b+5% Критического Удара'
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
    - 'on_click_right:message:&6Легендарная сила активирована!'
    - 'on_click_right:title:&6&lЛЕГЕНДАРНАЯ МОЩЬ!::&7Сила и Скорость на 10 секунд!'
    - 'on_equip:message:&6Легендарный Меч экипирован!'
    - 'on_unequip:message:&6Легендарный Меч снят.'
    - 'on_damage_dealt:damage:5'
    - 'on_damage_taken:effect:DAMAGE_RESISTANCE:5:1'
    - 'on_kill:heal:10'
    - 'on_kill:effect:REGENERATION:5:2'
    - 'on_block_break:particle:BLOCK_BREAK:20'
```

---

## 🎯 Типы Событий

### События Кликов

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_click_left` | Левый клик (ЛКМ) | Атака предметом |
| `on_click_right` | Правый клик (ПКМ) | Использование способности |

### События Экипировки

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_equip` | Предмет экипирован | Надеть броню |
| `on_unequip` | Предмет снят | Снять броню |
| `on_swap_hand` | Смена рук (F) | Переключить руки |

### Боевые События

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_damage_dealt` | Нанесение урона | Ударить моба |
| `on_damage_taken` | Получение урона | Получить удар |
| `on_kill` | Убийство моба | Убить моба/игрока |
| `on_death` | Смерть игрока | Умереть с предметом |

### События Движения

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_jump` | Прыжок игрока | Прыгнуть с предметом |
| `on_move` | Движение игрока | Идти с предметом |

### События Блоков

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_block_break` | Ломание блока | Копать киркой |
| `on_block_place` | Установка блока | Строить |

### События Предметов

| Событие | Описание | Пример |
|---------|----------|--------|
| `on_drop` | Выбрасывание | Бросить предмет |
| `on_pickup` | Подбор | Подобрать предмет |

---

## 🔧 Типы Действий

### Действия Сообщений

```yaml
- 'on_click_right:message:Привет Мир!'
- 'on_click_right:message:&aЗелёное сообщение!'
- 'on_click_right:message:&cКрасное &lжирное сообщение!'
```

### Действия Эффектов (Эффекты Зелий)

```yaml
# Формат: effect:ТИП:ДЛИТЕЛЬНОСТЬ_СЕК:УРОВЕНЬ
- 'on_click_right:effect:SPEED:10:2'
- 'on_click_right:effect:STRENGTH:30:1'
- 'on_click_right:effect:REGENERATION:5:3'
```

**Доступные Эффекты:**

| Эффект | Описание |
|--------|----------|
| SPEED | Скорость передвижения |
| SLOWNESS | Замедление |
| HASTE | Скорость копания |
| MINING_FATIGUE | Усталость от копания |
| STRENGTH | Урон атаки |
| JUMP_BOOST | Высота прыжка |
| NAUSEA | Качание экрана |
| REGENERATION | Восстановление здоровья |
| RESISTANCE | Снижение урона |
| FIRE_RESISTANCE | Неуязвимость к огню |
| WATER_BREATHING | Дыхание под водой |
| INVISIBILITY | Невидимость |
| NIGHT_VISION | Ночное зрение |
| WEAKNESS | Слабость |
| POISON | Отравление |
| WITHER | Иссушение |
| HEALTH_BOOST | Дополнительные сердца |
| ABSORPTION | Жёлтые сердца |
| SATURATION | Насыщение |
| GLOWING | Светящийся |
| LUCK | Удача |
| UNLUCK | Неудача |
| DOLPHINS_GRACE | Скорость плавания |
| CONDUIT_POWER | Подводная сила |
| SLOW_FALLING | Медленное падение |
| BAD_OMEN | Дурное предзнаменование |
| HERO_OF_THE_VILLAGE | Герой деревни |

### Действия Частиц

```yaml
# Формат: particle:ТИП:КОЛИЧЕСТВО
- 'on_click_right:particle:FLAME:50'
- 'on_click_right:particle:HEART:20'
- 'on_click_right:particle:CRIT:30'
```

**Доступные Частицы:**

| Частица | Описание |
|---------|----------|
| FLAME | Огненные частицы |
| SOUL_FIRE_FLAME | Синий огонь |
| HEART | Красные сердечки |
| CRIT | Критический удар |
| CRIT_MAGIC | Зачарованный крит |
| SPELL | Магические частицы |
| INSTANT_SPELL | Мгновенная магия |
| MOB_SPELL | Магия мобов |
| NOTE | Музыкальная нота |
| PORTAL | Портал Незера |
| ENCHANTMENT_TABLE | Зачарование |
| EXPLOSION_NORMAL | Малый взрыв |
| EXPLOSION_HUGE | Большой взрыв |
| FIREWORKS_SPARK | Фейерверк |
| WATER_SPLASH | Водяная вспышка |
| WATER_WAKE | Водяная рябь |
| SUSPENDED_DEPTH | Подводные |
| CLOUD | Облако |
| REDSTONE | Редстоун пыль |
| SNOWBALL | Снежок |
| SNOW_SHOVEL | Снег |
| SLIME | Слайм |
| BARRIER | Барьер |
| ITEM_CRACK | Разрушение предмета |
| BLOCK_CRACK | Разрушение блока |
| BLOCK_DUST | Пыль блока |
| WATER_DROP | Капля воды |
| ITEM_TAKE | Подбор предмета |
| MOB_APPEARANCE | Страж |

### Действия Звуков

```yaml
# Формат: sound:ТИП:ГРОМКОСТЬ:ТОН
- 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
- 'on_click_right:sound:ENTITY_BLAZE_SHOOT:2:0.5'
```

**Популярные Звуки:**

| Звук | Описание |
|------|----------|
| ENTITY_PLAYER_LEVELUP | Повышение уровня |
| ENTITY_PLAYER_BURP | Отрыжка |
| ENTITY_PLAYER_DEATH | Смерть |
| ENTITY_BLAZE_SHOOT | Выстрел огнера |
| ENTITY_CREEPER_HISS | Крипер |
| ENTITY_ENDERMAN_TELEPORT | Телепорт Эндмена |
| ENTITY_EXPLODE | Взрыв |
| ENTITY_LIGHTNING_THUNDER | Молния |
| BLOCK_GLASS_BREAK | Разбитие стекла |
| BLOCK_STONE_BREAK | Разбитие камня |
| ITEM_ARMOR_EQUIP_DIAMOND | Экипировка алмаза |
| ITEM_ARMOR_EQUIP_NETHERITE | Экипировка незерита |

### Действия Лечения

```yaml
# Формат: heal:КОЛИЧЕСТВО (в полухочках)
- 'on_click_right:heal:10'  # +5 сердец
- 'on_kill:heal:20'  # Полное лечение
```

### Действия Урона

```yaml
# Формат: damage:КОЛИЧЕСТВО
- 'on_click_right:damage:5'
- 'on_damage_dealt:damage:10'
```

### Действия Телепортации

```yaml
# Формат: teleport:X:Y:Z
- 'on_click_right:teleport:100:64:200'
```

### Действия Заголовков

```yaml
# Формат: title:ЗАГОЛОВОК:ПОДЗАГОЛОВОК:ПОЯВЛЕНИЕ:ДЕРЖАНИЕ:ИСЧЕЗНОВЕНИЕ
- 'on_click_right:title:МОЩЬ!::Активирована:10:40:10'
- 'on_click_right:title:&6&lМОЩЬ!::&7Способность активирована!:10:60:20'
```

### Действия Выдачи/Удаления

```yaml
# Формат: give:МАТЕРИАЛ:КОЛИЧЕСТВО
- 'on_kill:give:DIAMOND:1'

# Формат: remove:МАТЕРИАЛ:КОЛИЧЕСТВО
- 'on_click_right:remove:DIAMOND:1'
```

---

## 🎭 Типы Предметов

| Тип | Описание | Пример |
|-----|----------|--------|
| `TOOL` | Инструменты (мечи, кирки) | Оружие, инструменты |
| `ARMOR` | Носимые предметы | Шлемы, нагрудники |
| `RUNE` | Особые предметы | Телепорт, способности |
| `CONSUMABLE` | Расходуемые предметы | Зелья, еда |
| `PLACEABLE` | Можно разместить | Блоки, декорации |

---

## 🎯 Слоты Активации

| Слот | Описание | Когда Активен |
|------|----------|---------------|
| `HAND` | Основная рука | В правой руке |
| `OFFHAND` | Дополнительная рука | В левой руке |
| `HEAD` | Слот шлема | На голове |
| `CHEST` | Слот нагрудника | На груди |
| `LEGS` | Слот поножей | На ногах |
| `FEET` | Слот ботинок | На ступнях |

---

## 📊 Полные Примеры

### Огненный Меч

```yaml
fire_sword:
  item:
    type: DIAMOND_SWORD
    title: '&c&lОгненный Меч'
    glowing: true
    unbreakable: true
  lore:
    - ''
    - '&7Поджигает врагов при ударе'
    - '&7ПКМ для огненной ауры'
    - ''
  type: TOOL
  activation-slot: HAND
  click-cooldown: 3000
  trigger-actions:
    - 'on_damage_dealt:fire:100'
    - 'on_click_right:particle:FLAME:100'
    - 'on_click_right:effect:FIRE_RESISTANCE:30:0'
    - 'on_click_right:message:&cОгненная аура активирована!'
```

### Зелье Лечения

```yaml
healing_potion:
  item:
    type: EXPERIENCE_BOTTLE
    title: '&a&lЗелье Лечения'
    glowing: true
  lore:
    - ''
    - '&7Восстанавливает 10 сердец'
    - '&7Даёт регенерацию'
    - ''
  type: CONSUMABLE
  activation-slot: HAND
  trigger-actions:
    - 'on_click_right:heal:20'
    - 'on_click_right:effect:REGENERATION:10:2'
    - 'on_click_right:particle:HEART:50'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
    - 'on_click_right:message:&aЗдоровье восстановлено!'
    - 'on_click_right:remove:SELF:1'
```

### Защитный Шлем

```yaml
protective_helmet:
  item:
    type: DIAMOND_HELMET
    title: '&b&lЗащитный Шлем'
    glowing: true
    unbreakable: true
    enchantments:
      PROTECTION: 4
      UNBREAKING: 3
  lore:
    - ''
    - '&7Даёт ночное зрение'
    - '&7Восстанавливает здоровье'
    - ''
  type: ARMOR
  activation-slot: HEAD
  trigger-actions:
    - 'on_equip:effect:NIGHT_VISION:999999:0'
    - 'on_equip:effect:REGENERATION:999999:0'
    - 'on_equip:message:&bШлем экипирован!'
    - 'on_unequip:message:&bШлем снят.'
    - 'on_unequip:clear_effects'
```

---

## 💡 Советы

### Совет 1: Используйте Комментарии

Добавляйте комментарии чтобы объяснить что делает каждое действие:

```yaml
my_item:
  trigger-actions:
    # При ПКМ
    - 'on_click_right:message:Привет!'  # Показать приветствие
    - 'on_click_right:effect:SPEED:5:1'  # Ускорение
```

### Совет 2: Организуйте по Событиям

Группируйте действия по типу события для ясности:

```yaml
my_item:
  trigger-actions:
    # Действия кликов
    - 'on_click_right:message:Использовано!'
    - 'on_click_right:effect:SPEED:5:1'
    
    # Действия экипировки
    - 'on_equip:message:Экипировано!'
    - 'on_unequip:message:Снято.'
    
    # Боевые действия
    - 'on_damage_dealt:damage:5'
    - 'on_kill:heal:10'
```

### Совет 3: Тестируйте Каждое Действие

Тестируйте предметы после добавления каждого действия чтобы ловить ошибки на раннем этапе.

### Совет 4: Используйте Кулдауны

Предотвращайте спам с помощью кулдаунов:

```yaml
my_item:
  click-cooldown: 3000  # 3 секунды
```

---

## 📚 Связанная Документация

- [Начало Работы](GETTING_STARTED_RU.md)
- [Справочник Команд](COMMANDS_RU.md)
- [Руководство по Java API](JAVA_API_RU.md)
- [Руководство по Ресурс-Паку](RESOURCE_PACK_RU.md)

---

**Далее:** [Справочник Команд](COMMANDS_RU.md) →
