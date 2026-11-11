# Artifex — полная документация

**Версия документации:** 1.325.0
**Minecraft:** Paper/Spigot 1.21.x
**Java для сервера:** Java 21+ требуется для Paper 1.21.8+ и для сборки проекта.

Artifex позволяет создавать кастомные предметы, способности, команды, плейсхолдеры и GUI. Начать можно **без знания Java**: обычные предметы создаются в YAML. Java нужна только для сложных механик, собственных команд и модулей.

---

## 1. С чего начать

Если вы впервые видите YAML и Java, читайте в таком порядке:

1. [Установка и первый запуск](GETTING_STARTED_RU.md)
2. [Основы YAML](YAML_ITEMS_RU.md)
3. [Команды плагина](COMMANDS_RU.md)
4. [Сообщения и структура файлов](MESSAGES_DATABASE_RU.md)
5. [GUI и модули](MODULES_GUI_RU.md)
6. [Хранение данных: SQLite, MySQL и YAML](DATABASE_RU.md)
7. [Java с нуля](JAVA_API_RU.md)
8. [Оптимизация и решение проблем](TROUBLESHOOTING_RU.md)
9. [Resource pack и модели](RESOURCE_PACK_RU.md)

Рабочие примеры лежат в `samples/` в репозитории. При установке ничего не копируется — создаётся ровно один файл: `items/messages.java`. Чтобы включить образец, скопируйте нужный файл или папку из `samples/` в `items/`, уберите префикс `EXAMPLE-` из имени и выполните `/ci reload`.

---

## 2. Что умеет плагин

### Без программирования

Через YAML можно создавать:

- мечи, топоры, кирки и луки;
- броню со способностями при надевании и снятии;
- зелья, еду, свитки и тотемы;
- эффекты зелий;
- лечение и урон;
- частицы и звуки;
- телепортацию;
- выдачу и удаление обычных предметов;
- команды от имени игрока или консоли;
- кулдауны;
- ограниченное число использований;
- права на предметы;
- кастомные названия, lore и модели.

### С программированием на Java

Через `.java` можно создавать:

- предметы с любыми Bukkit/Paper-событиями;
- собственные команды и алиасы;
- плейсхолдеры;
- периодические эффекты;
- RPG-классы, квесты, боссы и мини-игры;
- модули со своими `config.yml`, `items.yml` и GUI;
- хранение прогресса в SQLite или MySQL;

Важно: Artifex компилирует пользовательские Java-файлы во время работы сервера. Поэтому запускайте только код, которому доверяете, и делайте резервную копию перед изменениями.

---

## 3. Установка

### Требования

- Paper или Spigot 1.21.x;
- Java 21 для Paper 1.21.8+;
- права администратора сервера;
- резервная копия мира и папки `plugins/`.

### Скачать релиз

```bash
cd ~/test/plugins
curl -fL -o dcustomitems.jar "https://github.com/animesao/dcustomitems/releases/download/v1.320.282/Artifex-1.320.282.jar"
```

Для обычного сервера замените `~/test/plugins` на путь к своей папке `plugins/`.

### Первый запуск

1. Положите `dcustomitems.jar` в `plugins/`.
2. Запустите сервер.
3. Дождитесь сообщения `CustomItems enabled!`.
4. Плагин создаст папку `plugins/Artifex/`.
5. Проверьте версию командой `/version Artifex` или по консоли.

При первом запуске создаются примерно такие файлы:

```text
plugins/Artifex/
├── config.yml       # настройки плагина
├── data.db          # SQLite-база (если выбран sqlite)
├── storage/         # небольшие YAML-хранилища
├── items/           # ваши YAML и Java-файлы
├── cache/           # кэш компилятора
└── compiled/        # скомпилированные классы
```

Плагин автоматически создаёт `items/messages.java`, если файла нет. Не удаляйте его без резервной копии, если используете настройку сообщений.

---

## 4. Самый простой предмет без Java

Создайте файл:

```text
plugins/Artifex/items/hello-sword.yml
```

Содержимое:

```yaml
hello_sword:
  type: TOOL
  activation-slot: HAND
  click-cooldown: 3000

  item:
    type: DIAMOND_SWORD
    title: '&aПриветственный меч'
    glowing: true
    unbreakable: true

  lore:
    - ''
    - '&7Это ваш первый предмет.'
    - '&eПКМ — получить скорость.'

  trigger-actions:
    - 'on_click_right:message:&aМеч работает!'
    - 'on_click_right:effect:SPEED:10:2'
    - 'on_click_right:particle:HEART:20'
    - 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
```

После сохранения выполните:

```text
/ci reload
/ci list
/ci give hello_sword
```

Возьмите меч в основную руку и нажмите ПКМ.

### Что означает каждая строка

- `hello_sword` — ID предмета. Используется в `/ci give hello_sword`.
- `type` — логический тип предмета: `TOOL`, `ARMOR`, `RUNE` или `CONSUMABLE`.
- `activation-slot` — где предмет активен.
- `click-cooldown` — задержка между срабатываниями в миллисекундах.
- `item.type` — настоящий материал Minecraft.
- `item.title` — название предмета.
- `glowing` — визуальное свечение.
- `unbreakable` — неломаемость.
- `lore` — список строк описания.
- `trigger-actions` — события и действия.

YAML чувствителен к отступам. Используйте пробелы, не табуляцию.

---

## 5. Как писать YAML, если вы новичок

YAML — это текстовый формат с отступами. В нём:

```yaml
ключ: значение
```

Список записывается через `-`:

```yaml
lore:
  - '&7Первая строка'
  - '&eВторая строка'
```

Вложенный раздел получает дополнительный отступ:

```yaml
item:
  type: DIAMOND_SWORD
  title: '&bМеч'
```

Правильно:

```yaml
item:
  type: DIAMOND_SWORD
```

Неправильно:

```yaml
item:
 type: DIAMOND_SWORD
```

Для текста с `:` используйте кавычки:

```yaml
- 'on_click_right:title:&6Сила!::&7Активирована:10:40:10'
```

Цвета Minecraft:

```text
&0 чёрный   &1 тёмно-синий  &2 тёмно-зелёный  &3 бирюзовый
&4 красный  &5 фиолетовый   &6 золотой       &7 серый
&8 тёмно-серый  &9 синий     &a зелёный       &b aqua
&c светло-красный &d розовый &e жёлтый        &f белый
&l жирный   &o курсив       &n подчёркивание &m зачёркивание
&r сброс форматирования
```

---

## 6. События предметов

Старый удобный формат:

```yaml
trigger-actions:
  - 'on_click_right:message:&aПКМ'
```

Также поддерживается структурированный формат:

```yaml
triggers:
  on_click_right:
    - 'message:&aПКМ'
    - 'effect:SPEED:5:1'
```

В текущем обработчике `trigger-actions` гарантированно подключены такие события:

| Событие | Когда срабатывает |
|---|---|
| `on_click_left` | ЛКМ с предметом |
| `on_click_right` | ПКМ с предметом |
| `on_equip` | Предмет экипирован |
| `on_unequip` | Предмет снят |
| `on_damage_dealt` | Игрок нанёс урон |
| `on_damage_taken` | Игрок получил урон |
| `on_kill` | Игрок убил другого игрока (текущий listener) |
| `on_death` | Игрок умер |
| `on_jump` | Игрок прыгнул |
| `on_drop` | Кастомный предмет выброшен |
| `on_pickup` | Кастомный предмет подобран |

Имена `on_block_break`, `on_block_place`, `on_sneak`, `on_sprint`, `on_swim` встречаются в моделях/старых примерах, но не следует считать их универсально активными в текущем `TriggerListener`. Проверяйте конкретную реализацию перед использованием.

Для Java API доступны отдельные методы `onMove`, `onBlockBreak`, `onSwapHand` и другие методы `AbstractCustomItem`; это другой путь обработки, не тот же самый, что YAML `trigger-actions`. `on_move` и `onMove` вызываются часто и могут нагрузить сервер.

### Bukkit-события для сторонних плагинов

Плагин публикует собственные Bukkit-события, чтобы другие плагины могли реагировать на кастомные предметы:

| Событие | Когда | Можно отменить |
|---|---|---|
| `CustomItemEquipEvent` | Предмет экипирован или снят (до эффектов/сообщений или хука `onEquip`/`onUnequip`) | Да |
| `CustomItemUseEvent` | ЛКМ/ПКМ с предметом (до действий клика или хука `onRightClick`/`onLeftClick`) | Да |
| `CustomItemCraftEvent` | Крафт — и в обычном верстаке, и в GUI `/craft` (до списания ингредиентов; результат можно заменить через `setResult`) | Да |
| `CustomItemDamageDealtEvent` | Урон нанесён предметом (до триггеров/хука `onDamageDealt`) | Да |
| `CustomItemDamageTakenEvent` | Урон получен с предметом (до триггеров/хука `onDamageTaken`) | Да |
| `CustomItemKillEvent` | Игрок убил игрока предметом (до `on_kill`/`onKill`) | Да |
| `CustomItemDeathEvent` | Игрок умер с предметом (до `on_death`/`onDeath`) | Да |
| `CustomItemPeriodicEvent` | Периодический эффект Java-предмета (до каждого `onPeriodic`) | Да |

События срабатывают и для YAML-предметов, и для Java API-предметов (`AbstractCustomItem`):

```java
import me.dcplugin.artifex.events.CustomItemUseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    @EventHandler
    public void onUse(CustomItemUseEvent event) {
        if ("vampire-blade".equals(event.getItemId())) {
            event.setCancelled(true); // запретить использование
        }
    }

    @EventHandler
    public void onEquip(CustomItemEquipEvent event) {
        if (event.getJavaItem() != null) {        // Java API-предмет
            String id = event.getJavaItem().getId();
        } else if (event.getCustomItem() != null) { // YAML-предмет
            String id = event.getCustomItem().getId();
        }
    }
}
```

Регистрация стандартная: `getServer().getPluginManager().registerEvents(new MyListener(), plugin)`. Отмена события подавляет только стандартную реакцию предмета (триггеры/хук); сам Bukkit-эффект (урон, смерть) отменяется через `getBukkitEvent()`/`setDamage()` для урон-событий.

---

## 7. Действия YAML

В обычном `trigger-actions` строка имеет формат:

```text
событие:действие:параметр1:параметр2
```

И `PlayerListener`, и `TriggerListener` выполняют ВСЕ действия через единый `ActionParser`. Доступны: `message`, `title`, `actionbar`, `announce`/`broadcast`, `effect`, `heal`, `damage` (+ `damage_nearby`/`damage_mobs`/`damage_players`), `heal_nearby`, `effect_nearby` (+ `_mobs`/`_players`), `teleport`, `teleport_relative`, `give` (материал или ID кастомного предмета), `remove`, `exp`, `lightning`, `lightning_forward`, `particle(s)`, `sound`, `fireworks`, `break`, `sethealth`, `setfood`, `set_xp`, `vanish`, `glow`, `speed`, `flight`, `knockback`/`launch`/`stun` (+ `_mobs`/`_players`), `command` (от консоли), `console_command`, а также расширенные: `particles_custom`, `sound_sequence`, `title_sequence`, `command_sequence`, `teleport_sequence`, `effect_sequence`, `damage_custom`, `heal_custom`.

Дефисы и подчёркивания эквивалентны (`damage-mobs` == `damage_mobs`). Действие нужно проверять на тестовом сервере.

### Сообщения и интерфейс

```yaml
- 'on_click_right:message:&aПривет, %player%!'
- 'on_click_right:actionbar:&eСпособность готова'
- 'on_click_right:broadcast:&6%player% активировал артефакт'
- 'on_click_right:announce:&6%player% активировал артефакт'
- 'on_click_right:title:&6Сила!|&7На 10 секунд|10|40|10'
```

В разных старых примерах встречается `::` в title. Для текущего Java-listener безопаснее использовать разделитель `|`.

### Эффекты

```yaml
# effect:ТИП:СЕКУНДЫ:УРОВЕНЬ
- 'on_click_right:effect:SPEED:10:2'
- 'on_click_right:effect:STRENGTH:5:1'
```

Уровень записывается как привычный уровень Minecraft: `1` — первый уровень, `2` — второй.

Поддерживаются, среди прочего: `SPEED`, `SLOWNESS`, `HASTE`, `MINING_FATIGUE`, `STRENGTH`, `JUMP_BOOST`, `REGENERATION`, `RESISTANCE`, `FIRE_RESISTANCE`, `WATER_BREATHING`, `INVISIBILITY`, `NIGHT_VISION`, `POISON`, `WITHER`, `HEALTH_BOOST`, `ABSORPTION`, `GLOWING`, `LUCK`, `UNLUCK`, `DOLPHINS_GRACE`, `CONDUIT_POWER`, `SLOW_FALLING`, `BAD_OMEN`, `HERO_OF_THE_VILLAGE` и эффекты новых версий Paper.

### Лечение и урон

```yaml
- 'on_click_right:heal:10'
- 'on_damage_dealt:damage:5'
- 'on_click_right:damage_nearby:8:4'
- 'on_click_right:damage_mobs:8:5'
- 'on_click_right:damage_players:4:3'
- 'on_click_right:heal_nearby:5:5'
```

В обработчике YAML `heal` использует величину здоровья Minecraft (обычно `10` = 5 сердец). Проверяйте результат на тестовом сервере.

### Предметы и опыт

```yaml
- 'on_kill:give:DIAMOND:1'
- 'on_click_right:remove:DIAMOND:1'
- 'on_click_right:exp:100:1'
```

### Телепортация

```yaml
- 'on_click_right:teleport:100:64:200'
- 'on_click_right:teleport_relative:~:2:~'
```

### Частицы и звуки

```yaml
- 'on_click_right:particle:FLAME:50'
- 'on_click_right:particles_custom:FLAME:100:0:1:0:0.5:0.5:0.5'
- 'on_click_right:sound:ENTITY_PLAYER_LEVELUP:1:1'
- 'on_click_right:fireworks:1'
```

Названия должны существовать в вашей версии Paper. Ошибки вроде `Unknown particle` или `Unknown sound` означают, что имя устарело или написано неверно.

### Состояние игрока

```yaml
- 'on_click_right:sethealth:20'
- 'on_click_right:setfood:20'
- 'on_click_right:vanish:10'
- 'on_click_right:glow:10'
- 'on_click_right:speed:10:2'
- 'on_click_right:flight:true'
- 'on_click_right:knockback:4'
- 'on_click_right:launch:1.5'
- 'on_click_right:stun:3:4'
```

### Команды и расширенные действия

В текущем YAML-trigger пути подтверждено действие `command`:

```yaml
# Команда выполняется текущим обработчиком предмета
- 'on_click_right:command:spawn'
```

Все перечисленные выше действия, включая расширенные, работают в YAML (и `trigger-actions`, и `triggers:`), потому что оба слушателя вызывают `ActionParser.execute()`.

Команды из пользовательского YAML могут быть опасными. Не давайте редактировать такие файлы обычным игрокам.

Дополнительная таблица действий находится в [README-ACTIONS.md](../samples/EXAMPLE-README-ACTIONS.md). Это справочный файл для старого и расширенного формата; при конфликте ориентируйтесь на текущую версию обработчика и проверяйте действие на тестовом сервере.

---

## 8. Поля YAML-предмета

```yaml
my_item:
  type: RUNE
  activation-slot: HAND
  placeable: false
  permission: 'myplugin.item.use'
  click-cooldown: 3000
  max-uses: 5

  item:
    type: BLAZE_ROD
    amount: 1
    title: '&dПосох'
    glowing: true
    unbreakable: true
    item-model: 'myplugin:staff'
    custom-model-data: 1001
    item-flags:
      - HIDE_ATTRIBUTES
    enchantments:
      UNBREAKING: 3

  lore:
    - '&7Использований: %uses%'
    - '&7Кулдаун: %cooldown% сек.'

  effects:
    - 'SPEED:1'

  trigger-actions:
    - 'on_click_right:lightning_forward:10'
```

`item-model` используется в современных версиях Minecraft. `custom-model-data` оставлен для совместимости со старыми ресурс-паками. Если указаны оба, приоритет имеет `item-model`.

`max-uses` используется обработчиком ограниченных применений; отображение `%uses%` зависит от конкретного обработчика и должно быть проверено на тестовом предмете.

### Крафт-рецепты (shaped / shapeless / furnace)

Рецепты задаются прямо в YAML предмета. Ингредиентом может быть материал (`DIAMOND`) или другой кастомный предмет по его id. Результат — сам предмет со всеми механиками. После изменения рецептов выполните `/ci reload`.

```yaml
my-crafted-sword:
  type: TOOL
  item:
    type: DIAMOND_SWORD
    title: '&bКрафтовый Меч'

  recipes:
    # Крафт по форме (пробел = пустая ячейка)
    shaped:
      - pattern:
          - " A "
          - "ABA"
          - " A "
        keys:
          A: DIAMOND
          B: STICK
        amount: 1

    # Крафт без формы
    shapeless:
      - ingredients: [ DIAMOND, DIAMOND, my-crafted-sword ]

    # Переплавка (200 тиков = 10 сек)
    furnace:
      - ingredient: IRON_INGOT
        experience: 0.7
        cooking-time: 200
```

Каждый список (`shaped`, `shapeless`, `furnace`) — это список рецептов, можно добавить несколько. См. также `samples/EXAMPLE-crafting.yml`.

Рецепты можно крафтить и через **GUI-верстак**: модуль-образец `items/EXAMPLE-customcraft/` добавляет команду `/craft` и окно 3×3 с предпросмотром результата. Чтобы включить модуль, скопируйте папку в `items/` и уберите префикс `EXAMPLE-` (`EXAMPLE-customcraft/` → `customcraft/`). Ингредиенты матчатся по PDC/NBT: кастомные предметы — по их ID (можно и «сам себя»), предметы других плагинов — по материалу, их данные не повреждаются. Готовый предмет инициализируется (uses/duration). Удали папку `customcraft/` — команда исчезнет.

Java API-предметы объявляют рецепты тем же способом через `getRecipes()` (класс `RecipeDef`) — они попадают и в обычный верстак, и в `/craft`. Пример см. в [JAVA_API_RU.md](JAVA_API_RU.md#-крафт-рецепты-java).

---

## 9. Команды плагина

### YAML-предметы

```text
/ci                         # помощь
/ci give <id>               # выдать себе предмет
/ci give <id> <игрок>       # выдать игроку
/ci list                    # список YAML-предметов
/ci reload                  # перезагрузить YAML, Java и модули
/ci update                  # проверить обновление
```

Алиас основной команды: `/customitems`.

### Java API-предметы

```text
/api-item                  # помощь
/api-item give <id>         # выдать себе Java-предмет
/api-item give <id> <игрок> # выдать игроку
/api-item list              # список Java-предметов
/api-item info <id>         # подробная информация
```

### Права

```text
customitems.give
customitems.list
customitems.reload
customitems.update (проверяется кодом, но не объявлен в текущем plugin.yml — при необходимости выдайте право через систему permissions)
customitems.admin (зарезервировано как общее admin-право; команды /ci используют отдельные права)
```

Если команда не работает, сначала проверьте `/plugins`, OP-статус и консоль.

---

## 10. Java для человека, который не знает Java

Java-файл — это инструкция для сервера. В нём есть:

- `class` — описание объекта;
- `method` — действие или функция;
- `String` — текст;
- `int`/`double` — числа;
- `boolean` — `true` или `false`;
- `@Override` — мы заменяем стандартное поведение базового класса.

Минимальный Java-предмет:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class WelcomeItem extends AbstractCustomItem {
    @Override
    public String getId() {
        return "welcome_item";
    }

    @Override
    public String getDisplayName() {
        return "&aПриветственный предмет";
    }

    @Override
    public Material getMaterial() {
        return Material.EMERALD;
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        ItemAPI.message(player, "&aВы нажали ПКМ!");
        ItemAPI.particles(player, Particle.HEART, 20);
    }
}
```

Сохраните как `WelcomeItem.java` или, для встроенного компилятора, используйте имя файла без пробелов и дефисов, например `welcome-item.java`. Имя публичного класса должно соответствовать правилам компилятора и быть уникальным.

Затем:

```text
/ci reload
/api-item list
/api-item give welcome_item
```

Если Java не компилируется, предмет не будет загружен — исправьте первую ошибку в консоли, а не последнюю строку стека.

Подробно: [JAVA_API_RU.md](JAVA_API_RU.md).

---

## 11. Java-команды

Наследуйте `CustomCommand`:

```java
import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;

public class PingCommand extends CustomCommand {
    public PingCommand() {
        super("ping", "Проверка пинга", "/ping", "example.ping", "p");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        msg(sender, "&aPong!");
        return true;
    }
}
```

После `/ci reload` в консоли должна появиться строка:

```text
[API] Command: /ping
[API] Registered command: /ping
```

Не используйте имя команды, уже занятой Paper или другим плагином. Не регистрируйте одну и ту же команду в нескольких Java-файлах.

---

## 12. Плейсхолдеры

Минимальный плейсхолдер:

```java
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

public class ServerNamePlaceholder extends CustomPlaceholder {
    public ServerNamePlaceholder() {
        super("server_name");
    }

    @Override
    public String getValue(Player player) {
        return "My Server";
    }
}
```

Значение запрашивается как `%server_name%` в системах, которые вызывают PlaceholderManager. Сам Artifex не является полноценным PlaceholderAPI-адаптером для всех сторонних плагинов, поэтому совместимость конкретного места использования нужно проверять.

---

## 13. DeluxeMenuX: меню без Java

Модуль находится в:

```text
plugins/Artifex/items/deluxemenux/   # после включения EXAMPLE-deluxemenux/ → deluxemenux/
├── config.yml
├── deluxemenux.java
├── menu-command.java
├── kits-command.java
├── shop-command.java
└── menus/
    ├── main.yml
    ├── kits.yml
    └── shop.yml
```

Команды:

```text
/menu
/menu <id>
/menu list
/menu reload
/deluxemenu
/kits
/kit
/shop
```

Меню создаются автоматически только при отсутствии файлов. Ваши существующие YAML не перезаписываются.

Пример кнопки:

```yaml
items:
  13:
    material: DIAMOND
    name: '&bАлмаз'
    lore:
      - '&7Нажмите для выдачи'
    permission: 'shop.diamond'
    command: 'give %player% diamond 1'
    message: '&aАлмаз выдан!'
    sound: 'ENTITY_PLAYER_LEVELUP'
    close: false
```

Обратите внимание:

- `command` выполняется от имени **консоли**;
- `%player%` заменяется именем игрока;
- `permission` проверяется на нажатии кнопки;
- число в `items.13` — слот от `0` до `size - 1`;
- `size` должен быть `9`, `18`, `27`, `36`, `45` или `54`;
- надпись `Цена: 100 монет` сама по себе ничего не списывает — экономику нужно подключить отдельным плагином или Java-механикой;
- `close-sound` сейчас читается конфигурацией, но закрывающий звук не является гарантированным действием текущего обработчика.

Подробно: [MODULES_GUI_RU.md](MODULES_GUI_RU.md).

---

## 14. Свой модуль внутри плагина

Каждая подпапка `items/` может быть модулем:

```text
items/my-feature/
├── config.yml
├── items.yml
├── my-feature.java
├── feature-command.java
└── menus/
    └── main.yml
```

`config.yml`:

```yaml
name: 'My Feature'
version: '1.0'
enabled: true
commands:
  - myfeature
permissions:
  - myfeature.use
```

Java-модуль наследует `Module` и реализует `onEnable()`/`onDisable()`. В `onEnable()` регистрируйте слушатели и запускайте механику; в `onDisable()` отменяйте задачи и очищайте ресурсы.

После изменения Java или YAML:

```text
/ci reload
```

Если меняется только ресурс-пак, нужен `F3+T` у игрока или повторная загрузка resource pack.

---

## 15. База данных SQLite, MySQL и YAML

Полное руководство с настройкой MySQL, промокодами и асинхронными запросами: [DATABASE_RU.md](DATABASE_RU.md).

По умолчанию работает локальный SQLite. Для MySQL в `config.yml` укажите `database.type: mysql`, заполните `database.mysql.*` и перезапустите сервер. Для небольших ручных списков используйте `YamlStorage`; для большого онлайна и промокодов — `DatabaseManager` с async-методами.

Плагин подключает базу:

```text
plugins/Artifex/data.db
```

Java API предоставляет `DatabaseManager`:

```java
DatabaseManager db = Main.getInstance().getDatabaseManager();
db.createTable("player_stats", "uuid TEXT PRIMARY KEY, kills INTEGER DEFAULT 0");
db.execute("INSERT INTO player_stats(uuid, kills) VALUES(?, ?)", uuid, 1);
int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid);
```

Доступны операции `connect`, `disconnect`, `reconnect`, `createTable`, `execute`, `insert`, `update`, `delete`, `queryInt`, `queryString`, `queryDouble`, `queryBoolean`, `queryOne`, `queryAll`, `increment` и `add`.

Не выполняйте тяжёлые SQL-запросы каждый тик и не удаляйте `data.db` без резервной копии.

---

## 16. Сообщения

**Все тексты, которые видит игрок, живут в одном месте — `items/messages.java`.** Это единственный файл, который плагин создаёт при установке.

Стандартные значения лежат в `MessagesConfig.java` внутри JAR. При первом запуске плагин создаёт `items/messages.java` — рабочую копию, которую можно править без пересборки.

**`items/messages.java` — рабочий конфиг сообщений.** Плагин компилирует его вместе с остальными Java-файлами и вызывает статический `load()` при старте и при каждом `/ci reload` — изменения вступают в силу без пересборки JAR.

Файл генерируется автоматически при первом запуске (содержит стандартные значения) и перезаписывает поля `MessagesConfig` (PREFIX, NO_PERMISSION, RELOAD_* и т.д.). Если вы удалили файл — он создастся заново при следующем старте.

Пример (примерно так выглядит сгенерированный файл):

```java
MessagesConfig.PREFIX = "&8[&bМой сервер&8] &r";
MessagesConfig.ITEM_EXPIRED = "&cПредмет {item} вышел из строя!";
MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cНедостаточно прав.";
```

Используйте только поля, которые существуют в текущем `MessagesConfig`. Каждый `public static String` из файла можно переопределить внутри `load()`. Если поле удалено в новой версии плагина — компиляция сообщит об этом в консоли (и файл просто не применится, останутся стандартные сообщения).

### Свои тексты по любому пути

Кроме стандартных полей можно завести **собственные ключи** — их читает и ваш модуль, и админ может переопределить их в том же файле:

```java
MessagesConfig.set("my-module.no-money", "&cНе хватает монет!");
MessagesConfig.set("my-module.purchased", "&aКуплено: {item} за {count} монет!");
```

### Как модуль подключается к общей системе

Модуль (папка в `items/`) регистрирует свои тексты и читает их по пути:

```java
@Override
protected void onEnable() {
    registerMessage("my-module.no-money", "&cNot enough money!");
}

void onBuy(Player player) {
    player.sendMessage(getMessage("my-module.no-money", "&cNot enough money!"));
}
```

Приоритет: переопределение из `items/messages.java` → текст модуля → стандартное сообщение плагина → запасной вариант по умолчанию.

То же самое доступно из обычной команды (`extends CustomCommand`):

```java
protected String message(String path, String defaultMessage) { ... }
```

---

## 17. Производительность

Чтобы сервер не нагружался:

- не используйте `on_move` для тяжёлых операций;
- не запускайте SQL каждый тик;
- ограничивайте радиус AoE;
- используйте `click-cooldown`;
- не создавайте сотни частиц на каждого игрока;
- не делайте бесконечные циклы в `onPeriodic`;
- отменяйте Bukkit-задачи в `onDisable`;
- не перезагружайте плагин каждую секунду;
- сначала тестируйте механику на локальном сервере;
- следите за `/spark profiler` и TPS.

`/ci reload` предназначен для разработки и настройки. На большом сервере не запускайте его во время массового онлайна без необходимости.

---

## 18. Резервные копии и обновление

Перед обновлением:

```bash
cp -a plugins/Artifex plugins/Artifex.backup
cp server.properties server.properties.backup
```

Обновление JAR:

```bash
dck stop test2
cd ~/test/plugins
curl -fL -o dcustomitems.jar "https://github.com/animesao/dcustomitems/releases/download/v1.320.282/Artifex-1.320.282.jar"
# очищайте cache/compiled только если менялась система Java-компиляции
rm -rf Artifex/cache Artifex/compiled
dck start test2
dck attach test2
```

Не удаляйте `Artifex/` целиком, если хотите сохранить свои предметы, меню и базу.

---

## 19. Диагностика

При проблеме сохраните:

- версию Paper;
- версию Java;
- версию Artifex;
- имя изменённого файла;
- полную первую ошибку из консоли;
- результат `/ci list`, `/api-item list` и `/plugins`.

Частые ошибки:

| Ошибка | Причина |
|---|---|
| `Unknown particle` | Частица удалена/переименована в вашей версии |
| `Unknown sound` | Неверное имя звука |
| `Class ... is public, should be declared in ...` | Имя публичного Java-класса не совпадает с ожидаемым именем файла |
| `cannot find symbol` | Нет импорта, неверный API Paper или опечатка |
| `Unknown or incomplete command` | Команда не зарегистрирована из-за ошибки компиляции/конфликта |
| `duplicate class definition` | Используется старый JAR/кэш или Java-класс загружается повторно; обновите JAR и очистите cache/compiled |
| `items folder is empty` | В items/ только образцы `EXAMPLE-*` — они не загружаются. Уберите префикс `EXAMPLE-` из имени файла или создайте свой `.yml` |
| `UnknownHostException` контейнера | Paper не может определить hostname контейнера; обычно не связано с плагином |

Полный список решений: [TROUBLESHOOTING_RU.md](TROUBLESHOOTING_RU.md).

---

## 20. Короткий чек-лист новичка

```text
[ ] Установлен Paper и подходящая Java
[ ] JAR лежит в plugins/
[ ] Сервер был перезапущен
[ ] Создан файл items/my-item.yml
[ ] Отступы YAML сделаны пробелами
[ ] Выполнен /ci reload
[ ] В консоли нет красных ошибок
[ ] Предмет виден через /ci list
[ ] Предмет выдан через /ci give <id>
[ ] Механика протестирована в безопасном мире
[ ] Перед публикацией сделана резервная копия
```

Если вы не знаете Java — оставайтесь на YAML. Если YAML уже стал понятен — переходите к Java API. Не нужно изучать весь Bukkit API сразу: добавляйте одну механику, перезагружайте, тестируйте и только потом расширяйте проект.

---

## Ссылки

- [Репозиторий GitHub](https://github.com/animesao/dcustomitems)
- [Релизы](https://github.com/animesao/dcustomitems/releases)
- [Issues](https://github.com/animesao/dcustomitems/issues)
- [English documentation](README_EN.md)
