# Предметы в Java

## Что ты узнаешь

- когда YAML не хватает и предмет нужно писать на Java;
- как это работает технически: файл `.java` в папке `items/`, команда `/ci reload`, компиляция прямо на сервере;
- полный рабочий пример класса, который можно скопировать и скомпилировать;
- все методы `AbstractCustomItem`: что каждый делает, когда вызывается и как выглядит;
- вспомогательный класс `ItemAPI` — короткие методы для эффектов, частиц, звуков, урона и прочего;
- как объявить рецепт крафта через `RecipeDef`;
- как пользоваться общей системой сообщений, чтобы администратор мог переопределить тексты;
- типичные ошибки: почему предмет не загрузился и почему код упал.

## Предусловия

- Плагин Artifex установлен — см. [Установка](01-getting-started.md).
- Вы понимаете, как устроена папка плагина и как выполняется `/ci reload` — см. [Как это работает](02-how-it-works.md) и [Команды](08-commands.md).
- Вы прочитали [Предметы в YAML](03-yaml-items.md) и знаете, что умеет YAML.
- На сервере установлен **JDK 21** (Java Development Kit), а не только JRE. Компилятор нужен плагину, чтобы собрать ваш `.java` в байт-код. Если стоит только JRE, в консоль уйдёт `JavaCompiler not found! JDK required.`
- Базовые знания Java: классы, `@Override`, `List.of(...)`. Если Java для вас новый — сначала сделайте предмет в YAML и посмотрите [примеры](16-examples.md).

## Содержание

- [Когда нужен Java](#когда-нужен-java)
- [Как это работает](#как-это-работает)
- [Правила для файлов](#правила-для-файлов)
- [Полный рабочий пример](#полный-рабочий-пример)
- [Обязательные методы](#обязательные-методы)
- [Внешний вид](#внешний-вид)
- [Поведение](#поведение)
- [Сообщения предмета](#сообщения-предмета)
- [Хуки](#хуки)
- [Рецепты](#рецепты)
- [Вспомогательные методы](#вспомогательные-методы)
- [Класс ItemAPI](#класс-itemapi)
- [Система сообщений](#система-сообщений)
- [Подводные камни](#подводные-камни)

## Когда нужен Java

YAML покрывает большинство задач: эффекты, атрибуты, клики, триггеры, рецепты. Java нужен там, где нужна **логика** — условия, которые зависят от состояния.

| Задача | YAML | Java |
|--------|------|------|
| Предмет с эффектами и атрибутами | Да | Да |
| Реакция на клик, прыжок, убийство | Да | Да |
| Если у игрока меньше 10 сердец — выдать щит | Нет | Да |
| Счётчик убийств, который хранится в базе | Нет | Да |
| Своя команда `/shop`, `/kit` | Нет | Да |
| Плейсхолдер `%dci_storm_kills%` для другого плагина | Нет | Да |
| Предмет, который меняет поведение по фазе (день/ночь) | Нет | Да |

Правило простое: **если условие «если…то…» можно записать в YAML — пишите в YAML.** Java-предметы сложнее поддерживать, и ошибка в них видна только в логах сервера.

## Как это работает

Плагин умеет **компилировать Java прямо во время работы сервера**. Никакой IDE, никакой сборки `.jar` вручную не нужно.

Порядок действий:

1. Кладёте файл `.java` в `plugins/Artifex/items/`.
2. Выполняете `/ci reload`.
3. Плагин находит файл, компилирует его в байт-код (`.class`) в памяти и загружает класс.
4. Класс, который наследует `AbstractCustomItem`, регистрируется как предмет с ID из `getId()`.

В консоли вы увидите:

```text
[JavaCompiler] Found 1 .java files
[JavaCompiler] Compiled: 1, Skipped: 0 (312ms)
[API] Loaded: 1 items, 0 commands, 0 placeholders, 0 modules
```

Что важно знать:

| Тема | Подробности |
|------|-------------|
| Нужен JDK | На сервере должен стоять JDK 21. На JRE компиляции не будет |
| Класс переименовывается | Имя класса **не обязано** совпадать с именем файла — компилятор переименует класс сам (см. ниже) |
| Кэш | Файлы, которые не менялись, не компилируются заново (сверка по MD5 в `cache/file_hashes.properties`) |
| Перезагрузка | `/ci reload` пересобирает и YAML, и Java-предметы |
| Безопасность | Кто может писать файлы в `items/` — тот может выполнять произвольный код на сервере. Если к папке есть доступ у посторонних, выключите компиляцию: `java-compilation: false` в `config.yml` |
| Опасные вызовы | Плагин **предупреждает** в логе про `System.exit`, `Runtime.getRuntime().exec`, `ProcessBuilder`, `.setAccessible(` — но не блокирует их |

### Как дать предмет игроку

В текущей версии встроенной команды выдачи нет: `/ci` умеет только `reload`, `validate`, `debug` и `backup`. Java-предмет выдают:

- модуль-магазин (см. [Модули](10-modules.md));
- ваша собственная команда — готовый образец лежит в `samples/EXAMPLE-give-command.java` (класс наследует `CustomCommand` и умеет выдавать и YAML-, и Java-предметы);
- любой другой плагин через служебные данные предмета.

## Правила для файлов

| Правило | Подробности |
|---------|--------------|
| Расширение | `.java` |
| Папка | `items/` — можно и во вложенных подпапках |
| Класс | Должен быть `public` и наследовать `AbstractCustomItem` |
| Конструктор | Нужен **публичный конструктор без аргументов** — плагин создаёт предмет через `new` |
| Имя класса и файла | Не обязаны совпадать: компилятор сам переименует класс. Например `storm-blade.java` → класс `storm_bladeItem` |
| Перекрёстные ссылки | Если один файл использует класс из другого, это работает: компилятор переименовывает упоминания во всех файлах сразу |
| `package` | Свою строку `package` писать не нужно — компилятор удаляет её и добавляет свою |
| Импорты | `AbstractCustomItem`, `ItemAPI`, `CustomCommand` и `CustomPlaceholder` добавляются автоматически, если их нет |
| Папки `EXAMPLE-*`, `EXAMPLES`, `_template` | Не компилируются — это справочные материалы |
| Файлы, начинающиеся с `Abstract` или `Custom` | Пропускаются компилятором |
| Размер | Максимум 512 КБ на файл и 300 файлов всего |

Если класс не `public` или нет пустого конструктора — плагин не сможет создать предмет и напишет об этом в лог.

## Полный рабочий пример

Скопируйте этот файл в `plugins/Artifex/items/storm-blade.java` и выполните `/ci reload`.

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.api.RecipeDef;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/**
 * Storm Blade — полный пример Java-предмета.
 *
 * Что показано:
 *   - обязательные методы (getId, getDisplayName, getMaterial);
 *   - внешний вид (getLore, isUnbreakable, isGlowing);
 *   - поведение (getType, getActivationSlot, getPermission, getClickCooldown, getMaxUses);
 *   - ограничения (isAllowedInWorld, getDuration);
 *   - свои сообщения (getUsesDepletedMessage и другие);
 *   - рецепт крафта (getRecipes + RecipeDef);
 *   - хуки кликов, боя и разрушения блоков;
 *   - периодический хук (getPeriodicInterval + onPeriodic).
 */
public class StormBlade extends AbstractCustomItem {

    // ===== ОБЯЗАТЕЛЬНЫЕ МЕТОДЫ =====

    /** Уникальный ID. Используется в командах, рецептах и правах. */
    @Override
    public String getId() {
        return "storm-blade";
    }

    /** Отображаемое имя. Поддерживает цвета & и §. */
    @Override
    public String getDisplayName() {
        return "&b⚡ &fStorm Blade";
    }

    /** Материал предмета. */
    @Override
    public Material getMaterial() {
        return Material.NETHERITE_SWORD;
    }

    // ===== ВНЕШНИЙ ВИД =====

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Клинок, в котором живёт гроза.",
            "",
            " &bПКМ: &fудар молнии",
            " &bЛКМ: &fускорение",
            ""
        );
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    // ===== ПОВЕДЕНИЕ =====

    /** Тип: RUNE, TOOL, ARMOR, CONSUMABLE. */
    @Override
    public String getType() {
        return "TOOL";
    }

    /** Слот, в котором предмет работает. */
    @Override
    public String getActivationSlot() {
        return "HAND";
    }

    /** Право, без которого предмет нельзя использовать. null — без ограничения. */
    @Override
    public String getPermission() {
        return "customitems.storm-blade";
    }

    /** Кулдаун между кликами в миллисекундах. */
    @Override
    public long getClickCooldown() {
        return 1500;
    }

    /** Сколько раз предмет можно использовать. 0 — без ограничения. */
    @Override
    public int getMaxUses() {
        return 50;
    }

    /** Время жизни в секундах. 0 — вечный предмет. */
    @Override
    public long getDuration() {
        return 0;
    }

    /** Разрешён ли предмет в этом мире. */
    @Override
    public boolean isAllowedInWorld(String worldName) {
        return !worldName.equals("pvp_arena");
    }

    // ===== СВОИ СООБЩЕНИЯ =====

    @Override
    public String getUsesDepletedMessage() {
        return "&cЗаряд клинка иссяк!";
    }

    @Override
    public String getDurationExpiredMessage() {
        return "&cКлинок рассеялся!";
    }

    @Override
    public String getWorldBlockedMessage() {
        return "&cКлинок нельзя использовать в этом мире!";
    }

    // ===== РЕЦЕПТ =====

    @Override
    public List<RecipeDef> getRecipes() {
        Map<Character, String> keys = Map.of(
            'N', "NETHERITE_INGOT",
            'S', "STICK"
        );
        return List.of(
            RecipeDef.shaped(List.of(
                " N ",
                " S ",
                " N "
            ), keys)
        );
    }

    // ===== ХУКИ =====

    /** ПКМ. */
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        ItemAPI.lightningForward(player, 12);
        ItemAPI.particles(player, Particle.ELECTRIC_SPARK, 60);
        ItemAPI.sound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        ItemAPI.title(player, "&b⚡ ГРОЗА!", "&7Небо отвечает");
    }

    /** ЛКМ. */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        ItemAPI.effect(player, PotionEffectType.SPEED, 8, 2);
        ItemAPI.particles(player, Particle.CLOUD, 30);
    }

    /** Игрок ударил клинком кого-то. */
    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        if (event.getEntity() instanceof LivingEntity target) {
            target.setFireTicks(40);
        }
        event.setDamage(event.getDamage() + 2.0);
    }

    /** Игрок убил игрока клинком. */
    @Override
    public void onKill(Player killer, Player victim) {
        ItemAPI.heal(killer, 2);
        ItemAPI.particlesAt(victim.getLocation(), Particle.ELECTRIC_SPARK, 40);
    }

    /** Игрок сломал блок клинком. */
    @Override
    public void onBlockBreak(Player player, BlockBreakEvent event) {
        ItemAPI.particlesAt(
            event.getBlock().getLocation().add(0.5, 1, 0.5),
            Particle.ELECTRIC_SPARK, 10
        );
    }

    /** Периодический хук: вызывается каждые getPeriodicInterval() тиков. */
    @Override
    public long getPeriodicInterval() {
        return 40; // 2 секунды
    }

    @Override
    public void onPeriodic(Player player) {
        ItemAPI.particles(player, Particle.ELECTRIC_SPARK, 4);
    }
}
```

## Обязательные методы

Эти три метода нужно переопределить — иначе класс не скомпилируется.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getId()` | `String` | Уникальный ID предмета. Он же используется в командах и рецептах |
| `getDisplayName()` | `String` | Имя, которое видит игрок. Поддерживает цвета `&` и `§` |
| `getMaterial()` | `Material` | Материал из Minecraft, например `Material.NETHERITE_SWORD` |

```java
@Override
public String getId() { return "storm-blade"; }

@Override
public String getDisplayName() { return "&b⚡ &fStorm Blade"; }

@Override
public Material getMaterial() { return Material.NETHERITE_SWORD; }
```

## Внешний вид

| Метод | По умолчанию | Что делает |
|-------|---------------|------------|
| `getLore()` | `List.of()` (пусто) | Описание предмета — список строк |
| `getAmount()` | `1` | Размер стака при выдаче |
| `getItemModel()` | `null` | Модель ресурс-пака (Minecraft 1.21.2+). Например `minecraft:item/netherite_sword` |
| `getCustomModelData()` | `-1` | Старое число модели для ресурс-паков. Работает, если `getItemModel()` вернул `null` |
| `isUnbreakable()` | `false` | Предмет нельзя сломать |
| `isGlowing()` | `false` | Предмет переливается магическим светом |

```java
@Override
public List<String> getLore() {
    return List.of("", " &7Клинок, в котором живёт гроза.", "");
}

@Override
public int getAmount() { return 1; }

@Override
public String getItemModel() { return "minecraft:item/netherite_sword"; }

@Override
public int getCustomModelData() { return -1; }

@Override
public boolean isUnbreakable() { return true; }

@Override
public boolean isGlowing() { return true; }
```

Если `getItemModel()` вернул непустую строку, она записывается и в служебные данные предмета — плагин сможет её найти.

## Поведение

| Метод | По умолчанию | Что делает |
|-------|---------------|------------|
| `getType()` | `"TOOL"` | Тип предмета: `RUNE`, `TOOL`, `ARMOR`, `CONSUMABLE` |
| `getActivationSlot()` | `"HAND"` | Слот, в котором предмет работает: `HAND`, `OFFHAND`, `HEAD`, `CHEST`, `LEGS`, `FEET` |
| `isPlaceable()` | `false` | Можно ли поставить предмет как блок |
| `getPermission()` | `null` | Право на использование. Без права клик отменяется и показывается сообщение |
| `getClickCooldown()` | `0` | Кулдаун между кликами в миллисекундах |
| `getMaxUses()` | `0` | Сколько раз можно использовать. `0` — без ограничения. Счётчик хранится в самом предмете |
| `getDuration()` | `0` | Время жизни в секундах. `0` — вечный. Отсчёт с момента попадания в инвентарь |
| `isAllowedInWorld(String worldName)` | `true` | Разрешён ли предмет в этом мире. Запрещённый предмет удаляется из инвентаря |
| `getRecipes()` | `List.of()` | Рецепты крафта (см. раздел [Рецепты](#рецепты)) |

Два отличия от YAML, о которых стоит знать:

| Механика | YAML | Java |
|----------|------|------|
| Кулдаун | Счётчик на игрока **и на предмет** — у разных предметов разные таймеры | Таймер **один на игрока**: клик по другому Java-предмету с кулдауном тоже может быть отклонён |
| Значения по умолчанию | `max-uses: -1`, `duration: -1` | `getMaxUses() → 0`, `getDuration() → 0` (в обоих случаях ноль означает «без ограничения») |

Проверка кулдауна происходит **до** события `CustomItemUseEvent`: пока кулдаун не истёк, событие вообще не вызывается.

```java
@Override
public String getType() { return "TOOL"; }

@Override
public String getActivationSlot() { return "HAND"; }

@Override
public boolean isPlaceable() { return false; }

@Override
public String getPermission() { return "customitems.storm-blade"; }

@Override
public long getClickCooldown() { return 1500; }

@Override
public int getMaxUses() { return 50; }

@Override
public long getDuration() { return 3600; } // час

@Override
public boolean isAllowedInWorld(String worldName) {
    return !worldName.equals("pvp_arena");
}
```

## Сообщения предмета

Три метода переопределяют стандартные сообщения плагина. Если вернуть `null` — покажется текст по умолчанию.

| Метод | Когда показывается | Текст по умолчанию |
|-------|--------------------|--------------------|
| `getUsesDepletedMessage()` | Заряды кончились | «Предмет закончился» |
| `getDurationExpiredMessage()` | Истекло время жизни | «Срок предмета истёк» |
| `getWorldBlockedMessage()` | Предмет запрещён в мире | «Предмет запрещён в этом мире» |

```java
@Override
public String getUsesDepletedMessage() {
    return "&cЗаряд клинка иссяк!";
}

@Override
public String getDurationExpiredMessage() {
    return "&cКлинок рассеялся!";
}

@Override
public String getWorldBlockedMessage() {
    return "&cКлинок нельзя использовать в этом мире!";
}
```

## Хуки

Хуки — это методы, которые плагин вызывает в определённый момент. Все они по умолчанию ничего не делают, поэтому переопределяйте только нужные.

### Клики

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `onRightClick(PlayerInteractEvent event, Player player)` | ПКМ по предмету | событие Bukkit и игрок |
| `onLeftClick(PlayerInteractEvent event, Player player)` | ЛКМ по предмету | событие Bukkit и игрок |

При клике по мобу вызывается **не** `onLeftClick`, а `onDamageDealt` — атака и «клик по воздуху» — разные вещи.

```java
@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    ItemAPI.effect(player, PotionEffectType.SPEED, 10, 2);
    ItemAPI.particles(player, Particle.ELECTRIC_SPARK, 40);
}
```

### Экипировка

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `onEquip(Player player)` | Предмет занял свой слот активации | игрок |
| `onUnequip(Player player)` | Предмет покинул слот активации | игрок |

Эти хуки вызывает глобальная проверка экипировки — она срабатывает, когда содержимое инвентаря изменилось, поэтому между надеванием и срабатыванием может пройти до 150 мс.

```java
@Override
public void onEquip(Player player) {
    ItemAPI.message(player, "&b⚡ Гроза пробуждается!");
}

@Override
public void onUnequip(Player player) {
    player.removePotionEffect(PotionEffectType.SPEED);
}
```

### Бой

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `onDamageDealt(EntityDamageByEntityEvent event, Player player)` | Игрок нанёс урон предметом из руки | событие урона и атакующий |
| `onDamageTaken(EntityDamageEvent event, Player player)` | Игрок получил урон, а предмет в руке или одет | событие урона и пострадавший |
| `onKill(Player killer, Player victim)` | Игрок убил игрока предметом из руки | убийца и жертва |
| `onDeath(Player player, PlayerDeathEvent event)` | Игрок умер, а предмет был в руке или одет | игрок и событие смерти |

`onKill` срабатывает **только** при убийстве игрока (событие `PlayerDeathEvent`). Убийство моба этим хуком не ловится.

`onDamageDealt` срабатывает и когда урон наносит снаряд (стрела, снежок), выпущенный игроком.

```java
@Override
public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
    event.setDamage(event.getDamage() + 2.0);
}

@Override
public void onKill(Player killer, Player victim) {
    ItemAPI.heal(killer, 2);
}
```

### Движение

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `onMove(PlayerMoveEvent event, Player player)` | Игрок сменил позицию (именно позицию, а не поворот камеры) | событие движения и игрок |
| `onJump(Player player)` | Игрок поднялся больше чем на 0.25 блока | игрок |

`onMove` вызывается часто — не делайте в нём тяжёлых операций.

```java
@Override
public void onJump(Player player) {
    ItemAPI.effect(player, PotionEffectType.SPEED, 3, 1);
}
```

### Предмет и слоты

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `onDrop(Player player, PlayerDropItemEvent event)` | Игрок выбросил предмет | игрок и событие броска |
| `onPickup(Player player)` | Игрок подобрал предмет | игрок |
| `onBlockBreak(Player player, BlockBreakEvent event)` | Игрок сломал блок предметом из руки | игрок и событие разрушения |
| `onSlotChange(PlayerItemHeldEvent event, Player player)` | Игрок переключил слот хотбара | событие и игрок |
| `onSwapHand(PlayerSwapHandItemsEvent event, Player player)` | Игрок нажал F (смена рук) | событие и игрок |

> Важно: хук `onPickup` объявлен в API, но **в текущей версии плагин его не вызывает** — обработчика подбора для Java-предметов нет. Если вам нужно reacts на подбор, используйте YAML-предмет с триггером `on_pickup`.

```java
@Override
public void onBlockBreak(Player player, BlockBreakEvent event) {
    ItemAPI.particlesAt(event.getBlock().getLocation(), Particle.ELECTRIC_SPARK, 10);
}
```

### Периодический хук

| Хук | Когда вызывается | Параметры |
|-----|------------------|-----------|
| `getPeriodicInterval()` | — (возвращает интервал) | — |
| `onPeriodic(Player player)` | Каждые `getPeriodicInterval()` тиков, пока предмет в слоте активации | игрок |

Значение `0` (по умолчанию) отключает периодический хук. 20 тиков = 1 секунда.

```java
@Override
public long getPeriodicInterval() { return 40; } // каждые 2 секунды

@Override
public void onPeriodic(Player player) {
    ItemAPI.particles(player, Particle.ELECTRIC_SPARK, 4);
}
```

## Рецепты

Рецепты Java-предмета возвращаются из `getRecipes()` в виде списка `RecipeDef`.

| Вызов | Что делает |
|-------|------------|
| `RecipeDef.shaped(pattern, keys)` | Рецепт по форме. `pattern` — строки одинаковой длины (1–3), пробел = пустая ячейка. `keys` — соответствие символа и ингредиента |
| `RecipeDef.shaped(pattern, keys, amount)` | То же, но с количеством результата |
| `RecipeDef.shapeless(ingredients...)` | Рецепт без формы: просто список ингредиентов |
| `RecipeDef.shapeless(ingredients)` | То же, списоком |
| `RecipeDef.shapeless(ingredients, amount)` | То же, с количеством результата |
| `RecipeDef.furnace(ingredient)` | Переплавка. По умолчанию 200 тиков и 0 опыта |
| `RecipeDef.furnace(ingredient, experience, cookingTime)` | Переплавка с опытом и временем |
| `RecipeDef.smithing(template, base, addition)` | Кузнечный стол |
| `RecipeDef.stonecutting(ingredient)` | Камнерез |

Ингредиентом может быть название материала (`DIAMOND`) или ID другого кастомного предмета — YAML или Java.

```java
@Override
public List<RecipeDef> getRecipes() {
    Map<Character, String> keys = Map.of(
        'N', "NETHERITE_INGOT",
        'S', "STICK"
    );
    return List.of(
        RecipeDef.shaped(List.of(" N ", " S ", " N "), keys),
        RecipeDef.shapeless("DIAMOND", "DIAMOND", "STICK"),
        RecipeDef.furnace("IRON_INGOT", 0.7f, 200)
    );
}
```

## Вспомогательные методы

Эти методы уже есть в `AbstractCustomItem` — переопределять их не нужно.

| Метод | Что делает |
|-------|------------|
| `createItemStack()` | Создаёт `ItemStack` из свойств этого предмета (с ID в служебных данных) |
| `isHeldBy(Player player)` | Держит ли игрок этот предмет в руке |
| `isEquippedBy(Player player)` | Находится ли этот предмет где-либо в инвентаре (включая броню) |

```java
@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    if (isHeldBy(player)) {
        ItemAPI.particles(player, Particle.ELECTRIC_SPARK, 20);
    }
}
```

## Класс ItemAPI

`ItemAPI` — набор статических (не требуют экземпляра) помощников. Он экономит код и закрывает крайние случаи вроде «не переполнить здоровье».

### Эффекты и лечение

| Метод | Что делает |
|-------|------------|
| `effect(player, type, duration, level)` | Зелье. `duration` — в секундах, `level` — 1 это обычное, 2 это усиленное |
| `speed(player, duration, level)` | Скорость |
| `strength(player, duration, level)` | Сила |
| `vanish(player, seconds)` | Невидимость |
| `glow(player, seconds)` | Свечение (контуры) |
| `heal(player, amount)` | Лечение. `amount` считается в **сердцах** (иконках) и умножается на 2: `heal(player, 5)` = +10 единиц здоровья |
| `healNearby(player, amount, radius)` | Лечит всех игроков в радиусе |
| `damage(player, amount)` | Урон самому игроку. `amount` — в единицах здоровья |
| `damageNearby(player, amount, radius)` | Урон всем игрокам в радиусе |

> Не путайте единицы: в YAML-действии `heal:10` — это +10 единиц здоровья, а в `ItemAPI.heal(player, 10)` — +10 сердец (20 единиц здоровья).

### Частицы и звуки

| Метод | Что делает |
|-------|------------|
| `particles(player, particle, count)` | Частицы над игроком |
| `particlesAt(location, particle, count)` | Частицы в точке |
| `particlesCustom(player, particle, count, offsetX, offsetY, offsetZ)` | Частицы с разбросом |
| `sound(player, sound, volume, pitch)` | Звук для игрока |
| `soundAt(location, sound, volume, pitch)` | Звук в точке (слышат все рядом) |

### Сообщения

| Метод | Что делает |
|-------|------------|
| `message(player, text)` | Сообщение в чат. Поддерживает цвета и плейсхолдеры PlaceholderAPI |
| `title(player, title, subtitle)` | Крупный заголовок по центру экрана |
| `actionbar(player, text)` | Текст над хотбаром |
| `broadcast(text)` | Сообщение всем игрокам онлайн |

### Перемещение и бой

| Метод | Что делает |
|-------|------------|
| `teleport(player, x, y, z)` | Телепорт на координаты (в текущем мире) |
| `teleportRelative(player, dx, dy, dz)` | Телепорт относительно текущей позиции |
| `teleportToNearest(player, maxRange)` | Телепорт к ближайшему игроку в пределах диапазона |
| `lightningForward(player, distance)` | Молния в точке перед игроком |
| `lightning(location)` | Молния в точке |
| `knockbackNearby(player, radius)` | Отбрасывает игроков в радиусе |
| `launchNearby(player, power, radius)` | Подбрасывает игроков в радиусе |

### Предметы, опыт и огонь

| Метод | Что делает |
|-------|------------|
| `giveItem(player, material, amount)` | Выдать предмет. Если инвентарь полон — остаток падает на землю |
| `removeItem(player, material, amount)` | Забрать предмет. Возвращает `true`, если убрано всё нужное количество |
| `giveExp(player, amount)` | Дать очки опыта |
| `giveLevels(player, amount)` | Дать уровни |
| `setOnFire(player, ticks)` | Поджечь игрока |
| `setNearbyOnFire(player, ticks, radius)` | Поджечь игроков в радиусе |

### Проверки

| Метод | Что делает |
|-------|------------|
| `isCustomItem(itemStack)` | Это кастомный предмет? |
| `getCustomItemId(itemStack)` | ID кастомного предмета или `null` |
| `isHolding(player, itemId)` | Держит ли игрок предмет с таким ID |
| `isEquipped(player, itemId)` | Есть ли такой предмет в инвентаре (включая броню) |
| `createItem(item)` | Создать `ItemStack` из Java-предмета |
| `createItem(item, attributes)` | То же, но с модификаторами атрибутов |

```java
@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    ItemAPI.effect(player, PotionEffectType.STRENGTH, 10, 2);
    ItemAPI.particles(player, Particle.FLAME, 50);
    ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
    ItemAPI.title(player, "&6СИЛА!", "&7Вам выдано усиление");
    ItemAPI.actionbar(player, "&7Заряды: &f" + getMaxUses());
}
```

## Система сообщений

Все тексты, которые видит игрок, живут в **одном месте** — файле `plugins/Artifex/items/messages.java`. Подробнее — в разделе [Сообщения](11-messages.md).

Java-предмет может пользоваться этой системой: зарегистрировать свой текст под своим путём и затем брать его через `getMessage()`.

```java
import me.dcplugin.artifex.Main;

// 1. Зарегистрировать свой текст (обычно в конструкторе или в статическом load()).
Main plugin = Main.getInstance();
if (plugin != null) {
    plugin.getMessageManager().registerMessage("storm-blade.activated", "&b⚡ Гроза пробуждается!");
}

// 2. Получить текст (с подстановкой плейсхолдеров PlaceholderAPI).
String text = plugin.getMessageManager()
        .getMessage("storm-blade.activated", "&b⚡ Гроза пробуждается!", player);
ItemAPI.message(player, text);
```

Приоритет источников текста:

1. Переопределение администратора из `items/messages.java`.
2. Ваша регистрация через `registerMessage(...)`.
3. Встроенный текст плагина.
4. Значение по умолчанию, которое вы передали в `getMessage(...)`.

То есть администратор может переписать ваш текст, не трогая ваш код.

## Подводные камни

| Проблема | Причина | Что делать |
|----------|---------|-----------|
| `error: class X is public, should be declared in a file named X.java` | Класс не `public` или не совпадает с переименованным файлом | Сделайте класс `public` и не переименовывайте его вручную — компилятор переименует сам |
| В консоли `JavaCompiler not found! JDK required.` | На сервере JRE, а не JDK | Поставьте JDK 21 |
| `error: cannot find symbol` на `AbstractCustomItem` | Нет импорта | Добавьте `import me.dcplugin.artifex.api.AbstractCustomItem;` (обычно компилятор добавляет сам) |
| Предмет загрузился, но не выдаётся | Встроенной команды выдачи нет | Используйте модуль-магазин или свою команду (образец — `samples/EXAMPLE-give-command.java`) |
| `onPickup` не вызывается | В текущей версии плагин не вызывает этот хук | Используйте YAML-предмет с триггером `on_pickup` |
| Предмет работает, но эффекты не наносятся | Предмет не в слоте активации | Проверьте `getActivationSlot()` |
| Исключение в логе: `Not on main thread` | Bukkit-API вызван из асинхронного потока | Перенесите работу в основной поток через `Bukkit.getScheduler().runTask(...)` |
| Кулдаун блокирует другой предмет | У Java-предметов таймер один на игрока | Уменьшите `getClickCooldown()` или разнесите предметы по разным ID |
| Два Java-предмета с одинаковым `getId()` | ID должен быть уникальным | Дайте каждому предмету свой ID |
| `getMaxUses()` списывает заряд даже после отмены события | Счётчик уменьшается до вызова события | Учитывайте это в логике; проверяйте заряды вручную |
| Предмет с тем же ID, что и YAML-предмет | ID YAML и Java-предметов должны различаться | Переименуйте один из них |
| Ошибка при компиляции после правки | Файл не перезаписан (BOM, неверная кодировка) | Сохраняйте файл в UTF-8 без BOM |

Общее правило: **никогда не вызывайте Bukkit-API из асинхронного потока**. Если вам нужно что-то тяжёлое (чтение файла, запрос в базу), делайте это в отдельном потоке, а результат возвращайте в основной через планировщик.

## Смотрите также

- [Предметы в YAML](03-yaml-items.md) — проще, и в большинстве случаев достаточно.
- [Действия](05-actions.md) и [Триггеры](06-triggers.md) — что доступно без Java.
- [Рецепты](07-recipes.md) — крафт в YAML и в Java.
- [События и API](13-events-api.md) — как реагировать на предметы из своего плагина.
- [Справочник API](18-api-reference.md) — все классы и методы списком.
- [Содержание вики](README.md).
