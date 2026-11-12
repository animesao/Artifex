# События и API

## Что ты узнаешь

- зачем плагин отдаёт наружу Bukkit-события и как их слушать из своего плагина;
- полный список событий: когда они срабатывают, что можно изменить и можно ли их отменить;
- точные конструкторы каждого события — с YAML-предметом и с Java-предметом;
- готовый пример слушателя, который запрещает один конкретный предмет;
- как отмена события влияет на Java-предмет: хук `onRightClick` просто не вызовется;
- почему в одном событии может быть как YAML-, так и Java-предмет.

## Предусловия

- Плагин Artifex установлен — см. [Установка](01-getting-started.md).
- Вы умеете писать плагины на Bukkit/Paper и знаете, что такое `Listener` и `@EventHandler` — иначе сначала посмотрите [События и API](13-events-api.md) на любом Bukkit-гайде.
- Вы прочитали [Предметы в YAML](03-yaml-items.md) и [Предметы в Java](04-java-items.md) — события относятся к обоим видам предметов.
- Сервер работает на Paper/Spigot версии 1.21, Java 21 — см. [Установка](01-getting-started.md).
- Общий список страниц — в [Содержании вики](README.md).

## Содержание

- [Зачем нужны события](#зачем-нужны-события)
- [Базовый класс CustomItemEvent](#базовый-класс-customitemevent)
- [Сводная таблица событий](#сводная-таблица-событий)
- [CustomItemUseEvent](#customitemuseevent)
- [CustomItemEquipEvent](#customitemequipevent)
- [CustomItemKillEvent](#customitemkillevent)
- [CustomItemDeathEvent](#customitemdeathevent)
- [CustomItemDamageDealtEvent](#customitemdamagedealtevent)
- [CustomItemDamageTakenEvent](#customitemdamagetakenevent)
- [CustomItemCraftEvent](#customitemcraftevent)
- [CustomItemPeriodicEvent](#customitemperiodicevent)
- [Готовый пример слушателя](#готовый-пример-слушателя)
- [Подключение к своему плагину](#подключение-к-своему-плагину)
- [Как отмена события влияет на предмет](#как-отмена-события-влияет-на-предмет)
- [Порядок вызовов](#порядок-вызовов)
- [Подводные камни](#подводные-камни)

## Зачем нужны события

Плагин умеет отменять обычные Bukkit-события — например, запретить установку блока кастомным предметом. Но как **вашему** плагину узнать, что игрок именно что-то сделал с кастомным предметом?

Для этого Artifex отдаёт наружу собственные Bukkit-события. Ваш плагин подписывается на них и решает, что делать:

- запретить определённый предмет в своей зоне;
- посчитать использования в своей статистике;
- выдать дополнительную награду;
- изменить урон по кастомному оружию;
- запретить крафт определённых предметов в своей зоне.

Главное свойство: **все эти события отменяемые**. Отменив событие, вы говорите плагину «не выполняй стандартное поведение предмета».

| Правило | Значение |
|---------|---------|
| Базовый класс | Все события наследуют `me.dcplugin.artifex.events.CustomItemEvent` |
| Поток | События вызываются в основном потоке сервера, синхронно |
| Предмет | В событии может быть **и YAML-предмет, и Java-предмет** — проверяйте `getCustomItem()` и `getJavaItem()` |
| Отмена | `event.setCancelled(true)` отменяет стандартное поведение |

## Базовый класс CustomItemEvent

`CustomItemEvent` — абстрактный класс, от которого наследуют все остальные события. Он даёт четыре общих метода:

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getPlayer()` | `Player` | Игрок, с которым связано событие. Для `CustomItemKillEvent` это убийца |
| `getCustomItem()` | `CustomItem` | Модель YAML-предмета. `null`, если предмет написан на Java |
| `getJavaItem()` | `AbstractCustomItem` | Java-предмет. `null`, если предмет описан в YAML |
| `getItemId()` | `String` | ID предмета в любом случае — YAML или Java. Удобно, когда тип не важен |
| `isJavaItem()` | `boolean` | `true`, если это Java-предмет |

Универсальная проверка для любого слушателя:

```java
String id = event.getItemId();
if (id == null) return;

if (event.isJavaItem()) {
    AbstractCustomItem javaItem = event.getJavaItem();   // Java-предмет
} else {
    CustomItem yamlItem = event.getCustomItem();         // YAML-предмет
}
```

## Сводная таблица событий

| Событие | Когда срабатывает | Отменяемое | Что можно изменить |
|---------|-------------------|------------|--------------------|
| [`CustomItemUseEvent`](#customitemuseevent) | Клик по кастомному предмету (ЛКМ или ПКМ) | Да | Клик отменяется целиком: ни действия, ни триггеры, ни хуки |
| [`CustomItemEquipEvent`](#customitemequipevent) | Предмет надет или снят | Да | Отменяются эффекты, частицы, сообщения и хуки `onEquip` / `onUnequip` |
| [`CustomItemKillEvent`](#customitemkillevent) | Игрок убил игрока кастомным предметом | Да | Триггеры и хук `onKill` не выполняются |
| [`CustomItemDeathEvent`](#customitemdeathevent) | Игрок умер с кастомным предметом | Да | Триггеры и хук `onDeath` не выполняются |
| [`CustomItemDamageDealtEvent`](#customitemdamagedealtevent) | Игрок нанёс урон кастомным предметом | Да | `setDamage(...)` — величина урона |
| [`CustomItemDamageTakenEvent`](#customitemdamagetakenevent) | Игрок получил урон с кастомным предметом | Да | `setDamage(...)` — величина урона |
| [`CustomItemCraftEvent`](#customitemcraftevent) | Крафт кастомного предмета | Да | `setResult(...)` — что именно получит игрок |
| [`CustomItemPeriodicEvent`](#customitemperiodicevent) | Периодический хук Java-предмета | Да | Вызов `onPeriodic` пропускается |

Общее для всех: конструктор всегда принимает игрока и предмет — в двух вариантах, для YAML и для Java.

## CustomItemUseEvent

Срабатывает при клике по кастомному предмету. Это самое часто используемое событие.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getUseType()` | `UseType` | `RIGHT_CLICK` или `LEFT_CLICK` |
| `isRightClick()` | `boolean` | `true`, если ПКМ |
| `isLeftClick()` | `boolean` | `true`, если ЛКМ |

Конструкторы:

```java
// YAML-предмет
public CustomItemUseEvent(Player player, CustomItem customItem, UseType useType)

// Java-предмет
public CustomItemUseEvent(Player player, AbstractCustomItem javaItem, UseType useType)
```

Пример — запрещаем админский меч в обычном мире:

```java
@EventHandler
public void onUse(CustomItemUseEvent event) {
    if ("admin-blade".equals(event.getItemId())) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cЭтот предмет недоступен.");
    }
}
```

## CustomItemEquipEvent

Срабатывает, когда предмет занял или покинул свой слот активации.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `isEquip()` | `boolean` | `true` — предмет надет |
| `isUnequip()` | `boolean` | `true` — предмет снят |

Конструкторы:

```java
// YAML-предмет
public CustomItemEquipEvent(Player player, CustomItem customItem, boolean equip)

// Java-предмет
public CustomItemEquipEvent(Player player, AbstractCustomItem javaItem, boolean equip)
```

Пример — не давать надевать кастомную броню в PvP-арене:

```java
@EventHandler
public void onEquip(CustomItemEquipEvent event) {
    if (!event.isEquip()) return;
    if (!event.getItemId().startsWith("arena-")) return;
    if (event.getPlayer().getWorld().getName().equals("pvp_arena")) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cВ арене это нельзя.");
    }
}
```

## CustomItemKillEvent

Срабатывает, когда игрок убил **игрока** кастомным предметом из руки.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getVictim()` | `Player` | Убитый игрок |
| `getKiller()` | `Player` | Убийца (то же, что `getPlayer()`) |

Конструкторы:

```java
// YAML-предмет
public CustomItemKillEvent(Player killer, CustomItem customItem, Player victim)

// Java-предмет
public CustomItemKillEvent(Player killer, AbstractCustomItem javaItem, Player victim)
```

Пример — награда за убийство:

```java
@EventHandler
public void onKill(CustomItemKillEvent event) {
    if (!"vampire-blade".equals(event.getItemId())) return;
    event.getKiller().sendMessage("§4Вы выпили жизненную силу соперника!");
}
```

## CustomItemDeathEvent

Срабатывает, когда игрок умер, а у него в руке или на себе был кастомный предмет.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getVictim()` | `Player` | Погибший игрок (то же, что `getPlayer()`) |
| `getDeathEvent()` | `PlayerDeathEvent` | Оригинальное событие Bukkit |

Конструкторы:

```java
// YAML-предмет
public CustomItemDeathEvent(Player player, CustomItem customItem, PlayerDeathEvent deathEvent)

// Java-предмет
public CustomItemDeathEvent(Player player, AbstractCustomItem javaItem, PlayerDeathEvent deathEvent)
```

Пример — запрещаем терять особый предмет при смерти:

```java
@EventHandler
public void onDeath(CustomItemDeathEvent event) {
    if (!"soul-lantern".equals(event.getItemId())) return;
    // Предмет можно забрать с трупа — убираем его из дропа
    event.getDeathEvent().getDrops().removeIf(stack ->
        stack != null && "soul-lantern".equals(event.getItemId()));
}
```

## CustomItemDamageDealtEvent

Срабатывает, когда игрок нанёс урон кастомным предметом из руки.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getBukkitEvent()` | `EntityDamageByEntityEvent` | Оригинальное событие урона |
| `getDamage()` | `double` | Текущая величина урона |
| `setDamage(double)` | — | Изменить величину урона |

Конструкторы:

```java
// YAML-предмет
public CustomItemDamageDealtEvent(Player player, CustomItem customItem, EntityDamageByEntityEvent bukkitEvent)

// Java-предмет
public CustomItemDamageDealtEvent(Player player, AbstractCustomItem javaItem, EntityDamageByEntityEvent bukkitEvent)
```

Пример — уменьшаем урон кастомного оружия в мирной зоне:

```java
@EventHandler
public void onDamageDealt(CustomItemDamageDealtEvent event) {
    if (!event.getPlayer().getWorld().getName().equals("spawn")) return;
    event.setDamage(event.getDamage() * 0.5);
}
```

Важно: отмена этого события **не отменяет сам урон**. Она лишь говорит плагину не выполнять стандартную реакцию предмета (триггеры и хук `onDamageDealt`). Если нужно отменить урон — отменяйте `event.getBukkitEvent()`.

## CustomItemDamageTakenEvent

Срабатывает, когда игрок получил урон, а кастомный предмет был в руке или одет.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getBukkitEvent()` | `EntityDamageEvent` | Оригинальное событие урона |
| `getDamage()` | `double` | Текущая величина урона |
| `setDamage(double)` | — | Изменить величину урона |

Конструкторы:

```java
// YAML-предмет
public CustomItemDamageTakenEvent(Player player, CustomItem customItem, EntityDamageEvent bukkitEvent)

// Java-предмет
public CustomItemDamageTakenEvent(Player player, AbstractCustomItem javaItem, EntityDamageEvent bukkitEvent)
```

## CustomItemCraftEvent

Срабатывает при крафте кастомного предмета — и в обычном верстаке, и в GUI-крафте модуля `customcraft` (`/craft`).

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getResult()` | `ItemStack` | Стак, который получит игрок |
| `setResult(ItemStack)` | — | Заменить выдаваемый предмет. `null` игнорируется |
| `getAmount()` | `int` | Количество в выдаваемом стаке |

Конструкторы:

```java
// YAML-предмет
public CustomItemCraftEvent(Player player, CustomItem customItem, ItemStack result)

// Java-предмет
public CustomItemCraftEvent(Player player, AbstractCustomItem javaItem, ItemStack result)
```

Пример — выдаём предмет с дополнительным зачарованием:

```java
@EventHandler
public void onCraft(CustomItemCraftEvent event) {
    if (!"my-crafted-sword".equals(event.getItemId())) return;

    ItemStack result = event.getResult().clone();
    ItemMeta meta = result.getItemMeta();
    if (meta != null) {
        meta.addEnchant(Enchantment.SHARPNESS, 6, true);
        result.setItemMeta(meta);
    }
    event.setResult(result);
}
```

Или запрещаем крафт:

```java
@EventHandler
public void onCraft(CustomItemCraftEvent event) {
    if (event.getItemId() != null && event.getItemId().startsWith("admin-")) {
        event.setCancelled(true);   // игрок не заберёт результат, ингредиенты останутся
    }
}
```

## CustomItemPeriodicEvent

Срабатывает перед каждым вызовом `onPeriodic` у Java-предмета. У YAML-предметов периодических эффектов нет, поэтому у этого события **только один конструктор** — для Java-предмета.

| Метод | Возвращает | Что это |
|-------|------------|---------|
| `getInterval()` | `long` | Интервал в тиках (из `getPeriodicInterval()`) |

Конструктор:

```java
public CustomItemPeriodicEvent(Player player, AbstractCustomItem javaItem, long interval)
```

Пример:

```java
@EventHandler
public void onPeriodic(CustomItemPeriodicEvent event) {
    if (event.getInterval() < 40) {
        event.setCancelled(true);   // запрещаем слишком частый вызов
    }
}
```

## Готовый пример слушателя

Полный рабочий слушатель: запрещает один предмет, следит за крафтом и смягчает урон кастомного оружия в мирном мире.

```java
package com.example.dci;

import me.dcplugin.artifex.events.CustomItemCraftEvent;
import me.dcplugin.artifex.events.CustomItemUseEvent;
import me.dcplugin.artifex.events.CustomItemDamageDealtEvent;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DciListener implements Listener {

    /** Предметы, которые запрещены в мирных мирах. */
    private static final String PEACEFUL_ITEM = "admin-blade";

    /**
     * Клик по кастомному предмету.
     * Срабатывает и для YAML-предметов, и для Java-предметов.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onUse(CustomItemUseEvent event) {
        String id = event.getItemId();
        if (id == null) return;

        Player player = event.getPlayer();

        // 1. Запрет в мирных мирах — работает для любого типа предметов
        if (PEACEFUL_ITEM.equals(id)
                && player.getWorld().getName().equals("spawn")) {
            event.setCancelled(true);
            player.sendMessage("§cВ этом мире этот предмет нельзя использовать.");
            return;
        }

        // 2. Своя логика в зависимости от типа предмета
        if (event.isJavaItem()) {
            AbstractCustomItem javaItem = event.getJavaItem();
            // у Java-предмета можно спросить ID и уникальные свойства
            if ("storm-blade".equals(javaItem.getId())) {
                player.sendMessage("§bИспользован Java-предмет.");
            }
        } else {
            CustomItem yamlItem = event.getCustomItem();
            // у YAML-предмета можно спросить тип и слот активации
            if ("ARMOR".equals(yamlItem.getType())) {
                player.sendMessage("§7Надето: " + yamlItem.getActivationSlot());
            }
        }
    }

    /**
     * Крафт кастомного предмета: запрещаем админские вещи и усиливаем обычные.
     */
    @EventHandler
    public void onCraft(CustomItemCraftEvent event) {
        String id = event.getItemId();
        if (id == null) return;

        if (id.startsWith("admin-")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cЭтот предмет нельзя скрафтить.");
            return;
        }

        if ("my-crafted-sword".equals(id)) {
            ItemStack result = event.getResult().clone();
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.SHARPNESS, 6, true);
                result.setItemMeta(meta);
            }
            event.setResult(result);
        }
    }

    /**
     * Урон кастомным оружием: в мирном мире уменьшаем его вдвое.
     */
    @EventHandler
    public void onDamageDealt(CustomItemDamageDealtEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("spawn")) return;
        event.setDamage(event.getDamage() * 0.5);
    }
}
```

Регистрация слушателя (в `onEnable()` вашего плагина):

```java
getServer().getPluginManager().registerEvents(new DciListener(), this);
```

## Подключение к своему плагину

Чтобы ваш плагин видел классы событий, добавьте зависимость в `plugin.yml`:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.dci.MyPlugin
api-version: '1.21'
depend:
  - Artifex
```

| Строка | Зачем |
|--------|-------|
| `depend` | Artifex должен запуститься раньше вашего плагина |
| `softdepend` | Подойдёт, если ваш плагин умеет работать и без Artifex |

Классы лежат в пакете `me.dcplugin.artifex.events`, базовый класс — `me.dcplugin.artifex.events.CustomItemEvent`.

## Как отмена события влияет на предмет

Отмена события не отменяет «мир», она отменяет только поведение предмета. Вот что происходит в разных случаях:

| Событие | Что будет, если отменить |
|---------|--------------------------|
| `CustomItemUseEvent` | Клик игнорируется целиком: не выполняются действия YAML-предмета, не выполняются триггеры, **не вызывается** `onRightClick` / `onLeftClick` Java-предмета, предмет не расходуется |
| `CustomItemEquipEvent` | Не применяются эффекты, не появляются частицы, не показываются сообщения и не вызываются `onEquip` / `onUnequip` |
| `CustomItemKillEvent` | Не выполняются триггеры `on_kill` и хук `onKill`. Само убийство никуда не денется |
| `CustomItemDeathEvent` | Не выполняются триггеры `on_death` и хук `onDeath`. Смерть игрока остаётся |
| `CustomItemDamageDealtEvent` | Не выполняются триггеры `on_damage_dealt` и хук `onDamageDealt`. Урон по-прежнему наносится |
| `CustomItemDamageTakenEvent` | Не выполняются триггеры `on_damage_taken` и хук `onDamageTaken` |
| `CustomItemCraftEvent` | Игрок не заберёт результат, ингредиенты останутся на месте |
| `CustomItemPeriodicEvent` | Конкретный вызов `onPeriodic` пропускается |

Пример: как «отключить» Java-предмет из своего плагина:

```java
@EventHandler
public void onUse(CustomItemUseEvent event) {
    // Отменяем клик — значит, onRightClick() этого предмета не вызовется
    if ("storm-blade".equals(event.getItemId())) {
        event.setCancelled(true);
    }
}
```

Важно: заряд (`getMaxUses()`) списывается **до** того, как вызывается событие. Отмена события не вернёт заряд обратно.

## Порядок вызовов

Для клика по кастомному предмету порядок такой:

1. Проверка права. Нет права → клик отменяется, **событие не вызывается**.
2. Проверка кулдауна. Предмет на кулдауне → клик игнорируется, **событие не вызывается**.
3. Списание заряда (`getMaxUses()`).
4. `CustomItemUseEvent`.
5. Если событие отменено — всё заканчивается.
6. Иначе: действия YAML-предмета, триггеры, эффекты или хуки Java-предмета.

Для надевания и снятия:

1. Проверка мира (запрещённый предмет удаляется раньше).
2. `CustomItemEquipEvent`.
3. Если отменено — эффекты, частицы, сообщения и хуки пропускаются.
4. Иначе — эффекты и сообщения YAML-предмета или хук `onEquip` / `onUnequip`.

## Подводные камни

| Проблема | Почему | Что делать |
|----------|--------|-----------|
| Событие не приходит | События срабатывают только для кастомных предметов | Проверьте, что предмет действительно кастомный |
| Событие не приходит при клике | Не хватает права или действует кулдаун | Проверьте `permission` и `click-cooldown` |
| `getCustomItem()` вернул `null` | Предмет написан на Java | Используйте `getJavaItem()` или `isJavaItem()` |
| `getJavaItem()` вернул `null` | Предмет описан в YAML | Используйте `getCustomItem()` |
| Отмена не помогла | Отменяемое событие отменяет только поведение предмета | Для отмены урона отменяйте `getBukkitEvent()` |
| Заряд списался, хотя вы отменили событие | Счётчик уменьшается до вызова события | Учитывайте это в логике |
| `NullPointerException` в `getResult()` | Результат может быть `null` | Проверяйте на `null` перед `getItemMeta()` |
| Исключение в слушателе ломает предмет | Ошибка в вашем коде | Оборачивайте код в `try/catch` и пишите в лог |

## Смотрите также

- [Предметы в YAML](03-yaml-items.md) и [Предметы в Java](04-java-items.md) — что именно вызывают эти события.
- [Справочник API](18-api-reference.md) — все классы и методы списком.
- [Модули](10-modules.md) — если нужна не реакция на событие, а отдельная подсистема.
- [Сообщения](11-messages.md) — как сделать свои тексты переопределяемыми.
- [Содержание вики](README.md).
