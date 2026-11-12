<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8%2B-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-2.1.0-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Modules-5-purple?style=for-the-badge" alt="Modules">
  <img src="https://img.shields.io/github/license/animesao/Artifex-purple?style=for-the-badge" alt="License">
</p>

<h1 align="center">⚔️ Artifex</h1>

<p align="center">
  <b>Модульный плагин кастомных предметов и механик для Minecraft 1.21.8+ (Paper/Spigot)</b><br>
  Создавай, настраивай и расширяй кастомные предметы без ограничений!
</p>

<p align="center">
  <a href="#что-такое-artifex">О плагине</a> •
  <a href="#установка">Установка</a> •
  <a href="#быстрый-старт">Быстрый старт</a> •
  <a href="#модульная-архитектура">Модули</a> •
  <a href="#команды">Команды</a> •
  <a href="#java-api">Java API</a> •
  <a href="#документация">Документация</a>
</p>

---

## 📖 Что такое Artifex

**Artifex** — это модульный плагин для Minecraft 1.21.8+, который позволяет создавать кастомные предметы с уникальными механиками.

### Ключевые особенности

| Особенность | Описание |
|-------------|----------|
| 🏗️ **Модульная архитектура** | Каждый компонент — отдельный модуль. Включай/выключай что угодно |
| 📦 **YAML + Java** | Простые предметы через YAML, сложные — через Java API |
| 🎒 **Универсальный /give** | Выдача и кастомных, и ванильных предметов |
| 🔗 **PlaceholderAPI** | 25+ плейсхолдеров для интеграции с другими плагинами |
| 💰 **Vault Economy** | Валюта игроков: баланс и переводы (магазин — отдельные плагины) |
| ☕ **Java API** | Предметы, команды, плейсхолдеры, GUI, uses/duration/миры — компиляция против Paper и любых сторонних jar |
| 🧪 **Крафт-рецепты** | shaped / shapeless / furnace в YAML предмета и через `getRecipes()` в Java API |
| 🛠 **GUI-крафт** | Модуль `customcraft/`: `/craft` собирает рецепты YAML и Java, идентичность по PDC/NBT |
| 🔔 **API-события** | `Equip/Use/Craft/DamageDealt/DamageTaken/Kill/Death/Periodic` — YAML и Java-предметы |
| 🧰 **Чужие библиотеки** | `libs/` в папке плагина + все `plugins/*.jar` — модули компилируются против чего угодно |
| ⚡ **Оптимизация** | Глобальный таск, кэширование enum, стабильные UUID |

---

## 🏗️ Модульная архитектура

Artifex построен по принципу **"ядро + модули"**. Ядро плагина минимально — вся функциональность в модулях.

```
plugins/Artifex/
├── items/                      # Все модули, предметы и образцы
│   ├── EXAMPLE-vault/          # 💰 Модуль экономики (Vault) — образец
│   │   ├── config.yml          # Настройки модуля
│   │   └── vault.java          # Java-класс модуля
│   ├── EXAMPLE-deluxemenux/    # 🎨 Модуль GUI-меню — образец
│   ├── EXAMPLE-shop/           # 🛒 Модуль магазина — образец
│   ├── EXAMPLE-give-command.java  # 🎒 Команда /give — образец
│   ├── EXAMPLE-runes.yml       # Предметы: руны — образец
│   ├── EXAMPLE-armor.yml       # Предметы: броня — образец
│   ├── EXAMPLE-tools.yml       # Предметы: оружие — образец
│   ├── EXAMPLE-totems.yml      # Предметы: тотемы — образец
│   └── messages.java           # Кастомные сообщения
├── config.yml                  # Глобальные настройки
└── data.db                     # База данных (SQLite)
```

> 📦 В jar и репозитории весь контент хранится как образцы с префиксом
> `EXAMPLE-*`: при первом запуске они копируются в `items/`, но **не
> загружаются**. Включить образец = убрать префикс `EXAMPLE-` из имени
> файла/папки (`EXAMPLE-vault/` → `vault/`) и сделать `/ci reload`.

### Как работает модульность

```bash
# Включить модуль из образца: убрать префикс EXAMPLE-
mv items/EXAMPLE-vault/ items/vault/   # ✅ Экономика включена
/ci reload

# Выключить модуль — удалить папку
rm -rf items/vault/                     # ❌ Экономика выключена

# Или отключить в config.yml модуля
enabled: false
```

**Ядро плагина** содержит только:
- `/ci reload` — единственная встроенная команда
- Загрузчик модулей
- База данных

**Всё остальное** — в модулях:
- Команды (`/give`, `/buy`, `/sell`, `/menu`)
- Экономика (Vault)
- GUI (DeluxeMenuX)
- Магазин (Shop)
- Кастомные команды (Java API)

### Создай свой модуль

```bash
# 1. Скопируй шаблон
cp -r items/_template/ items/my-module/

# 2. Переименуй
mv items/my-module/template.java items/my-module/my-module.java

# 3. Настрой config.yml
vim items/my-module/config.yml

# 4. Перезагрузи
/ci reload
```

---

## 📦 Установка

### Быстрая установка

```bash
# Скачать последний релиз
cd ~/server/plugins
curl -L -o artifex.jar https://github.com/animesao/Artifex/releases/download/v2.1.0/Artifex-2.1.0.jar

# Перезапустить сервер
./restart.sh
```

### Ручная установка

1. Скачай `Artifex-2.1.0.jar` из [Releases](https://github.com/animesao/Artifex/releases)
2. Помести в папку `plugins/`
3. Перезапусти сервер
4. Настрой `plugins/Artifex/config.yml`

> 🧩 При установке плагин создаёт **ровно один файл** — `plugins/Artifex/items/messages.java` (все сообщения плагина, их можно менять). Больше ничего не копируется: примеры лежат в репозитории ([`samples/`](samples/)), а не внутри jar. Включи нужный пример: скопируй файл/папку из `samples/` в `items/` и убери префикс `EXAMPLE-` из имени (`EXAMPLE-give-command.java` → `give-command.java`, `EXAMPLE-vault/` → `vault/`), затем `/artifex reload`. Контент отключается удалением файла/папки из `items/`.

> ⚠️ О безопасности Java API: плагин компилирует `.java`-файлы из `items/` прямо
> на сервере — это выполнение произвольного кода. Если доступ к файлам плагина
> может быть у недоверенных лиц, отключи это в `config.yml`:
> `java-compilation: false` (YAML-предметы продолжат работать). Подробнее —
> [SECURITY.md](SECURITY.md).

### Зависимости (опционально)

| Плагин | Зачем | Статус |
|--------|-------|--------|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Плейсхолдеры в scoreboard/tab/chat | Опционально |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) + economy | Покупка/продажа предметов | Опционально |
| [DeluxeMenu](https://www.spigotmc.org/resources/deluxemenu.14736/) | GUI-меню | Опционально |

---

## 🚀 Быстрый старт

### 1. Выдай предмет

```bash
# Кастомный предмет
/give vampire-blade

# Ванильный предмет
/give diamond
/give minecraft:netherite_sword
/give cooked_beef 64
```

### 2. Создай свой предмет

**Способ А (самый простой) — экспортируй предмет прямо из руки:**
Возьми любой настроенный предмет в главную руку в игре и введи:
```bash
/ci save my-sword
# или коротко:
/saveitem my-sword
```
Плагин автоматически считает материал, название, лор (с сохранением цветов и HEX), чары, флаги, атрибуты, custom-model-data, модель 1.21.4+ и текстуру головы, создаст готовый файл `plugins/Artifex/items/my-sword.yml` с шаблонами триггеров и сразу перезагрузит предметы!

**Способ Б — создай файл вручную:**
Создай файл `plugins/Artifex/items/my-sword.yml`:

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/animesao/Artifex/main/schema/items.schema.json
my-sword:
  type: TOOL
  activation-slot: HAND

  item:
    type: DIAMOND_SWORD
    title: '&bМой Меч'
    glowing: true

  effects:
    - 'SPEED:1'
    - 'STRENGTH:2'

  right-click-actions:
    - 'heal:10'
    - 'message:&aВы исцелены на 10 HP!'

  left-click-actions:
    - 'lightning'
    - 'sound:ENTITY_ENDER_DRAGON_GROWL:1:1.5'

  click-cooldown: 3000
  equip-message: '&aМеч экипирован!'
  unequip-message: '&cМеч снят!'
```

### 3. Перезагрузи и используй

```bash
/ci reload
/give my-sword
```

---

## 🎮 Команды

### Основные команды

| Команда | Описание |
|---------|----------|
| `/ci reload` | Перезагрузка плагина и всех модулей |
| `/ci menu` (или `/ci gui`) | Интерактивный GUI-каталог предметов (категории, пагинация, выдача) |
| `/ci save <id> [--force]` | Сохранить предмет из руки в `items/<id>.yml` |
| `/saveitem <id> [--force]` | Быстрый алиас сохранения предмета из руки в конфиг |
| `/give <id\|material>` | Выдать предмет себе |
| `/give <id\|material> <player>` | Выдать игроку |
| `/give <id\|material> <player> <amount>` | Выдать N штук (1-64) |
| `/give <id\|material> all` | Выдать всем онлайн |
| `/give list` | Список кастомных предметов |
| `/give materials` | Список ванильных материалов |
| `/give materials <filter>` | Поиск материалов по фильтру |

### Модульные команды

| Модуль (включить: убрать EXAMPLE-) | Команды | Описание |
|--------|---------|----------|
| `EXAMPLE-vault/` → `vault/` | `/buy <id> [amount]`, `/sell <id> [amount]` | Покупка/продажа |
| `EXAMPLE-deluxemenux/` → `deluxemenux/` | `/menu`, `/shop`, `/kits` | GUI-меню |
| `EXAMPLE-give-command.java` → `give-command.java` | `/give` | Выдача предметов |
| Java API | Любые команды | Создавай свои! |

### Примеры

```bash
# Кастомные предметы
/give vampire-blade
/give test-sword Steve 3

# Ванильные предметы
/give diamond
/give minecraft:netherite_sword
/give cooked_beef 64
/give minecraft:acacia_boat

# Выдать всем
/give diamond all 16
/give health-potion all 5

# Поиск
/give materials
/give materials diamond
/give materials netherite
```

---

## 🎨 YAML-формат предмета

```yaml
my-item:
  type: TOOL                    # RUNE | TOOL | ARMOR | CONSUMABLE
  activation-slot: HAND         # HAND | OFFHAND | HEAD | CHEST | LEGS | FEET
  placeable: false

  item:
    type: DIAMOND_SWORD         # Minecraft материал
    title: '&6Мой Предмет'
    amount: 1
    glowing: true
    unbreakable: true
    item-model: "custom_model"  # Resource Pack model (1.21.8+)
    custom-model-data: 100      # Legacy model data
    texture: "eyJ..."          # Skull texture (PLAYER_HEAD)

    enchantments:
      sharpness: 5
      unbreaking: 3

    item-flags:
      - HIDE_ENCHANTS
      - HIDE_ATTRIBUTES

    attributes:
      # Поддерживаются и legacy-имена (GENERIC_*/PLAYER_*), и современные
      # (attack_damage, max_health), и регистровые ключи.
      GENERIC_ATTACK_DAMAGE: 10
      GENERIC_ATTACK_SPEED: 2

  effects:
    - 'SPEED:2'
    - 'STRENGTH:1'
    - 'REGENERATION:1'

  armor-set: "shadow"           # ID сета
  has-set-bonus: true

  lore:
    - '&7Урон: &c+10'
    - '&7Скорость: &a+20%'
    - '&7Кулдаун: &e%cooldown%с'
    - '&7Использования: &b%uses%'

  left-click-actions:
    - 'lightning'
    - 'sound:ENTITY_ENDER_DRAGON_GROWL:1:1.5'
    - 'particle:FLAME:50'

  right-click-actions:
    - 'heal:10'
    - 'message:&aВы исцелены!'
    - 'effect:REGENERATION:15:1'
    - 'teleport:~0:3:~0'
    - 'title:&6Сpecial!::&7Описание'
    - 'announce:&eИгрок %player% использовал предмет!'
    - 'command:give %player% diamond 1'

  triggers:                     # Канон с 1.328 (trigger-actions удалён)
    on_kill:
      - 'heal:5'
    on_jump:
      - 'particle:FLAME:20'
    on_equip:
      - 'sound:ENTITY_PLAYER_LEVELUP:1:1.5'
    on_click_right:
      - 'damage:15'
    on_kill:
      - 'heal:10'
      - 'exp:50'

  click-cooldown: 3000          # мс
  max-uses: 10                  # -1 = бесконечно
  permission: 'myplugin.use'

  equip-particles: 'FLAME:30'
  equip-sounds: 'ENTITY_PLAYER_LEVELUP:1:1'
  unequip-particles: 'SMOKE:20'
  unequip-sounds: 'ENTITY_VILLAGER_NO:1:1'

  equip-message: '&aПредмет экипирован!'
  unequip-message: '&cПредмет снят!'
  cooldown-message: '&cКулдаун: {seconds}с'
  activation-message: '&6Предмет активирован!'
  deactivation-message: '&7Предмет деактивирован!'
  uses-depleted-message: '&cПредмет использован!'

```

---

## 🔗 PlaceholderAPI

### Настройка

1. Установи [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
2. Плагин автоматически регистрирует плейсхолдеры при старте

### Плейсхолдеры игрока

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_player%` | Имя игрока | `Steve` |
| `%dci_player_uuid%` | UUID | `8667ba71-...` |
| `%dci_health%` | Текущее здоровье | `20` |
| `%dci_max_health%` | Макс. здоровье | `20` |
| `%dci_health_percent%` | % здоровья | `100` |
| `%dci_food%` | Уровень еды | `20` |
| `%dci_saturation%` | Насыщение | `5` |
| `%dci_level%` | Уровень опыта | `30` |
| `%dci_x%`, `%dci_y%`, `%dci_z%` | Координаты | `100`, `64`, `-200` |
| `%dci_world%` | Мир | `world` |
| `%dci_gamemode%` | Режим игры | `SURVIVAL` |
| `%dci_fly_status%` | Полёт | `on` / `off` |
| `%dci_item_in_hand%` | Предмет в руке | `DIAMOND_SWORD` |
| `%dci_online%` | Онлайн | `15` |
| `%dci_max_players%` | Макс. игроков | `50` |

### Плейсхолдеры плагина

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_version%` | Версия плагина | `1.328.0` |
| `%dci_item_count%` | YAML-предметов | `45` |
| `%dci_java_item_count%` | Java API предметов | `6` |
| `%dci_command_count%` | Команд | `3` |
| `%dci_placeholder_count%` | Плейсхолдеров | `12` |
| `%dci_module_count%` | Модулей | `2` |
| `%dci_database_type%` | Тип БД | `sqlite` |
| `%dci_active_effects%` | Активные эффекты | `SPEED:II, STRENGTH:I` |
| `%dci_active_effects_count%` | Кол-во эффектов | `3` |

### Динамические плейсхолдеры предметов

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_has_<id>%` | Есть ли предмет | `%dci_has_vampire-blade%` → `true` |
| `%dci_holding_<id>%` | Держит ли в руке | `%dci_holding_rune-1%` → `false` |
| `%dci_equipped_<id>%` | Экипирован ли | `%dci_equipped_helmet%` → `true` |
| `%dci_item_amount_<id>%` | Кол-во в инвентаре | `%dci_item_amount_potion%` → `3` |

### Плейсхолдеры в сообщениях

Плейсхолдеры работают **автоматически** во всех сообщениях плагина:

```yaml
my-sword:
  equip-message: '&aHP: %dci_health%/%dci_max_health%'
  cooldown-message: '&cКулдаун: {seconds}s | &bУровень: &a%dci_level%'
```

---

## 🏪 Vault Economy (Модуль)

### Настройка

1. Установи [Vault](https://www.spigotmc.org/resources/vault.34315/) + плагин экономики (EssentialsX)
2. Включи модуль-образец: переименуй `items/EXAMPLE-vault/` → `items/vault/`
3. Для отключения — удали папку `items/vault/`

### YAML-конфиг модуля

```yaml
# items/vault/config.yml (после включения EXAMPLE-vault/ -> vault/)
name: "Vault Economy"
version: "2.0"
enabled: true

economy:
  storage: vault   # vault = экономика Vault; own = собственная БД плагина (data.db/MySQL)
  starting-balance: 0
  pay-enabled: true
  min-pay: 1.0


### Собственная БД (`economy.storage: own`)

При `storage: own` экономика модуля полностью живёт в базе данных плагина
(`data.db` SQLite или MySQL — как задано в `config.yml` ядра):

- **`dci_vault_balances`** — балансы игроков (uuid, name, balance)
- **`dci_vault_transactions`** — журнал всех операций (pay/eco/starting)
- Плагин экономики (EssentialsX и т.п.) **не требуется**; сам Vault должен быть
  установлен — его jar нужен runtime-компилятору для сборки модуля
- Валюта настраивается: `currency-symbol`, `currency-name`, `currency-name-plural`


### Команды

| Команда | Описание |
|---------|----------|
| `/balance [player]` (алиасы `/bal`, `/money`) | Баланс свой или другого игрока |
| `/pay <player> <amount>` | Перевести деньги игроку |
| `/eco give\|take\|set\|reset <player> [amount]` | Управление балансом (право `dci.eco`, по умолчанию у OP) |

Права: `dci.balance`, `dci.balance.others`, `dci.pay` — по умолчанию у всех игроков;
`dci.eco` — у OP. Магазин/покупка-продажа предметов — отдельные плагины, работающие с валютой через Vault или публичный API модуля (`getBalance` / `withdraw` / `deposit` / `format`).

---

## ☕ Java API

### Кастомный предмет

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my-sword"; }

    @Override
    public String getDisplayName() { return "§6My Sword"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public ItemStack buildItemStack() {
        return new ItemStack(Material.DIAMOND_SWORD);
    }

    @Override
    public String getItemModel() { return "custom_sword"; }
}
```

### Кастомная команда

```java
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand extends CustomCommand {

    public HealCommand() {
        super("heal", "Исцелить игрока", "/heal [player]", "myplugin.heal");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return true;

        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
        msg(target, "&aВы исцелены!");
        return true;
    }
}
```

### Кастомный плейсхолдер

```java
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

public class KillsPlaceholder extends CustomPlaceholder {

    public KillsPlaceholder() {
        super("kills"); // %dci_kills%
    }

    @Override
    public String getValue(Player player) {
        return String.valueOf(getPlugin().getDatabaseManager().queryInt(
            "SELECT kills FROM stats WHERE uuid=?",
            player.getUniqueId().toString()
        ));
    }
}
```

### Примеры в плагине

| Файл | Описание |
|------|----------|
| `EXAMPLE-kills-placeholder.java` | Счётчик убийств |
| `EXAMPLE-playtime-placeholder.java` | Время игры |
| `EXAMPLE-streak-placeholder.java` | Серия входов |
| `EXAMPLE-balance-placeholder.java` | Виртуальная валюта |
| `EXAMPLE-combo-placeholder.java` | Комбо убийств |
| `EXAMPLE-status-placeholder.java` | Ранг игрока |
| `EXAMPLE-dark-sword.java` | Тёмный меч |
| `EXAMPLE-phoenix-totem.java` | Тотем Феникса |
| `EXAMPLE-give-command.java` | Команда /give |

---

## 📦 Модули

### Включение/выключение

```bash
# Включить модуль из образца — убрать префикс EXAMPLE-
mv items/EXAMPLE-vault/ items/vault/   # ✅ Включён
/ci reload

# Выключить модуль — удалить папку
rm -rf items/vault/                     # ❌ Выключен

# Или в config.yml модуля:
enabled: false
```

### Шаблон модуля

Скопируй `items/_template/` для создания нового модуля:

```bash
cp -r items/_template/ items/my-module/
mv items/my-module/template.java items/my-module/my-module.java
```

### Структура модуля

```
items/my-module/
├── config.yml      # Настройки модуля
├── my-module.java  # Java-класс модуля
└── README.md       # Документация
```

### Доступные модули

| Образец (включить: убрать EXAMPLE-) | Описание | Команды |
|--------|----------|---------|
| `EXAMPLE-vault/` | Vault Economy | `/balance`, `/pay` |
| `EXAMPLE-deluxemenux/` | GUI-меню | `/menu`, `/shop`, `/kits` |
| `EXAMPLE-shop/` | Магазин | `/shop` |
| `EXAMPLE-customcraft/` | GUI-крафт | `/craft` |
| `_template/` | Шаблон (в jar, не копируется) | — |

---

## 🏗️ Архитектура

```
Artifex/
├── src/main/java/me/dcplugin/artifex/
│   ├── Main.java                    # Точка входа (делегирует в PluginBootstrap)
│   ├── bootstrap/
│   │   ├── PluginBootstrap.java     # Инициализация всех компонентов
│   │   ├── CommandRegistrar.java    # Динамическая регистрация команд
│   │   └── ConfigMigrator.java      # Миграция конфига
│   ├── handlers/
│   │   ├── CustomItemHandler.java   # Менеджер кастомных предметов
│   │   ├── ItemLoader.java          # Загрузка из YAML
│   │   ├── LoreManager.java         # Шаблоны lore
│   │   ├── UsesManager.java         # Счётчик использований
│   │   └── EquippedItemsChecker.java # Глобальный таск проверки экипировки
│   ├── managers/
│   │   ├── EffectManager.java       # Эффекты зелий
│   │   ├── AttributeManager.java    # Атрибуты предметов
│   │   ├── ArmorSetManager.java     # Сеты брони
│   │   └── MessageManager.java      # Сообщения
│   ├── listeners/
│   │   ├── PlayerListener.java      # События игрока
│   │   └── TriggerListener.java     # Триггеры
│   ├── api/                         # Java API
│   ├── commands/                    # Встроенные команды
│   ├── models/                      # Модели данных
│   └── utils/
│       ├── EnumCache.java           # Кэширование Bukkit enum
│       ├── ColorUtils.java          # Цвета + PAPI
│       └── ItemBuilder.java         # Билдер предметов
├── src/main/resources/
│   ├── plugin.yml                   # Конфигурация плагина
│   └── config.yml                   # Настройки
```

---

## ⚡ Оптимизации

| Оптимизация | Описание | Эффект |
|-------------|----------|--------|
| 🌍 Глобальный таск | Один `BukkitRunnable` вместо per-player | -90% scheduled задач |
| 🔍 EnumCache | Кэширование Material/Particle/Sound/Attribute (ConcurrentHashMap) | -80% рефлексии |
| 🔧 Стабильные ключи | AttributeManager использует фиксированные ключи в namespace плагина | Корректное удаление |
| ⏱️ Кулдаун пересчёта | 150ms между проверками экипировки | -50% пересчётов |
| 📇 Одно чтение PDC | ID предметов всех слотов читаются один раз за тик | -75% `getItemMeta()` |
| 🧹 Очистка памяти | Кулдауны, эффекты и атрибуты чистятся при выходе игрока | Нет утечек |
| 🧵 Отдельный поток reload | `reloadAsync()` не занимает поток общего пула | Нет блокировок |

### Опции `config.yml`

| Опция | По умолчанию | Описание |
|-------|--------------|----------|
| `java-compilation` | `true` | Runtime-компиляция `.java` из `items/` (выполнение произвольного кода) |
| `debug-mode` | `false` | Подробное логирование |
| `file-logging` | `false` | Логи плагина в `plugins/Artifex/logs/latest.log` вместо консоли |
| `backup.on-reload` / `backup.keep` | `true` / `5` | Бэкап `items/` перед `/artifex reload` |

---

## 🛠️ Требования

- Minecraft 1.21.8+
- Paper/Spigot (рекомендуется Paper — плагин компилируется против paper-api)
- Java 21+
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (опционально)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + economy plugin (опционально)

---

## 📚 Документация

| Язык | Ссылка |
|------|--------|
| 📖 **Вики (с нуля, A→Z)** | [**wiki/README.md**](wiki/README.md) — 19 страниц: установка, YAML и Java-предметы, все действия и триггеры, рецепты, команды, плейсхолдеры, модули, сообщения, БД, события, конфиг, права, примеры, решение проблем |
| 🇬🇧 English | [Full Documentation](docs/README_EN.md) |
| 🇷🇺 Русский | [Полная документация](docs/README_RU.md) |
| ☕ Java API | [Java API Guide](docs/JAVA_API_RU.md) |
| 📦 YAML Предметы | [YAML Items Guide](docs/YAML_ITEMS_RU.md) |
| ⚡ Команды | [Commands Guide](docs/COMMANDS_RU.md) |
| 🗄️ База данных | [Database Guide](docs/DATABASE_RU.md) |
| 🎨 Resource Pack | [Resource Pack Guide](docs/RESOURCE_PACK_RU.md) |
| 🔧 Модули GUI | [Modules Guide](docs/MODULES_GUI_RU.md) |
| 💬 Сообщения | [Messages Guide](docs/MESSAGES_DATABASE_RU.md) |
| ❓ Решение проблем | [Troubleshooting](docs/TROUBLESHOOTING_RU.md) |

---

## 🔗 Ссылки

- [GitHub Repository](https://github.com/animesao/Artifex)
- [Releases](https://github.com/animesao/Artifex/releases)
- [Issues](https://github.com/animesao/Artifex/issues)

---

## 📄 Лицензия

MIT License — см. [LICENSE](LICENSE) для подробностей.

---

**Сделано с ❤️ by animesao**