# 10 — Модули

## Что ты узнаешь

- Что такое **модуль** и чем он отличается от обычного предмета.
- Как устроена папка модуля: `config.yml` и `.java`-файл.
- Какие ключи есть в `config.yml` модуля и как их читать.
- Как плагин находит модуль и включает его.
- Класс `Module`: конструктор, жизненный цикл (`onEnable` / `onDisable` / `reload`) и все помощники.
- Полный рабочий пример модуля.
- Готовые модули из `samples/`: `shop`, `vault`, `customcraft`, `deluxemenux` — что каждый делает и что нужно для его работы.
- Папка `_template/` — отправная точка для своего модуля.
- Что происходит с модулями при `/ci reload` и что это значит для их данных.
- `SimpleModule` — модуль, который состоит только из YAML.

## Предусловия

- Плагин установлен — [Установка](01-getting-started.md).
- Ты понимаешь структуру папки `items/` — [Как это работает](02-how-it-works.md).
- Умеешь писать YAML — [Предметы (YAML)](03-yaml-items.md).
- Для своей части — базовые знания Java и [Предметы (Java)](04-java-items.md).
- Знаешь, как пишутся свои команды — [Команды](08-commands.md).
- Общий список страниц — [оглавление](README.md).

## Содержание

- [Что такое модуль](#что-такое-модуль)
- [Структура папки модуля](#структура-папки-модуля)
- [config.yml модуля](#configyml-модуля)
- [Как модуль находится и включается](#как-модуль-находится-и-включается)
- [Класс Module](#класс-module)
  - [Конструктор](#конструктор)
  - [Жизненный цикл](#жизненный-цикл)
  - [Помощники модуля](#помощники-модуля)
- [Полный рабочий пример модуля](#полный-рабочий-пример-модуля)
- [Готовые модули из samples](#готовые-модули-из-samples)
  - [shop — простой магазин](#shop--простой-магазин)
  - [vault — экономика](#vault--экономика)
  - [customcraft — крафт через GUI](#customcraft--крафт-через-gui)
  - [deluxemenux — меню](#deluxemenux--меню)
- [Шаблон _template](#шаблон-_template)
- [Что происходит с модулями при ci reload](#что-происходит-с-модулями-при-ci-reload)
- [SimpleModule — модуль только на YAML](#simplemodule--модуль-только-на-yaml)
- [Частые ошибки](#частые-ошибки)

---

## Что такое модуль

**Модуль** — это «плагин внутри плагина»: отдельная папка в `items/`, в которой лежат свои настройки, свои предметы и свой Java-код.

Плагин сам по себе умеет делать предметы. Всё остальное — магазин, экономика, крафт в GUI, меню — делают модули:

| | Обычный предмет | Модуль |
|---|---|---|
| Где лежит | `items/меч.yml` | `items/shop/` (папка) |
| Что умеет | Эффекты, действия, рецепты | Команды, GUI-меню, экономика, своя логика |
| Свой код | нет (только YAML) или Java-предмет | Java-класс `extends Module` |
| Настройки | внутри YAML предмета | свой `config.yml` |
| Включение | файл существует | папка существует (+ `enabled: true`) |

Главное правило: **модуль — это папка**. Удалили папку — модуля больше нет. Никакой отдельной команды «включить модуль» не существует.

## Структура папки модуля

```text
plugins/Artifex/items/
├── messages.java          ← сообщения плагина (не модуль)
├── my-item.yml            ← обычный предмет (не модуль)
└── shop/                  ← а вот это модуль
    ├── config.yml         ← настройки модуля (обязательно)
    ├── shop.java          ← код модуля (для Java-модуля)
    ├── items.yml          ← предметы модуля (необязательно)
    └── README.md          ← документация (необязательно)
```

| Файл | Обязателен? | Что это |
|------|--------------|---------|
| `config.yml` | да | Настройки модуля. Если файла нет — плагин создаст пустой с ключами по умолчанию |
| `<имя>.java` | для Java-модуля | Класс модуля. Должен лежать **в папке модуля** и наследовать `Module` |
| `items.yml` | нет | Предметы модуля (для простых YAML-модулей и меню) |
| `README.md` | нет | Ваша документация |

> **Имя папки = id модуля.** Папка `shop` → id `shop`. Имя файла `.java` может быть любым (`shop.java`, `main.java`), но id модуля всегда равен имени папки.

## config.yml модуля

Ключи, которые читает плагин:

```yaml
name: "Shop"          # человеческое имя (в логах)
version: "1.0"        # версия модуля
enabled: true         # false → Java-модуль не включается
commands:             # список команд модуля (для справки и документации)
  - shop
permissions:          # список прав модуля (для справки и документации)
  - shop.use
  - shop.admin
```

| Ключ | Тип | Кто читает | Описание |
|------|-----|-----------|----------|
| `name` | строка | логи | Имя модуля в сообщениях консоли |
| `version` | строка | — | Версия модуля |
| `enabled` | да/нет | `ModuleManager` | `false` — Java-модуль не включается (подробности ниже) |
| `commands` | список | `Module` | Команды модуля. Плагин **не** регистрирует их сам — модуль регистрирует их в `onEnable()` через `registerDynamicCommand(...)`. Список нужен для документации и для программ, которые его читают |
| `permissions` | список | `Module` | Права модуля — тоже для документации |

Всё остальное, что вы положите в `config.yml`, — ваши собственные настройки. Модуль читает их через помощники `getConfig…` (см. ниже):

```yaml
# Свои настройки — как хотите
settings:
  max-items: 10
  price-multiplier: 1.5
  message-prefix: "&a[Магазин] &r"
```

## Как модуль находится и включается

При старте сервера и на каждой `/ci reload` плагин выполняет `ModuleManager.loadAll()`:

1. Смотрит в папку `items/` и берёт **все подпапки**.
2. Пропускает папки-образцы: те, что начинаются с `EXAMPLE-`, и папки `EXAMPLES` и `_template`. Они лежат в репозитории как примеры и не должны включаться случайно.
3. Для каждой оставшейся папки:
   - если нет `config.yml` — создаёт его с ключами по умолчанию;
   - ищет **Java-модуль** с таким id (уже скомпилированный из `items/<папка>/*.java`);
   - если Java-модуль найден — кладёт его в список и включает, если в `config.yml` стоит `enabled: true`;
   - если Java-модуля нет — создаёт **простой YAML-модуль** (`SimpleModule`) и включает его.

В консоли это выглядит так:

```text
[Modules] Found 3 potential modules
[Modules] Loading module: Shop (shop)
[Module] Enabled: shop
[Modules] Loaded 3 modules
```

### Про `enabled: false`

| Тип модуля | Что делает `enabled: false` |
|-------------|----------------------------|
| Java-модуль (есть `.java`) | Модуль **не включается**: `onEnable()` не вызывается, его команды не регистрируются |
| Простой YAML-модуль (только YAML) | Флаг **не проверяется** — модуль всё равно загрузится. Такой модуль отключают, удалив папку или убрав из `config.yml` секцию `menu:` |

## Класс Module

Базовый класс: `me.dcplugin.artifex.api.modules.Module`.

### Конструктор

```java
public Module(Main plugin, String id, File folder) {
    this.plugin = plugin;
    this.id = id;
    this.folder = folder;

    // Сразу читает config.yml и items.yml, если они есть
    this.config = configFile.exists() ? YamlConfiguration.loadConfiguration(configFile) : new YamlConfiguration();
    this.itemsConfig = itemsFile.exists() ? YamlConfiguration.loadConfiguration(itemsFile) : new YamlConfiguration();

    loadConfig();
}
```

| Параметр | Что это |
|----------|---------|
| `plugin` | Главный класс плагина (`me.dcplugin.artifex.Main`) |
| `id` | id модуля = имя папки |
| `folder` | Папка модуля на диске |

Плагин вызывает конструктор сам, от вас нужен **публичный** конструктор с ровно такой сигнатурой:

```java
public shopModule(Main plugin, String id, File folder) {
    super(plugin, id, folder);
}
```

Внутри модуля доступны поля базового класса:

| Поле | Тип | Что это |
|------|-----|---------|
| `plugin` | `Main` | Главный класс плагина |
| `id` | `String` | id модуля (имя папки) |
| `folder` | `File` | Папка модуля |
| `config` | `YamlConfiguration` | Содержимое `config.yml` |
| `itemsConfig` | `YamlConfiguration` | Содержимое `items.yml` |
| `enabled` | `boolean` | Включён ли модуль сейчас |

### Жизненный цикл

```text
создание → enable() → onEnable()  →  (работает)
                    ↓
                 reload() → disable() → onDisable() → loadConfig() → enable() → onEnable()
                    ↓
                 disable() → onDisable()
```

| Метод | Когда вызывается | Что делать в нём |
|-------|------------------|------------------|
| `protected abstract void onEnable()` | Модуль включился (старт сервера или `/ci reload`) | Зарегистрировать команды и слушатели, подключиться к базе, запустить таймеры |
| `protected abstract void onDisable()` | Модуль выключается (перезагрузка или остановка сервера) | Отменить таймеры, снять слушателей, сохранить данные, убрать эффекты |
| `public void enable()` | Внешний вызов | Вызывает `onEnable()`, если модуль ещё не включён |
| `public void disable()` | Внешний вызов | Вызывает `onDisable()` и снимает динамические команды |
| `public void reload()` | Внешний вызов | `disable()` → перечитать `config.yml` → `enable()` |

> **Осторожно с `onDisable`.** Он вызывается и при перезагрузке, и при выключении сервера. Всё, что там есть, должно быть безопасно выполнять дважды. И наоборот: то, что должно жить вместе с модулем, регистрируйте в `onEnable()`.

### Помощники модуля

Все эти методы уже есть в базовом классе — их не нужно писать.

**Чтение `config.yml`:**

| Метод | Что возвращает | Пример |
|------|----------------|--------|
| `String getConfig(String path, String def)` | Строку | `getConfig("settings.message-prefix", "&a[Модуль] &r")` |
| `int getConfigInt(String path, int def)` | Целое число | `getConfigInt("settings.max-items", 10)` |
| `boolean getConfigBoolean(String path, boolean def)` | Да/нет | `getConfigBoolean("settings.enabled", true)` |
| `List<String> getConfigList(String path)` | Список строк | `getConfigList("commands")` |

**Работа с `items.yml`:**

| Метод | Что возвращает |
|-------|----------------|
| `Map<String, Object> getItem(String itemId)` | Карту с полями предмета из секции `items.<itemId>` (пустую карту, если предмета нет) |
| `Set<String> getAllItemIds()` | Множество id всех предметов из `items.yml` |

**Команды:**

| Метод | Что делает |
|-------|-----------|
| `void registerDynamicCommand(String name, CommandExecutor executor)` | Регистрирует команду в `CommandMap` сервера. Право по умолчанию — `dci.<имя>` |
| `void registerDynamicCommand(String name, CommandExecutor executor, String permission)` | То же, но с явным правом |

Команда, зарегистрированная так, **автоматически снимается** при выключении модуля — «мёртвые» команды не остаются в `CommandMap`.

**Сообщения (центральная система):**

| Метод | Что делает |
|-------|-----------|
| `String getMessage(String path, String defaultMessage)` | Берёт текст по пути из центральной системы сообщений. Администратор может переопределить его в `items/messages.java` — см. [Сообщения](11-messages.md) |
| `void registerMessage(String path, String value)` | Регистрирует **свой** текст модуля под произвольным путём, чтобы его тоже можно было переопределить |
| `void unregisterMessage(String path)` | Убирает ранее зарегистрированный текст |

**Плейсхолдеры:**

В базовом классе метода `registerPlaceholders()` нет — это **ваш** метод (так сделано и в `_template/template.java`, и в `EXAMPLE-vault`). Регистрация выглядит так:

```java
private void registerPlaceholders() {
    PlaceholderManager pm = plugin.getPlaceholderManager();
    if (pm == null) return;

    pm.register("shop_balance", player -> "1000");   // %artifex_shop_balance%
}
```

Подробнее — в [Плейсхолдерах](09-placeholders.md).

**Открытие меню:**

| Метод | Что делает |
|-------|-----------|
| `boolean openMenu(Player player, String menuId)` | Открывает меню модуля. По умолчанию возвращает `false` — модуль с меню переопределяет этот метод |

**Геттеры:**

`getId()`, `getFolder()`, `getConfig()`, `getItemsConfig()`, `isEnabled()`, `getCommands()`, `getPermissions()`.

## Полный рабочий пример модуля

Файл `plugins/Artifex/items/my-module/my-module.java`:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

public class myModule extends Module implements Listener, CommandExecutor {

    private int greetings = 0;

    public myModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    // ===== ЖИЗНЕННЫЙ ЦИКЛ =====

    @Override
    protected void onEnable() {
        // 1. Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // 2. Регистрируем команду (право по умолчанию — dci.mycommand)
        registerDynamicCommand("mycommand", this, "dci.mycommand");

        // 3. Регистрируем свои тексты — их можно переопределить в items/messages.java
        registerMessage("my-module.hello", "&aПривет, {player}!");
        registerMessage("my-module.no-permission", "&cВам сюда нельзя.");

        // 4. Читаем настройки
        greetings = getConfigInt("settings.greetings", 1);
        plugin.getLogger().info("[MyModule] Включён, приветствий: " + greetings);
    }

    @Override
    protected void onDisable() {
        // Снимаем слушатель — иначе после перезагрузки их будет всё больше
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("[MyModule] Выключен");
    }

    // ===== КОМАНДА =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Текст берём из центральной системы сообщений:
        // администратор может заменить его в items/messages.java
        sender.sendMessage(getMessage("my-module.hello", "&aПривет, {player}!")
                .replace("{player}", sender.getName()));
        return true;
    }

    // ===== СЛУШАТЕЛЬ =====

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < greetings; i++) {
            player.sendMessage(getMessage("my-module.hello", "&aПривет, {player}!")
                    .replace("{player}", player.getName()));
        }
    }
}
```

И `config.yml` рядом с ним:

```yaml
name: "My Module"
version: "1.0"
enabled: true

commands:
  - mycommand

permissions:
  - dci.mycommand

settings:
  greetings: 1
```

После `/ci reload`:

```text
[Modules] Loading module: My Module (my-module)
[Module my-module] Registered command: /mycommand
[Module] Enabled: my-module
```

## Готовые модули из samples

В репозитории плагина есть папка `samples/` с готовыми примерами. Они **не устанавливаются** автоматически и не лежат внутри jar-файла — это образцы, которые нужно скопировать себе.

Общее правило для всех образцов:

1. Скопируйте нужную папку в `plugins/Artifex/items/`.
2. Уберите префикс `EXAMPLE-` из имени папки.
3. Выполните `/ci reload`.

Пока в имени папки есть `EXAMPLE-`, модуль **не загрузится** — это защита от случайного включения.

| Образец | Папка после копирования | Что делает | Что нужно |
|---------|------------------------|------------|-----------|
| Магазин | `shop/` | Простое GUI-меню с товарами из `items.yml` | Ничего |
| Экономика | `vault/` | Баланс игрока, `/balance`, `/pay`, `/eco`, стартовый баланс | Vault + плагин экономики (EssentialsX) **или** `economy.storage: own` |
| Крафт | `customcraft/` | Крафт кастомных предметов через GUI-верстак | Ничего (рецепты берутся из ваших предметов) |
| Меню | `deluxemenux/` | Полноценная система меню: `/menu`, `/kits`, `/shop` | Ничего |

### shop — простой магазин

Состав:

```text
items/shop/
├── config.yml     ← настройки меню
├── items.yml      ← товары (материал, название, лор, цена, команда, сообщение)
└── shop.java      ← код: собирает меню и выдаёт товар
```

Товар в `items.yml` выглядит так:

```yaml
items:
  diamond:
    material: DIAMOND
    name: "&bАлмаз"
    lore:
      - ""
      - "&7Цена: &a100$"
    price: 100
    command: "give %player% diamond 1"
    message: "&aВы купили алмаз!"
```

Ключи товара:

| Ключ | Что это |
|------|---------|
| `material` | Материал иконки (`DIAMOND`, `EMERALD`, …) |
| `name` | Название |
| `lore` | Описание (список строк) |
| `price` | Цена (для своей валюты) |
| `command` | Команда, которая выполняется при покупке. `%player%` — ник игрока |
| `message` | Сообщение, которое видит игрок после покупки |

Раздел `menu` в `config.yml` описывает само окно:

```yaml
menu:
  title: "&6Магазин"
  size: 27
  fill:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
  items:
    11:
      item: diamond
    13:
      item: emerald
```

| Ключ | Что это |
|------|---------|
| `menu.title` | Заголовок окна |
| `menu.size` | Размер (кратно 9: 9, 18, 27, 36, 45, 54) |
| `menu.fill.material` / `menu.fill.name` | Чем заполнить пустые слоты |
| `menu.items.<слот>.item` | Какой товар из `items.yml` поставить в слот |

> **Образец-скелет.** Модуль `shop.java` показывает, как собрать окно из `items.yml` (метод `openShop(Player)`), но он **не** регистрирует команду `/shop` и **не** слушает клики. Это заготовка: допишите команду (по образцу `deluxemenux`) и обработчик `InventoryClickEvent` — тогда получится рабочий магазин.

### vault — экономика

Самый большой образец: полноценная валюта игроков.

Что умеет:

| Команда | Что делает | Право |
|---------|-----------|-------|
| `/balance [игрок]`, `/bal`, `/money` | Показать баланс | `dci.balance`, `dci.balance.others` |
| `/pay <игрок> <сумма>` | Перевести деньги | `dci.pay` |
| `/eco give\|take\|set\|reset <игрок> [сумма]` | Управление балансами | `dci.eco` |

Главное настройка в `config.yml`:

```yaml
economy:
  storage: vault        # vault — через Vault; own — своя база плагина
  currency-symbol: "$"
  currency-name: "dollar"
  currency-name-plural: "dollars"
  starting-balance: 0
  pay-enabled: true
  min-pay: 1.0
```

| `economy.storage` | Что нужно |
|-------------------|-----------|
| `vault` (по умолчанию) | Плагин **Vault** + любой плагин экономики (EssentialsX, ChargeMe и т. п.) |
| `own` | Только база данных самого плагина (`data.db` или MySQL из `config.yml`). Плагин экономики не нужен |

> **Нюанс.** Даже при `storage: own` для **компиляции** модуля нужен jar-файл плагина Vault (из него берётся класс `net.milkbowl.vault.economy.Economy`). Без Vault в консоли будет ошибка компиляции модуля.

Все тексты модуля лежат в `config.yml` (раздел `economy`) и могут быть переопределены через центральную систему сообщений.

### customcraft — крафт через GUI

Даёт игроку виртуальный верстак (`/craft`), где видны **все** рецепты ваших предметов сразу — и `shaped`, и `shapeless`.

Настройки в `config.yml`:

```yaml
command: craft
permission: dci.craft

gui:
  title: "&8⚒ CustomCraft"
  filler: GRAY_STAINED_GLASS_PANE
  result-slot: 22
  return-items-on-close: true

messages:
  no-recipes: "&cУ вас нет доступных рецептов."
  no-permission: "&cУ вас нет прав на /craft!"
  crafted: "&aПредмет создан!"
  inventory-full: "&cИнвентарь полон!"
```

| Ключ | Что это |
|------|---------|
| `command` | Имя команды |
| `permission` | Право на команду |
| `gui.title` | Заголовок окна |
| `gui.filler` | Чем заполнить пустые слоты |
| `gui.result-slot` | Слот с результатом |
| `gui.return-items-on-close` | Возвращать ли ингредиенты при закрытии окна |

Как работает:

- Модуль берёт рецепты **из ваших предметов** (раздел `recipes:`) — отдельно их описывать не нужно.
- Ингредиенты определяются по **PDC** (внутренним данным предмета): свои предметы — по id, чужие — по материалу (чужие NBT не портятся).
- Готовый предмет получает правильные заряды и время жизни через `ItemHandler`.

Рецепты из ваших YAML-предметов — [Рецепты](07-recipes.md).

### deluxemenux — меню

Полноценная система меню, похожая на DeluxeMenus.

```text
items/deluxemenux/
├── config.yml
├── deluxemenux.java
├── menu-command.java   ← команда /menu
├── kits-command.java   ← команда /kits
├── shop-command.java   ← команда /shop
├── README.md
└── menus/
    ├── shop.yml
    └── kits.yml
```

Меню описываются YAML-файлами в папке `menus/`:

```yaml
title: "&6🛒 Магазин"
size: 45
open-sound: "UI_BUTTON_CLICK"

fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "

items:
  19:
    material: DIAMOND
    name: "&bАлмаз"
    lore:
      - ""
      - "&7Цена: &a100$"
    command: "give %player% diamond 1"
    message: "&aВы купили алмаз!"
    sound: "ENTITY_PLAYER_LEVELUP"
    close: false
```

| Ключ | Что это |
|------|---------|
| `title` | Заголовок окна |
| `size` | Размер (9, 18, 27, 36, 45, 54) |
| `open-sound` / `close-sound` | Звуки при открытии и закрытии |
| `fill.material` / `fill.name` | Фон пустых слотов |
| `items.<слот>.material` | Материал иконки |
| `items.<слот>.command` | Команда при клике. `%player%` — ник игрока |
| `items.<сlot>.message` | Сообщение игроку |
| `items.<слот>.sound` | Звук при клике |
| `items.<слот>.close` | Закрывать ли окно после клика |
| `items.<слот>.permission` | Право, без которого предмет не показывается |

Команды модуля: `/menu`, `/menu <id>`, `/menu list`, `/menu reload`, `/kits` (алиас `/kit`), `/shop`.

Права: `deluxemenux.use`, `deluxemenux.admin`, `deluxemenux.kit.*`, `deluxemenux.shop.*`.

Если файла меню нет — модуль создаст пустой пример. Ваши правки он **не** перезаписывает.

> **Как это устроено внутри.** Команды `/menu`, `/kits`, `/shop` — это отдельные файлы `*-command.java` рядом с модулем. Они наследуют `CustomCommand` (см. [Команды](08-commands.md)) и находят модуль через `plugin.getModuleManager().getModule("deluxemenux")`, а затем открывают меню. Сам модуль `deluxemenux.java` слушает `InventoryClickEvent` и выполняет команды из меню. Это удобный образец: **модуль + его команды живут в одной папке**.

## Шаблон _template

В `samples/_template/` лежит заготовка, с которой проще всего начать свой модуль.

```text
samples/_template/
├── config.yml        ← подробно расписанный пример config.yml
├── template.java     ← код модуля-шаблона
├── README.md         ← пошаговая инструкция
└── player-diary/    ← пример модуля, который хранит данные игроков
```

Как использовать:

```bash
cd plugins/Artifex/items/
cp -r _template/ my-module/
cd my-module/
mv template.java my-module.java
```

Потом в файле замените:

| Было | Стало |
|------|-------|
| `templateModule` | `myModule` |
| `template` | `my-module` |
| `mymodule_` | `my-` |

И поправьте `config.yml` под свои задачи.

Сам шаблон показывает всё, что может понадобиться:

| Блок в шаблоне | Что показывает |
|----------------|----------------|
| `onEnable()` | Регистрация команд, слушателей, плейсхолдеров, текстов |
| `onDisable()` | Снятие слушателей, остановка таймеров |
| `onCommand()` | Обработка команды |
| `onTabComplete()` | Автодополнение |
| `@EventHandler` | Слушатель событий (вход и выход игрока) |
| `registerPlaceholders()` | Регистрация плейсхолдеров |
| Работа с предметами | Как взять, выдать и забрать свой предмет |
| Работа с базой | `execute` / `queryString` |
| GUI-меню | Создание окна инвентаря |
| Таймеры | `runTaskTimer` и `runTaskLater` |

### player-diary — модуль с данными игроков

Внутри `_template/` лежит ещё один пример — `player-diary/`. Это модуль, который считает убийства, смерти и время игры.

Он показывает главное, чего нет в простых образцах: **как сохранять данные**.

| Что | Как |
|-----|-----|
| Где хранятся данные | В базе плагина (таблица с uuid игрока) |
| Как читать | `plugin.getDatabaseManager().queryString(...)` |
| Как писать | `plugin.getDatabaseManager().execute(...)` |
| Как часто сохранять | По таймеру — в `config.yml` есть `settings.save-interval` |
| Плейсхолдеры | `%artifex_diary_kills%`, `%artifex_diary_deaths%`, `%artifex_diary_playtime%` |

Подробнее про базу — [База данных](12-database.md).

## Что происходит с модулями при ci reload

При `/ci reload` модули проходят полный круг:

```text
disable()  →  onDisable()  →  снятие динамических команд
   ↓
перечитывание config.yml
   ↓
enable()   →  onEnable()   →  новая регистрация команд и слушателей
```

| Что происходит | Пояснение |
|----------------|-----------|
| Команды модуля снимаются с сервера | И не остаются «мёртвыми» в `CommandMap` |
| Слушатели надо снимать самим | В `onDisable()` вызовите `HandlerList.unregisterAll(this)` — иначе они накопятся |
| Класс модуля создаётся заново | Статические поля обнуляются: после перезагрузки их значения — как у нового объекта |
| Данные в памяти теряются | Поэтому всё важное пишите в файл или в базу (в `onDisable()` или сразу при изменении) |
| Файлы модуля остаются | Папка `items/my-module/` не трогается — это ваши данные |
| Данные в базе остаются | Таблицы модуля не чистятся |

Вывод: **модуль без базы или файла потеряет данные при перезагрузке**. Если модуль что-то считает — пишите это в базу или в файл в папке модуля.

> **Замечание о двойном вызове.** Модули перезагружаются внутри перезагрузки Java API, а затем ещё раз — отдельным вызовом в конце `/ci reload`. Поэтому в консоли вы можете увидеть `[Module] Enabled: …` дважды. Это не ошибка: `onDisable()` и `onEnable()` просто вызываются по два раза. Именно поэтому оба метода должны быть безопасны при повторном вызове.

Отключение модуля — два способа:

| Способ | Как |
|-------|-----|
| Выключить | `enabled: false` в `config.yml` (для Java-модуля) + `/ci reload` |
| Удалить | Удалить папку модуля + `/ci reload` |

## SimpleModule — модуль только на YAML

Если в папке модуля нет `.java`-файла, плагин создаёт **простой модуль** — `SimpleModule`. Он умеет ровно одну вещь: собрать меню из `config.yml` и `items.yml` и выполнить команду при клике.
Структура:

```text
items/simple-shop/
├── config.yml
└── items.yml
```

`config.yml`:

```yaml
name: "Simple Shop"
version: "1.0"
enabled: true

menu:
  title: "&6Простой магазин"
  size: 27
  fill:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
  items:
    13:
      item: diamond
```

`items.yml`:

```yaml
items:
  diamond:
    material: DIAMOND
    name: "&bАлмаз"
    lore:
      - "&7Цена: &a100$"
    price: 100
    command: "give %player% diamond 1"
    message: "&aВы купили алмаз!"
```

| Что умеет SimpleModule | Подробности |
|------------------------|-------------|
| Собирает окно по разделу `menu` | Заголовок, размер, фон, предметы по номерам слотов |
| Открывает окно игроку | Метод `openMenu(Player)` — **его нужно вызвать из своего кода** |
| Обрабатывает клик по предмету | Метод `handleMenuClick(Player, slot)`: выполняет `command` (с подстановкой `%player%`) и показывает `message`. **Его тоже нужно вызвать из своего кода** |
| Свои тексты | Только те, что вы написали в YAML |

> **Важно.** Сам по себе `SimpleModule` — это «заготовка», а не готовый магазин: он не регистрирует слушатель событий и не создаёт команду. Чтобы им пользоваться, напишите Java-модуль или свою команду, которая откроет меню:
>
> ```java
> SimpleModule module = (SimpleModule) plugin.getModuleManager().getModule("simple-shop");
> if (module != null) module.openMenu(player);
> ```
>
> Обработку клика (событие `InventoryClickEvent`) вы описываете сами и вызываете `module.handleMenuClick(player, slot)`.
>
> Готовый пример Java-модуля, который собирает меню из `items.yml`, — в `samples/EXAMPLE-shop/`. Обработку кликов и команду там нужно дописать самому (образец полностью рабочей системы меню — в `samples/EXAMPLE-deluxemenux/`).

| чего SimpleModule **не** умеет | Что делать |
|------------------------------|------------|
| Свои команды | Напишите Java-модуль |
| Своя логика (проверки, права, расчёты) | Напишите Java-модуль |
| Свои плейсхолдеры | Напишите Java-модуль |
| Работа с базой | Напишите Java-модуль |

## Частые ошибки

| Симптом | Причина | Что делать |
|---------|---------|------------|
| Модуль не появился в логах | Папка называется `EXAMPLE-…`, `EXAMPLES` или `_template` | Уберите префикс `EXAMPLE-` |
| Модуль появился, но `onEnable()` не вызван | В `config.yml` стоит `enabled: false` | Поставьте `enabled: true` |
| Модуль загрузился, но ничего не делает | Это `SimpleModule`, а в `config.yml` нет секции `menu:` | Добавьте `menu:` или напишите Java-модуль |
| `ClassCastException` / ошибка загрузки класса | Класс не `public` или нет конструктора `(Main, String, File)` | Сделайте `public class X extends Module` и `public X(Main plugin, String id, File folder)` |
| Команда модуля не появилась | Команда не зарегистрирована в `onEnable()` | Добавьте `registerDynamicCommand("имя", this, "право")` |
| Команда осталась после удаления модуля | Команда зарегистрирована вручную, а не через `registerDynamicCommand` | Используйте `registerDynamicCommand` — он снимает команду сам |
| После нескольких перезагрузок события срабатывают несколько раз | Слушатели не сняты в `onDisable()` | Добавьте `HandlerList.unregisterAll(this)` |
| Данные модуля пропали после reload | Данные лежали только в памяти | Сохраняйте их в файл или в базу |
| Класс модуля не найден | Класс лежит не в папке модуля, а в `items/` | Файл `.java` должен лежать **внутри** папки модуля |
| Ошибка компиляции модуля | В коде опечатка | Смотрите консоль: `[JavaCompiler] ❌ файл:строка - …` |

---

Смотрите также:

- [Команды](08-commands.md) — свои команды и `CustomCommand`.
- [Сообщения](11-messages.md) — `registerMessage` / `getMessage` в модуле.
- [Плейсхолдеры](09-placeholders.md) — свои плейсхолдеры модуля.
- [База данных](12-database.md) — как сохранять данные.
- [Примеры](16-examples.md) — готовые связки.
