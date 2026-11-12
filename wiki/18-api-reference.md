# 18. Справочник API

> **Что ты узнаешь:** полный список классов и методов, доступных из ваших
> `.java`-файлов, модулей и сторонних плагинов.
>
> **Предусловия:** базовое знание Java. Для пошагового введения —
> [Предметы (Java)](04-java-items.md).

---

## Содержание

- [Как обращаться к API](#как-обращаться-к-api)
- [Классы](#классы)
- [AbstractCustomItem](#abstractcustomitem)
- [ItemAPI](#itemapi)
- [RecipeDef](#recipedef)
- [CustomCommand](#customcommand)
- [CustomPlaceholder](#customplaceholder)
- [Module](#module)
- [MessageManager и MessagesConfig](#messagemanager-и-messagesconfig)
- [DatabaseManager](#databasemanager)
- [YamlStorage](#yamlstorage)
- [События](#события)
- [Менеджеры](#менеджеры)
- [Утилиты](#утилиты)

---

## Как обращаться к API

Из файла в `items/`:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.api.RecipeDef;
```

Из модуля или другого плагина — через `Main`:

```java
Main plugin = Main.getInstance();
plugin.getItemHandler();       // менеджер предметов
plugin.getApiItemRegistry();   // реестр Java API
plugin.getMessageManager();    // сообщения
plugin.getDatabaseManager();   // база данных
plugin.getModuleManager();     // модули
plugin.getConfigManager();     // config.yml
plugin.getEffectManager();     // эффекты
plugin.getAttributeManager();  // атрибуты
plugin.getArmorSetManager();   // сеты брони
plugin.getPlaceholderManager();// плейсхолдеры
plugin.getApiItemRegistry().getCompiler(); // компилятор
```

**Все имена пакетов:**

| Пакет | Что внутри |
|-------|-----------|
| `me.dcplugin.artifex.api` | `AbstractCustomItem`, `ItemAPI`, `ItemRegistry`, `RecipeDef`, `JavaItemCompiler` |
| `me.dcplugin.artifex.api.commands` | `CustomCommand`, `CustomCommandExecutor` |
| `me.dcplugin.artifex.api.config` | `MessagesConfig` |
| `me.dcplugin.artifex.api.database` | `DatabaseManager`, `YamlStorage` |
| `me.dcplugin.artifex.api.modules` | `Module`, `ModuleManager`, `SimpleModule` |
| `me.dcplugin.artifex.api.placeholders` | `CustomPlaceholder`, `PlaceholderManager`, `DCIPlaceholderExpansion` |
| `me.dcplugin.artifex.events` | 8 событий плагина |
| `me.dcplugin.artifex.managers` | `EffectManager`, `AttributeManager`, `MessageManager`, `ConfigManager`, `ArmorSetManager` |
| `me.dcplugin.artifex.models` | `CustomItem` (модель YAML-предмета) |
| `me.dcplugin.artifex.utils` | `ActionParser`, `ColorUtils`, `EnumCache`, `ItemBuilder`, `ItemFX`, `CooldownStore`, `UpdateChecker` |

---

## Классы

| Класс | Тип | Назначение |
|-------|-----|-----------|
| `AbstractCustomItem` | абстрактный | Базовый класс Java-предмета |
| `ItemAPI` | статический | Хелперы (эффекты, частицы, телепорт, урон) |
| `ItemRegistry` | обычный | Реестр Java-предметов, команд, плейсхолдеров, модулей |
| `RecipeDef` | значение | Описание рецепта для Java API |
| `CustomCommand` | абстрактный | Базовый класс команды |
| `CustomPlaceholder` | абстрактный | Базовый класс плейсхолдера |
| `Module` | абстрактный | Базовый класс модуля |
| `SimpleModule` | обычный | YAML-модуль без кода |
| `ModuleManager` | обычный | Загрузка и выгрузка модулей |
| `MessagesConfig` | статический | Все тексты плагина |
| `MessageManager` | обычный | Резолв текстов по путям |
| `DatabaseManager` | обычный | SQLite / MySQL |
| `YamlStorage` | обычный | Простое YAML-хранилище |
| `CustomItem` | значение | Модель YAML-предмета |
| `ActionParser` | статический | Выполнение действий |
| `ColorUtils` | статический | Цвета и PlaceholderAPI в тексте |
| `EnumCache` | статический | Кэш enum-констант Bukkit |
| `ItemBuilder` | обычный | Сборка ItemStack |

---

## AbstractCustomItem

Полный справочник — в [Предметах (Java)](04-java-items.md). Кратко:

### Обязательные

| Метод | Возвращает |
|-------|-----------|
| `String getId()` | id предмета (уникальный, в нижнем регистре) |
| `String getDisplayName()` | имя с кодами `&` |
| `Material getMaterial()` | материал |

### Внешний вид (по умолчанию)

| Метод | По умолчанию |
|-------|--------------|
| `List<String> getLore()` | пусто |
| `int getAmount()` | 1 |
| `String getItemModel()` | `null` |
| `int getCustomModelData()` | -1 |
| `boolean isUnbreakable()` | false |
| `boolean isGlowing()` | false |

### Поведение (по умолчанию)

| Метод | По умолчанию |
|-------|--------------|
| `String getType()` | `"TOOL"` |
| `String getActivationSlot()` | `"HAND"` |
| `boolean isPlaceable()` | false |
| `String getPermission()` | `null` (без ограничений) |
| `long getClickCooldown()` | 0 |
| `int getMaxUses()` | 0 (безлимитно) |
| `long getDuration()` | 0 (вечный) |
| `boolean isAllowedInWorld(String)` | true |
| `List<RecipeDef> getRecipes()` | пусто |

### Хуки (все — no-op)

`onRightClick`, `onLeftClick`, `onEquip`, `onUnequip`, `onKill`, `onDeath`,
`onDamageTaken`, `onDamageDealt`, `onJump`, `onMove`, `onDrop`, `onPickup`,
`onBlockBreak`, `onSlotChange`, `onSwapHand`, `onPeriodic`.

### Свои сообщения

`getUsesDepletedMessage()`, `getDurationExpiredMessage()`,
`getWorldBlockedMessage()` — `null` означает «использовать текст плагина».

### Утилиты

`ItemStack createItemStack()`, `boolean isHeldBy(Player)`,
`boolean isEquippedBy(Player)`.

---

## ItemAPI

Все методы **статические**.

### Предметы

| Метод | Что делает |
|-------|-----------|
| `ItemStack createItem(AbstractCustomItem)` | собрать предмет из описания |
| `ItemStack createItem(AbstractCustomItem, Map<Attribute,Double>)` | то же + атрибуты |
| `boolean isCustomItem(ItemStack)` | проверка, кастомный ли предмет |
| `String getCustomItemId(ItemStack)` | id из PDC |
| `boolean isHolding(Player, String)` | предмет в руке |
| `boolean isEquipped(Player, String)` | предмет надет/в руке |

### Эффекты и урон

| Метод | Что делает |
|-------|-----------|
| `void effect(Player, PotionEffectType, int seconds, int level)` | зелье (уровень 1 = обычный) |
| `void speed(Player, int seconds, int level)` | скорость |
| `void strength(Player, int seconds, int level)` | сила |
| `void vanish(Player, int seconds)` | невидимость |
| `void glow(Player, int seconds)` | свечение |
| `void heal(Player, double hearts)` | лечение (1 сердце = 2 HP) |
| `void damage(Player, double amount)` | урон себе |
| `void damageNearby(Player, double, double radius)` | урон игрокам рядом |
| `void healNearby(Player, double, double radius)` | лечение игроков рядом |

### Частицы и звук

| Метод | Что делает |
|-------|-----------|
| `void particles(Player, Particle, int count)` | частицы над игроком |
| `void particlesAt(Location, Particle, int count)` | частицы в точке |
| `void particlesCustom(Player, Particle, int, double, double, double)` | частицы со смещением |
| `void sound(Player, Sound, float volume, float pitch)` | звук игроку |
| `void soundAt(Location, Sound, float, float)` | звук всем рядом |

### Сообщения

| Метод | Что делает |
|-------|-----------|
| `void message(Player, String)` | сообщение с цветами и PAPI |
| `void title(Player, String title, String subtitle)` | заголовок |
| `void actionbar(Player, String)` | строка над хотбаром |
| `void broadcast(String)` | всем игрокам |

### Перемещение и бой

| Метод | Что делает |
|-------|-----------|
| `void teleport(Player, double x, double y, double z)` | телепорт |
| `void teleportRelative(Player, double dx, double dy, double dz)` | относительный телепорт |
| `void teleportToNearest(Player, double maxRange)` | к ближайшему игроку (в том же мире) |
| `void lightning(Location)` | молния |
| `void lightningForward(Player, int distance)` | молния по курсу |
| `void knockbackNearby(Player, double radius)` | отброс |
| `void launchNearby(Player, double power, double radius)` | подброс |
| `void setOnFire(Player, int ticks)` | поджечь |

### Инвентарь и опыт

| Метод | Что делает |
|-------|-----------|
| `void giveItem(Player, Material, int amount)` | выдать (излишек падает на землю) |
| `boolean removeItem(Player, Material, int amount)` | забрать, вернёт `true`, если всё забрато |
| `void giveExp(Player, int amount)` | опыт |
| `void giveLevels(Player, int amount)` | уровни |

---

## RecipeDef

Неизменяемый объект-описание рецепта.

| Фабрика | Что делает |
|---------|-----------|
| `RecipeDef.shaped(List<String> pattern, Map<Character,String> keys)` | крафт по сетке |
| `RecipeDef.shapeless(List<String> ingredients)` | крафт без сетки |
| `RecipeDef.furnace(String ingredient)` | в печи |
| `RecipeDef.furnace(String ingredient, float experience, int cookingTime)` | в печи с опытом и временем |
| `RecipeDef.smithing(String template, String base, String addition)` | верстак smithing |
| `RecipeDef.stonecutting(String ingredient)` | камнерез |
| `RecipeDef.builder(RecipeDef.Type)` | универсальный конструктор |

Методы: `getType()`, `getPattern()`, `getKeys()`, `getIngredients()`,
`getAmount()`, `getExperience()`, `getCookingTime()`, `describe()`.

---

## CustomCommand

| Метод | Что делает |
|-------|-----------|
| `CustomCommand(String name, String description, String usage, String permission)` | конструктор |
| `boolean execute(CommandSender, String[] args)` | **переопределить** — сама команда |
| `List<String> tabComplete(CommandSender, String[] args, int cursor)` | **переопределить** — подсказки |
| `void onRegister()` | при регистрации команды |
| `void onUnregister()` | при снятии команды |
| `boolean hasPermission(CommandSender)` | проверка права |
| `Player getTarget(CommandSender, String[], int index)` | взять игрока из аргументов |
| `int getInt(String[] args, int index, int def)` | число из аргумента |
| `double getDouble(String[] args, int index, double def)` | дробное из аргумента |
| `String joinArgs(String[], int start)` | склеить аргументы |
| `void msg(CommandSender, String)` | сообщение |
| `void title(Player, String, String)` | заголовок |
| `String colorize(String)` | `&` → `§` |
| `String message(String path, String defaultMessage)` | текст из общей системы |
| `String getPermissionMessage()` / `setPermissionMessage(String)` | текст «нет прав» |

---

## CustomPlaceholder

| Метод | Что делает |
|-------|-----------|
| `CustomPlaceholder(String identifier)` | конструктор (id в нижнем регистре) |
| `String getValue(Player player)` | **переопределить** — значение |
| `void onRegister()` / `onUnregister()` | жизненный цикл |
| `String getIdentifier()` | id плейсхолдера |
| `Main getPlugin()` | ссылка на плагин |

---

## Module

| Метод | Что делает |
|-------|-----------|
| `Module(Main plugin, String id, File folder)` | конструктор |
| `void onEnable()` | **переопределить** — включение |
| `void onDisable()` | **переопределить** — выключение |
| `void reload()` | перезагрузка модуля |
| `void registerDynamicCommand(String name, CommandExecutor)` | команда с правом `dci.<name>` |
| `void registerDynamicCommand(String name, CommandExecutor, String permission)` | команда со своим правом |
| `String getMessage(String path, String default)` | текст из общей системы |
| `void registerMessage(String path, String value)` | зарегистрировать свой текст |
| `void unregisterMessage(String path)` | снять свой текст |
| `String getConfig(String path, String def)` | значение из `config.yml` модуля |
| `int getConfigInt(String path, int def)` | число из конфига |
| `boolean getConfigBoolean(String path, boolean def)` | флаг из конфига |
| `List<String> getConfigList(String path)` | список из конфига |
| `Map<String,Object> getItem(String itemId)` | предмет из `items.yml` модуля |
| `Set<String> getAllItemIds()` | все предметы модуля |
| `File getFolder()` | папка модуля |
| `YamlConfiguration getConfig()` / `getItemsConfig()` | конфиги модуля |
| `List<String> getCommands()` / `getPermissions()` | из конфига модуля |
| `boolean isEnabled()` | включён ли |

---

## MessageManager и MessagesConfig

### MessageManager

| Метод | Что делает |
|-------|-----------|
| `String getMessage(String path, String default)` | текст по пути |
| `String getMessage(String path, String default, Player)` | текст по пути + PlaceholderAPI |
| `void registerMessage(String path, String value)` | зарегистрировать текст модуля |
| `void unregisterMessage(String path)` | снять текст |
| `Map<String,String> getRegisteredMessages()` | все зарегистрированные |

### MessagesConfig

| Метод | Что делает |
|-------|-----------|
| `void set(String path, String value)` | задать текст по своему пути |
| `String get(String path, String default)` | получить текст |
| `String getCustom(String path)` | только переопределения |
| `String getBuiltIn(String path)` | только встроенные |
| `Map<String,String> getCustom()` | все переопределения |
| `void clearCustom()` | сбросить переопределения |
| `String format(String message, String... replacements)` | подставить `{placeholder}` |
| `String colorize(String message)` | `&` → `§` |

Поля (все `public static String`, переопределяются из `items/messages.java`):
`PREFIX`, `NO_PERMISSION`, `PLAYER_NOT_FOUND`, `ITEM_NOT_FOUND`,
`UNKNOWN_COMMAND`, `ONLY_FOR_PLAYERS`, `ITEM_USES_DEPLETED`, `ITEM_EXPIRED`,
`ITEM_WORLD_BLOCKED`, `ITEM_NO_PERMISSION`, `COMMAND_NOT_FOUND`,
`COMMAND_ERROR`, `COMMAND_NO_PERMISSION`, `COMMAND_PLAYER_NOT_FOUND`,
`COMMAND_SPECIFY_PLAYER`, `CI_HEADER`, `CI_HELP_RELOAD`, `CI_HELP_VALIDATE`,
`CI_HELP_DEBUG`, `CI_HELP_BACKUP`, `RELOAD_SUCCESS`, `RELOAD_YAML`,
`RELOAD_JAVA_ITEMS`, `RELOAD_JAVA_COMMANDS`, `RELOAD_JAVA_PLACEHOLDERS`,
`RELOAD_ERROR`, `CI_MODULES`, `CI_VALIDATION_OK`, `CI_VALIDATION_PROBLEMS`,
`CI_DEBUG_USAGE`, `CI_DEBUG_ITEM_NOT_FOUND`, `CI_BACKUP_CREATED`,
`CI_BACKUP_FAILED`, `UPDATE_AVAILABLE`, `UPDATE_DOWNLOAD`,
`UPDATE_NOTIFICATION_HEADER`, `UPDATE_NOTIFICATION_TITLE`,
`UPDATE_NOTIFICATION_CURRENT`, `UPDATE_NOTIFICATION_LATEST`,
`UPDATE_NOTIFICATION_DOWNLOAD`.

---

## DatabaseManager

| Метод | Что делает |
|-------|-----------|
| `boolean connect()` / `disconnect()` / `reconnect()` | управление соединением |
| `Connection getConnection()` | соединение (MySQL — закройте его) |
| `boolean isConnected()` | статус |
| `String getType()` / `isMySql()` | какой тип БД |
| `boolean execute(String sql, Object... params)` | запрос без результата |
| `int executeUpdate(String sql, Object... params)` | число затронутых строк |
| `boolean insert(String table, Map<String,Object> data)` | вставка |
| `boolean update(String table, Map<String,Object> data, String where, Object... params)` | обновление |
| `boolean delete(String table, String where, Object... params)` | удаление |
| `int queryInt(String sql, Object... params)` | одно число |
| `String queryString(...)` | одна строка |
| `double queryDouble(...)` | одно число |
| `boolean queryBoolean(...)` | одно значение |
| `List<Map<String,Object>> queryAll(...)` | все строки |
| `Map<String,Object> queryOne(...)` | первая строка |
| `boolean increment(String table, String column, String where, Object... params)` | `+1` к счётчику |
| `boolean add(String table, String column, double amount, String where, Object... params)` | `+N` к счётчику |
| `String upsertSql(String table, String columns, String values, String updateClause)` | SQL для upsert |
| `boolean createTable(String table, String columns)` | создать таблицу |
| `boolean tableExists(String table)` | есть ли таблица |

**Async-варианты** (не блокируют главный поток):

`executeAsync`, `executeUpdateAsync`, `queryIntAsync`, `queryStringAsync`,
`queryAllAsync` — возвращают `CompletableFuture`.

---

## YamlStorage

| Метод | Что делает |
|-------|-----------|
| `YamlStorage(Main plugin, String fileName)` | конструктор (файл в `plugins/Artifex/storage/`) |
| `void reload()` | перечитать файл |
| `boolean contains(String path)` | есть ли путь |
| `String getString(String path, String def)` | строка |
| `int getInt(String path, int def)` | число |
| `boolean getBoolean(String path, boolean def)` | флаг |
| `List<String> getStringList(String path)` | список |
| `void set(String path, Object value)` | записать |
| `boolean save()` | сохранить на диск |
| `File getFile()` | файл |

---

## События

Все — в пакете `me.dcplugin.artifex.events`, все отменяемые
(`setCancelled(true)`), наследуют `CustomItemEvent`.

| Событие | Когда |
|---------|-------|
| `CustomItemUseEvent` | игрок кликнул предметом (`getUseType()`) |
| `CustomItemEquipEvent` | предмет надели / сняли (`isEquipped()`) |
| `CustomItemCraftEvent` | предмет скрафтили |
| `CustomItemKillEvent` | игрок убил кого-то с предметом |
| `CustomItemDeathEvent` | игрок умер с предметом |
| `CustomItemDamageTakenEvent` | игрок получил урон |
| `CustomItemDamageDealtEvent` | игрок нанёс урон |
| `CustomItemPeriodicEvent` | периодический хук предмета |

Подробно — в [Событиях API](13-events-api.md).

---

## Менеджеры

| Класс | Зачем |
|-------|-------|
| `EffectManager` | применение и снятие эффектов |
| `AttributeManager` | применение и снятие атрибутов |
| `ArmorSetManager` | подсчёт частей сета, бонусы |
| `MessageManager` | тексты (см. выше) |
| `ConfigManager` | `config.yml` |
| `ModuleManager` | загрузка/выгрузка модулей |
| `PlaceholderManager` | регистрация плейсхолдеров |

---

## Утилиты

| Класс | Зачем |
|-------|-------|
| `ActionParser` | `execute(Player, "effect:SPEED:10:2")` — выполнить действие |
| `ColorUtils` | `colorize(String)`, `processMessage(Player, String)`, `processTitle(...)`, `formatTime(long)` |
| `EnumCache` | `getMaterial`, `getParticle`, `getSound`, `getPotionEffect`, `getAttribute`, `getEnchantment`, `getItemFlag` — возвращают `null`, если не найдено |
| `ItemBuilder` | сборка `ItemStack` из компонентов |
| `ItemFX` | `spawnEquipEffects(plugin, player, item, isEquip)` |
| `CooldownStore` | постоянные кулдауны в `cooldowns.yml` |
| `UpdateChecker` | проверка новой версии |

---

**Страницы вики:** [Установка](01-getting-started.md) ·
[Как это работает](02-how-it-works.md) ·
[Предметы (YAML)](03-yaml-items.md) ·
[Предметы (Java)](04-java-items.md) ·
[Действия](05-actions.md) ·
[Триггеры](06-triggers.md) ·
[Рецепты](07-recipes.md) ·
[Команды](08-commands.md) ·
[Плейсхолдеры](09-placeholders.md) ·
[Модули](10-modules.md) ·
[Сообщения](11-messages.md) ·
[База данных](12-database.md) ·
[События API](13-events-api.md) ·
[Конфигурация](14-configuration.md) ·
[Права](15-permissions.md) ·
[Примеры](16-examples.md) ·
[Решение проблем](17-troubleshooting.md) ·
[Справочник API](18-api-reference.md) ·
[⬆️ Начало](README.md)
