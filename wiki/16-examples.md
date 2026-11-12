# 16. Примеры

> **Что ты узнаешь:** готовые рабочие рецепты, которые можно скопировать и
> сразу использовать — от простейшего предмета до собственного модуля.
>
> **Предусловия:** [Установка](01-getting-started.md) выполнена.

---

## Содержание

- [Как пользоваться примерами](#как-пользоваться-примерами)
- [1. Простейший предмет](#1-простейший-предмет)
- [2. Предмет с эффектами и кулдауном](#2-предмет-с-эффектами-и-кулдауном)
- [3. Меч вампира (урон + лечение)](#3-меч-вампира-урон--лечение)
- [4. Предмет с ограниченным числом использований](#4-предмет-с-ограниченным-числом-использований)
- [5. Предмет с ограниченным сроком жизни](#5-предмет-с-ограниченным-сроком-жизни)
- [6. Броня и сет с бонусом](#6-броня-и-сет-с-бонусом)
- [7. Реликвия, запрещённая в мире](#7-реликвия-запрещённая-в-мире)
- [8. Предмет с рецептом](#8-предмет-с-рецептом)
- [9. Предмет с триггерами](#9-предмет-с-триггерами)
- [10. След частиц](#10-след-частиц)
- [11. Своя команда](#11-своя-команда)
- [12. Свой плейсхолдер](#12-свой-плейсхолдер)
- [13. Модуль с командой и данными](#13-модуль-с-командой-и-данными)
- [14. Предмет на Java](#14-предмет-на-java)
- [15. Свои сообщения](#15-свои-сообщения)
- [16. Модуль на Java с базой данных](#16-модуль-на-java-с-базой-данных)

---

## Как пользоваться примерами

1. Создайте файл в `plugins/Artifex/items/`.
2. Вставьте пример.
3. Выполните `/ci reload`.
4. Проверьте в консоли, что предмет загрузился:
   `[items] Loaded item 'vampire-blade' from vampire-blade.yml`

**Если предмет не появился** — прочитайте [Решение проблем](17-troubleshooting.md).

> Все примеры используют id в нижнем регистре и без пробелов — так надёжнее.
> id предмета = ключ в YAML, по нему же работают плейсхолдеры и рецепты.

---

## 1. Простейший предмет

`items/simple-sword.yml`

```yaml
simple-sword:
  type: TOOL
  activation-slot: HAND
  item:
    type: DIAMOND_SWORD
    title: '&bПростой меч'
    lore:
      - '&7Обычный, но честный.'
```

Показывает: минимальный набор полей. Всё остальное необязательно.

---

## 2. Предмет с эффектами и кулдауном

`items/haste-helm.yml`

```yaml
haste-helm:
  type: ARMOR
  activation-slot: HEAD
  placeable: false
  click-cooldown: 1000
  item:
    type: LEATHER_HELMET
    title: '&6Шлем спешки'
    glowing: true
    unbreakable: true
    enchantments:
      PROTECTION: 2
    lore:
      - '&7Кулдаун: &e%cooldown%с'
  effects:
    - 'SPEED:2'
    - 'JUMP_BOOST:1'
```

**Что важно:**

- `activation-slot: HEAD` — эффекты идут, пока шлем на голове;
- `click-cooldown: 1000` — секундный кулдаун между кликами;
- `%cooldown%` в лоре показывает, сколько осталось ждать;
- `glowing: true` даёт свечение без лишнего зачарования (без +1 к урону).

---

## 3. Меч вампира (урон + лечение)

`items/vampire-blade.yml`

```yaml
vampire-blade:
  type: TOOL
  activation-slot: HAND
  permission: customitems.vampire
  item:
    type: NETHERITE_SWORD
    title: '&4[Кровь] &fКлинок вампира'
    unbreakable: true
    enchantments:
      SHARPNESS: 4
    lore:
      - '&7Правый клик — пить кровь'
      - '&7Левый клик — удар по площади'
  effects:
    - 'REGENERATION:1'
  right-click-actions:
    - 'heal:6'
    - 'particle:REDSTONE:30'
    - 'sound:ENTITY_WITCH_DRINK:1:1.2'
  left-click-actions:
    - 'damage_nearby:4:3'
    - 'particle:CRIT:20'
```

**Разбор:**

- `right-click-actions` / `left-click-actions` — списки действий
  (все 54 — в [Действиях](05-actions.md));
- `damage_nearby:4:3` — 4 урона всем живым в радиусе 3 блоков;
- `heal:6` — восстановить 6 HP;
- `permission` — использовать меч может только игрок с правом.

**Добавьте триггер** (по желанию) — лечение при убийстве:

```yaml
  triggers:
    on_kill:
      - 'heal:4'
      - 'particle:REDSTONE:40'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:1.5'
```

---

## 4. Предмет с ограниченным числом использований

`items/throw-scroll.yml`

```yaml
throw-scroll:
  type: CONSUMABLE
  activation-slot: HAND
  max-uses: 3
  uses-depleted-message: '&cСвиток рассыпался.'
  item:
    type: PAPER
    title: '&fСвиток телепортации'
    lore:
      - '&7Использований: &b%uses%'
  right-click-actions:
    - 'teleport:~:5:~'
    - 'particle:PORTAL:60'
    - 'sound:ENTITY_ENDERMAN_TELEPORT:1:1'
```

**Что происходит:**

- каждый клик тратит одно использование;
- когда счётчик кончился, предмет исчезает, игрок получает сообщение;
- `%uses%` в лоре показывает остаток.

---

## 5. Предмет с ограниченным сроком жизни

`items/temporary-shield.yml`

```yaml
temporary-shield:
  type: ARMOR
  activation-slot: CHEST
  duration: 300
  max-duration-message: '&cЩит рассыпался по истечении времени.'
  item:
    type: SHIELD
    title: '&bВременный щит'
    unbreakable: true
```

`duration: 300` — 5 минут. Плагин сам следит за временем: раз в секунду
проверяет инвентарь и убирает истёкшие предметы.

---

## 6. Броня и сет с бонусом

**Четыре предмета** — `items/dark-lord-set.yml`:

```yaml
dark-lord-helmet:
  type: ARMOR
  activation-slot: HEAD
  armor-set: dark-lord
  item:
    type: NETHERITE_HELMET
    title: '&4Шлем Повелителя'

dark-lord-chestplate:
  type: ARMOR
  activation-slot: CHEST
  armor-set: dark-lord
  item:
    type: NETHERITE_CHESTPLATE
    title: '&4Нагрудник Повелителя'

dark-lord-leggings:
  type: ARMOR
  activation-slot: LEGS
  armor-set: dark-lord
  item:
    type: NETHERITE_LEGGINGS
    title: '&4Поножи Повелителя'

dark-lord-boots:
  type: ARMOR
  activation-slot: FEET
  armor-set: dark-lord
  item:
    type: NETHERITE_BOOTS
    title: '&4Ботфорты Повелителя'
```

**Бонус за полный комплект** — в `config.yml`:

```yaml
set-bonuses:
  dark-lord:
    effects:
      - 'INCREASE_DAMAGE:3'
      - 'SPEED:2'
      - 'REGENERATION:2'
```

Бонус включается, когда надеты **все четыре** части с одним `armor-set`.

> `armor-set` и `attributes` можно указывать и на верхнем уровне предмета,
> и внутри секции `item:` — работают оба варианта.

---

## 7. Реликвия, запрещённая в мире

`items/world-key.yml`

```yaml
world-key:
  type: TOOL
  activation-slot: HAND
  disabled-worlds:
    - creative_world
  item:
    type: TRIPWIRE_HOOK
    title: '&eКлюч мира'
```

Подбор в `creative_world` блокируется, а предмет, уже лежащий в инвентаре,
будет удалён при попытке смены мира.

---

## 8. Предмет с рецептом

`items/blessed-blade.yml`

```yaml
blessed-blade:
  type: TOOL
  activation-slot: HAND
  item:
    type: DIAMOND_SWORD
    title: '&6Благословлённый клинок'
  recipes:
    shaped:
      - pattern:
          - ' A '
          - 'ABA'
          - ' A '
        keys:
          A: DIAMOND
          B: NETHERITE_INGOT
        amount: 1
    furnace:
      - ingredient: IRON_SWORD
        experience: 0.7
        cooking-time: 200
```

`A` и `B` — любые буквы. В `keys` указывается **материал** (`DIAMOND`) или
**id другого кастомного предмета** (`vampire-blade`).

Подробности — в [Рецептах](07-recipes.md).

---

## 9. Предмет с триггерами

`items/guardian-amulet.yml`

```yaml
guardian-amulet:
  type: ARMOR
  activation-slot: CHEST
  item:
    type: GOLDEN_APPLE
    title: '&bОберег'
  triggers:
    on_damage_taken:
      - 'particle:SHIELD:25'
      - 'sound:ITEM_SHIELD_BLOCK:1:1.4'
    on_equip:
      - 'message:&7Оберег на вас.'
    on_unequip:
      - 'message:&7Оберег покидает вас.'
    on_pickup:
      - 'message:&7Вы подобрали оберег.'
```

Триггер срабатывает, только когда предмет **в своём слоте активации**.

Полный список — в [Триггерах](06-triggers.md).

---

## 10. След частиц

`items/spirit-wand.yml`

```yaml
spirit-wand:
  type: TOOL
  activation-slot: HAND
  item:
    type: BLAZE_ROD
    title: '&dПосох духа'
  trail-particles:
    - 'FLAME:8'
    - 'SOUL_FIRE_FLAME:4'
  trail-particle-interval: 5
```

След идёт за игроком, пока посох в руке. `trail-particle-interval` — раз в
сколько тиков (1 тик = 0.05 с); 5 → каждые 0.25 с.

---

## 11. Своя команда

`items/heal.java`

```java
import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Heal extends CustomCommand {

    public Heal() {
        super("heal", "Heal a player", "/heal [player]", "customitems.heal");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return true;

        target.setHealth(Math.min(
                target.getHealth() + 10.0,
                target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        target.sendMessage("&aВы вылечены.");
        return true;
    }
}
```

Команда появится после `/ci reload`. Право — `customitems.heal`.

Подробно — в [Командах](08-commands.md).

---

## 12. Свой плейсхолдер

`items/kills.java`

```java
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

public class Kills extends CustomPlaceholder {

    private int kills = 0;

    public Kills() {
        super("kills");
    }

    @Override
    public String getValue(Player player) {
        return String.valueOf(kills);
    }

    public void addKill() {
        kills++;
    }
}
```

Использование: `%dci_kills%` (или `%artifex_kills%`).

---

## 13. Модуль с командой и данными

`items/duels/config.yml`:

```yaml
name: Duels
version: "1.0"
enabled: true
commands:
  - duel
permissions:
  - customitems.duel
```

`items/duels/duel.java`:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class Duel extends Module {

    private int duels = 0;

    public Duel(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        registerDynamicCommand("duel", this, "customitems.duel");
        plugin.getLogger().info("Duels module loaded, duels: " + duels);
    }

    @Override
    protected void onDisable() {
        plugin.getLogger().info("Duels module disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players!");
            return true;
        }
        duels++;
        player.sendMessage("&aYour duel count: &e" + duels);
        return true;
    }
}
```

> **Важно:** команда модуля должна иметь метод `onCommand(...)`.
> Базовый класс `Module` не содержит `onCommand` — добавьте его сами
> (как выше) или используйте `registerDynamicCommand` с отдельным
> `CommandExecutor`.

---

## 14. Предмет на Java

`items/lightning-staff.java`

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class LightningStaff extends AbstractCustomItem {

    @Override
    public String getId() { return "lightning_staff"; }

    @Override
    public String getDisplayName() { return "&6Посох молнии"; }

    @Override
    public Material getMaterial() { return Material.BLAZE_ROD; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        player.getWorld().strikeLightning(player.getEyeLocation());
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(0, 1, 0), 40);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
    }
}
```

Полный справочник хуков — в [Предметах (Java)](04-java-items.md).

---

## 15. Свои сообщения

Всё, что видит игрок, лежит в `items/messages.java`. Пример:

```java
MessagesConfig.PREFIX = "&8[&6DCI&8] &r";
MessagesConfig.ITEM_EXPIRED = "&cПредмет {item} вышел из строя!";
MessagesConfig.ITEM_WORLD_BLOCKED = "&cПредмет {item} запрещён в этом мире!";
```

Свои ключи для модуля:

```java
MessagesConfig.set("my-module.no-money", "&cНе хватает монет!");
```

А модуль читает их так:

```java
player.sendMessage(getMessage("my-module.no-money", "&cNot enough money!"));
```

Подробно — в [Сообщениях](11-messages.md).

---

## 16. Модуль на Java с базой данных

`items/stats/config.yml`:

```yaml
name: Stats
version: "1.0"
enabled: true
```

`items/stats/stats.java`:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class Stats extends Module {

    public Stats(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        plugin.getDatabaseManager().execute(
                "CREATE TABLE IF NOT EXISTS player_stats (uuid TEXT PRIMARY KEY, kills INTEGER)");
        registerDynamicCommand("stats", this, "customitems.stats");
    }

    @Override
    protected void onDisable() {
    }

    public int getKills(Player player) {
        return plugin.getDatabaseManager().queryInt(
                "SELECT kills FROM player_stats WHERE uuid = ?", player.getUniqueId().toString());
    }

    public void addKill(Player player) {
        plugin.getDatabaseManager().execute(
                "INSERT INTO player_stats (uuid, kills) VALUES (?, 1) "
                        + "ON CONFLICT(uuid) DO UPDATE SET kills = kills + 1",
                player.getUniqueId().toString());
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        // Читаем БД в отдельном потоке, чтобы не блокировать сервер
        CompletableFuture.supplyAsync(() -> getKills(player))
                .thenAccept(kills -> plugin.getServer().getScheduler()
                        .runTask(plugin, () ->
                                player.sendMessage("&aУбийств: &e" + kills)));
        return true;
    }
}
```

> **Имена таблиц и колонок** проверяются: допустимы только латинские буквы,
> цифры и подчёркивание. Это защита от SQL-инъекций.

Подробно — в [Базе данных](12-database.md).

---

## Ещё примеры

В репозитории лежит **82 готовых файла** — предметы, команды, плейсхолдеры,
модули, шаблоны:

| Что | Где |
|-----|-----|
| Все примеры | [`samples/`](https://github.com/animesao/Artifex/tree/main/samples) |
| Список действий | [`samples/EXAMPLE-README-ACTIONS.md`](https://github.com/animesao/Artifex/blob/main/samples/EXAMPLE-README-ACTIONS.md) |
| Шаблон модуля | [`samples/_template/`](https://github.com/animesao/Artifex/tree/main/samples/_template) |
| Магазин | `samples/EXAMPLE-shop/` |
| Экономика | `samples/EXAMPLE-vault/` |
| GUI-крафт | `samples/EXAMPLE-customcraft/` |
| Меню | `samples/EXAMPLE-deluxemenux/` |

**Как включить пример:** скопируйте файл или папку в `items/`, уберите
префикс `EXAMPLE-` из имени, сделайте `/ci reload`.

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
