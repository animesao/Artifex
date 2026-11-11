# Руководство по Java API

## 📚 Полный Справочник

Это руководство описывает всё о создании кастомных предметов с помощью Java API.

---

## 🎯 Обзор

Java API позволяет создавать продвинутые кастомные предметы с полной поддержкой кода Java. Это идеально для:

- Сложных механик предметов
- Кастомных игровых механик
- Продвинутых эффектов и анимаций
- Предметов с управлением состоянием
- Многофункциональных предметов

---

## 🚀 Быстрый Старт

### Шаг 1: Создайте Класс Предмета

Создайте новый Java файл в `plugins/Artifex/items/`:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my_sword"; }

    @Override
    public String getDisplayName() { return "&6Мой Java Меч"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        ItemAPI.heal(player, 5);
        ItemAPI.effect(player, PotionEffectType.SPEED, 10, 1);
        ItemAPI.particles(player, Particle.FLAME, 50);
    }
}
```

### Шаг 2: Перезагрузите Плагин

```
/ci reload
```

### Шаг 3: Получите Предмет

```
/api-item give my_sword
```

---

## 📝 Структура Класса

### Обязательные Методы

```java
public class MyItem extends AbstractCustomItem {

    // ОБЯЗАТЕЛЬНО: Уникальный ID предмета
    @Override
    public String getId() { return "my_item"; }

    // ОБЯЗАТЕЛЬНО: Отображаемое название
    @Override
    public String getDisplayName() { return "&6Мой Предмет"; }

    // ОБЯЗАТЕЛЬНО: Тип материала
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
}
```

### Необязательные Методы

```java
public class MyItem extends AbstractCustomItem {

    @Override
    public String getId() { return "my_item"; }

    @Override
    public String getDisplayName() { return "&6Мой Предмет"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    // Необязательно: Описание предмета
    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Строка описания 1",
            "&eСтрока описания 2",
            ""
        );
    }

    // Необязательно: Модель для ресурс-пака
    @Override
    public String getItemModel() { return "my_model"; }

    // Необязательно: Неломаемость
    @Override
    public boolean isUnbreakable() { return true; }

    // Необязательно: Эффект свечения
    @Override
    public boolean isGlowing() { return true; }

    // Необязательно: Кулдаун кликов в миллисекундах
    @Override
    public long getClickCooldown() { return 3000; }

    // Необязательно: Требуемое право
    @Override
    public String getPermission() { return "myplugin.use"; }

    // Необязательно: Тип предмета
    @Override
    public String getType() { return "TOOL"; }

    // Необязательно: Слот активации
    @Override
    public String getActivationSlot() { return "HAND"; }
}
```

---

## 🎯 Методы Событий

### События Кликов

```java
@Override
public void onLeftClick(PlayerInteractEvent event, Player player) {
    // Действие при ЛКМ
}

@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    // Действие при ПКМ
}
```

### События Экипировки

```java
@Override
public void onEquip(Player player) {
    // При экипировке предмета
}

@Override
public void onUnequip(Player player) {
    // При снятии предмета
}
```

Хуки `onEquip`/`onUnequip` вызываются глобальным чекером экипировки, когда предмет лежит в своём `getActivationSlot()` (HEAD/CHEST/LEGS/FEET/HAND/OFFHAND). Перед хуком плагин публикует Bukkit-событие `CustomItemEquipEvent` — если сторонний плагин его отменит, хук не вызовется (см. раздел «Bukkit-события» в README).

### Боевые События

```java
@Override
public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
    // При нанесении урона
}

@Override
public void onDamageTaken(EntityDamageEvent event, Player player) {
    // При получении урона
}

@Override
public void onKill(Player killer, Player victim) {
    // При убийстве игрока
}

@Override
public void onDeath(Player player, PlayerDeathEvent event) {
    // При смерти игрока с этим предметом
}
```

### События Движения

```java
@Override
public void onJump(Player player) {
    // При прыжке игрока
}

@Override
public void onMove(PlayerMoveEvent event, Player player) {
    // При движении игрока
}
```

### События Блоков

```java
@Override
public void onBlockBreak(Player player, BlockBreakEvent event) {
    // При ломании блока
}
```

### События Предметов

```java
@Override
public void onDrop(Player player, PlayerDropItemEvent event) {
    // При выбрасывании предмета
}

@Override
public void onPickup(Player player) {
    // При подборе предмета
}
```

### Периодические Эффекты

```java
@Override
public long getPeriodicInterval() {
    return 20; // Каждые 20 тиков (1 секунда)
}

@Override
public void onPeriodic(Player player) {
    // Вызывается периодически
}
```

`onPeriodic` тоже вызывает глобальный чекер экипировки — только когда предмет находится в своём слоте активации и только раз в `getPeriodicInterval()` тиков. Не делайте внутри тяжёлых операций.

### Механики YAML-уровня для Java-предметов

Java-предметы получили те же базовые механики, что и YAML:

```java
@Override
public int getMaxUses() { return 5; }        // 0 = безлимит (списывается при кликах)

@Override
public long getDuration() { return 3600; }   // секунды жизни предмета, 0 = вечный

@Override
public boolean isAllowedInWorld(String worldName) {
    return !worldName.equals("pvp_arena"); // запретить в своих мирах
}

@Override
public String getPermission() { return "items.frostblade"; } // теперь проверяется при кликах
```

`getMaxUses()` списывается обработчиком кликов (при исчерпании предмет удаляется), `getDuration()` и `isAllowedInWorld()` проверяются глобальным чекером (при нарушении предмет удаляется). Сообщения переопределяются через `getUsesDepletedMessage()`, `getDurationExpiredMessage()`, `getWorldBlockedMessage()`.

Перед каждым хуком (`onDamageDealt`, `onDamageTaken`, `onKill`, `onDeath`, `onPeriodic`) теперь стреляют одноимённые отменяемые Bukkit-события `CustomItem*Event` — сторонние плагины могут отменить стандартную реакцию предмета.

---

## 🛠️ Утилиты ItemAPI

### Лечение

```java
// Лечение игрока (в полухочках)
ItemAPI.heal(player, 10); // +5 сердец
```

### Эффекты

```java
// Применить эффект зелья
ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
// effect: игрок, тип, длительность_сек, уровень
```

### Частицы

```java
// Создать частицы
ItemAPI.particles(player, Particle.FLAME, 50);
// particles: игрок, тип, количество
```

### Звуки

```java
// Воспроизвести звук
ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
// sound: игрок, тип, громкость, тон
```

### Телепортация

```java
// Телепортация на координаты
ItemAPI.teleport(player, 100, 64, 200);

// Телепортация к другому игроку
ItemAPI.teleportToPlayer(player, targetPlayer, 2);
```

### Молния

```java
// Вызвать молнию вперёд
ItemAPI.lightningForward(player, 10);
```

### Урон

```java
// Нанести урон ближайшим
ItemAPI.damageNearby(player, 10, 5);
// damageNearby: игрок, урон, радиус
```

### Лечение (AoE)

```java
// Лечить ближайших
ItemAPI.healNearby(player, 10, 5);
// healNearby: игрок, количество, радиус
```

### Заголовки

```java
// Показать заголовок
ItemAPI.title(player, "Заголовок", "Подзаголовок");

// Показать заголовок с таймингами
player.sendTitle("Заголовок", "Подзаголовок", 10, 40, 10);
// fadeIn, stay, fadeOut в тиках
```

### Сообщения

```java
// Отправить сообщение
ItemAPI.message(player, "&aПривет!");
```

### Выдача Предметов

```java
// Выдать предмет игроку
ItemAPI.giveItem(player, Material.DIAMOND, 5);
```

---

## 📝 Полные Примеры

### Огненный Меч

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FireSword extends AbstractCustomItem {

    @Override
    public String getId() { return "fire_sword"; }

    @Override
    public String getDisplayName() { return "&c&lОгненный Меч"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7ПКМ для огненной силы!",
            "&7Поджигает врагов при ударе",
            ""
        );
    }

    @Override
    public String getItemModel() { return "fire_sword"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getClickCooldown() { return 5000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Дать сопротивление огню
        ItemAPI.effect(player, PotionEffectType.FIRE_RESISTANCE, 30, 0);

        // Частицы и звук
        ItemAPI.particles(player, Particle.FLAME, 100);
        ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

        // Заголовок
        ItemAPI.title(player, "&c&lОГНЕННАЯ СИЛА!", "&7Сопротивление огню на 30 секунд!");
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        // Поджечь цель
        if (event.getEntity() instanceof LivingEntity) {
            ((LivingEntity) event.getEntity()).setFireTicks(100);
        }
    }
}
```

### Посох Телепортации

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class TeleportStaff extends AbstractCustomItem {

    @Override
    public String getId() { return "teleport_staff"; }

    @Override
    public String getDisplayName() { return "&b&lПосох Телепортации"; }

    @Override
    public Material getMaterial() { return Material.BLAZE_ROD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7ПКМ для телепортации вперёд",
            "&7ЛКМ для случайной телепортации",
            ""
        );
    }

    @Override
    public String getItemModel() { return "teleport_staff"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public long getClickCooldown() { return 3000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Телепортация вперёд на 10 блоков
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(10));
        ItemAPI.teleport(player, loc.getX(), loc.getY(), loc.getZ());

        // Эффекты
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&b&lТЕЛЕПОРТ!", "&7Вперёд на 10 блоков");
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // Случайная телепортация
        double x = player.getLocation().getX() + (Math.random() * 200 - 100);
        double z = player.getLocation().getZ() + (Math.random() * 200 - 100);
        double y = player.getWorld().getHighestBlockYAt((int) x, (int) z);

        ItemAPI.teleport(player, x, y + 1, z);

        // Эффекты
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&d&lСЛУЧАЙНЫЙ ТЕЛЕПОРТ!", "");
    }
}
```

### Лечащий Шлем

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HealingHelmet extends AbstractCustomItem {

    @Override
    public String getId() { return "healing_helmet"; }

    @Override
    public String getDisplayName() { return "&a&lЛечащий Шлем"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_HELMET; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            "&7Даёт регенерацию",
            "&7при экипировке",
            ""
        );
    }

    @Override
    public String getItemModel() { return "healing_helmet"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public String getActivationSlot() { return "HEAD"; }

    @Override
    public long getPeriodicInterval() { return 100; } // Каждые 5 секунд

    @Override
    public void onEquip(Player player) {
        // Дать регенерацию
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 999999, 0);

        // Эффекты
        ItemAPI.particles(player, Particle.HEART, 20);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        ItemAPI.message(player, "&aЛечащий шлем экипирован!");
    }

    @Override
    public void onUnequip(Player player) {
        // Убрать регенерацию
        player.removePotionEffect(PotionEffectType.REGENERATION);

        ItemAPI.message(player, "&aЛечащий шлем снят.");
    }

    @Override
    public void onPeriodic(Player player) {
        // Лечение 1 сердце каждые 5 секунд
        ItemAPI.heal(player, 2);
    }
}
```

---

## 📦 Крафт-рецепты (Java)

Java-предмет может объявить крафт-рецепты — переопределите `getRecipes()`:

```java
import me.dcplugin.artifex.api.RecipeDef;

@Override
public List<RecipeDef> getRecipes() {
    Map<Character, String> keys = new HashMap<>();
    keys.put('N', "NETHERITE_INGOT");      // материал
    keys.put('B', "vampire-blade");        // или ID кастомного предмета (YAML/Java)

    return List.of(
        // По форме: 1-3 строки одинаковой длины, пробел = пустая ячейка
        RecipeDef.shaped(List.of("NNN", "NBN"), keys),

        // Без формы: просто список ингредиентов
        RecipeDef.shapeless("DIAMOND", "DIAMOND", "STICK"),

        // Переплавка: предмет, опыт, время в тиках
        RecipeDef.furnace("IRON_INGOT", 0.7f, 200)
    );
}
```

Рецепты регистрируются в обычный верстак автоматически и попадают в GUI-крафт модуля `/craft` (образец `items/EXAMPLE-customcraft/`). Ингредиентом может быть любой кастомный предмет — YAML или Java — по его ID (включая сам предмет). После `RecipeDef.shapeless(...)` количество результата по умолчанию 1; для другого количества есть версии с параметром `amount`, а для `furnace` — `experience` и `cookingTime`.

---

## 💡 Советы

### Совет 1: Используйте ItemAPI

Всегда используйте методы `ItemAPI` вместо сырого Bukkit API когда это возможно. Он обрабатывает краевые случаи и обеспечивает согласованное поведение.

### Совет 2: Проверяйте Null

Всегда проверяйте нулевые значения:

```java
@Override
public void onRightClick(PlayerInteractEvent event, Player player) {
    if (event.getItem() == null) return;
    
    // Ваш код здесь
}
```

### Совет 3: Используйте Кулдауны

Предотвращайте спам способностей:

```java
@Override
public long getClickCooldown() { return 3000; }
```

### Совет 4: Обрабатывайте Права

Проверяйте права перед мощными способностями:

```java
@Override
public String getPermission() { return "myplugin.powerful"; }
```

---

## 📚 Связанная Документация

- [Начало Работы](GETTING_STARTED_RU.md)
- [Руководство по YAML](YAML_ITEMS_RU.md)
- [Руководство по Ресурс-Паку](RESOURCE_PACK_RU.md)
- [Справочник Команд](COMMANDS_RU.md)

---

**Далее:** [Руководство по Ресурс-Паку](RESOURCE_PACK_RU.md) →
