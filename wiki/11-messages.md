# 11 — Сообщения

## Что ты узнаешь

- Главная идея версии **1.333**: **все** тексты, которые видит игрок, собраны в одном файле — `items/messages.java`.
- Как этот файл создаётся при установке и когда именно он применяется.
- Полный список встроенных полей `MessagesConfig` — что каждое означает.
- Полный список **путей сообщений** (`items.expired`, `ci.reload.success`, `commands.not-found`, …) и зачем каждый нужен.
- Как переопределить текст: по полю или по пути.
- Как модуль подключает свои тексты к общей системе (`registerMessage` / `getMessage` / `unregisterMessage`).
- Как своя команда читает сообщение (`message(path, default)`).
- Точный порядок приоритетов: что победит, если текст задан в нескольких местах.
- Что произойдёт, если удалить строку из `messages.java`.
- Полный пример переписанного `messages.java`.
- Почему нельзя писать несуществующие поля — и как это выглядит в консоли.

## Предусловия

- Плагин установлен, версия **1.333.0** или новее — [Установка](01-getting-started.md).
- Знаешь, что такое папка `items/` — [Как это работает](02-how-it-works.md).
- Умеешь писать Java (хотя бы одну строку) — [Предметы (Java)](04-java-items.md).
- Для модулей и команд — [Модули](10-modules.md) и [Команды](08-commands.md).
- Общий список страниц — [оглавление](README.md).

## Содержание

- [Идея: один файл на все тексты](#идея-один-файл-на-все-тексты)
- [Как файл создаётся и когда применяется](#как-файл-создаётся-и-когда-применяется)
  - [Создание при установке](#создание-при-установке)
  - [Применение при старте и при перезагрузке](#применение-при-старте-и-при-перезагрузке)
  - [Что должно быть в файле](#что-должно-быть-в-файле)
- [Встроенные поля MessagesConfig](#встроенные-поля-messagesconfig)
  - [Общие](#общие)
  - [Предметы](#предметы)
  - [Команды](#команды)
  - [Команда ci](#команда-ci)
  - [Проверка обновлений](#проверка-обновлений)
- [Пути сообщений](#пути-сообщений)
- [Как переопределить текст](#как-переопределить-текст)
  - [Переопределение по полю](#переопределение-по-полю)
  - [Переопределение по пути](#переопределение-по-пути)
- [Как модуль подключает свои тексты](#как-модуль-подключает-свои-тексты)
- [Как своя команда читает сообщение](#как-своя-команда-читает-сообщение)
- [Порядок приоритетов](#порядок-приоритетов)
- [Что будет, если удалить строку](#что-будет-если-удалить-строку)
- [Полный пример переписанного messages.java](#полный-пример-переписанного-messagesjava)
- [Только существующие поля](#только-существующие-поля)
- [Частые ошибки](#частые-ошибки)

---

## Идея: один файл на все тексты

До версии 1.333 тексты были разбросаны по коду плагина: чтобы перевести сообщение «Не хватает монет», нужно было менять исходники или искать по файлам.

Сейчас работает просто:

> **Каждый текст, который видит игрок, определён в одном файле — `plugins/Artifex/items/messages.java`.**

Что это даёт:

| Выгода | Пояснение |
|--------|-----------|
| Перевод | Меняете строки — и весь плагин говорит по-русски |
| Единый стиль | Префикс, цвета, формат — в одном месте |
| Без правок кода | Не нужно пересобирать jar-файл |
| Свои тексты модулей | Модуль отдаёт свой текст в общую систему, и вы переопределяете его там же |

Файл — обычный Java-файл (плагин умеет компилировать `.java` прямо на сервере, см. [Предметы (Java)](04-java-items.md)). Поэтому в нём можно писать условия, конкатенацию строк и всё остальное, что умеет Java.

> **Важно.** `items/messages.java` — единственный файл, который плагин создаёт сам при установке. Остальные файлы в `items/` вы создаёте сами.

## Как файл создаётся и когда применяется

### Создание при установке

При первом запуске плагин:

1. Создаёт папку `plugins/Artifex/items/`, если её нет.
2. Проверяет, есть ли `items/messages.java`.
3. Если файла нет — создаёт его с рабочим содержимым по умолчанию (на английском) и пишет в консоль:

```text
[Config] Created default messages.java
```

Файл записывается в кодировке **UTF-8** — русский тек в нём не превратится в «кракозябры».

Если файл уже есть — плагин его **не трогает**: все ваши правки сохраняются при обновлении плагина.

### Применение при старте и при перезагрузке

Файл не читается как текст — он **компилируется**, и у его класса вызывается статический метод `load()`.

```text
старт сервера                        /ci reload
─────────────                        ──────────
компиляция items/*.java               компиляция items/*.java
  ↓                                     ↓
items.messagesItem                    items.messagesItem
  ↓                                     ↓
MessagesConfig.clearCustom()          MessagesConfig.clearCustom()
  ↓                                     ↓
messagesItem.load()                   messagesItem.load()
  ↓                                     ↓
все MessagesConfig.* перезаписаны     все MessagesConfig.* перезаписаны
```

То есть:

| Момент | Что происходит |
|--------|----------------|
| Старт сервера | Файл компилируется, `load()` вызывается, тексты применяются |
| `/ci reload` | То же самое: старые переопределения сбрасываются, файл применяется заново |
| Выключение плагина | Ничего special — тексты живут в памяти класса |

Если всё прошло хорошо, в консоли вы увидите:

```text
[Messages] items/messages.java applied (runtime load())
```

### Что должно быть в файле

Минимальные требования:

| Требование | Пример | Зачем |
|------------|--------|-------|
| Класс `public` | `public class messages {` | Плагин переименовывает класс по имени файла и обращается к нему напрямую |
| Статический метод `load()` без аргументов | `public static void load() { ... }` | Именно его плагин вызывает: `clazz.getMethod("load").invoke(null)` |
| Импорт класса сообщений | `import me.dcplugin.artifex.api.config.MessagesConfig;` | Чтобы обращаться к полям |

Имя класса в файле (`messages`) не обязано совпадать с именем файла — но так привычнее, и именно так создаёт сам плагин.

> **Не переименовывайте `items/messages.java`.** Плагин ищет скомпилированный класс по имени файла: `items/messages.java` превращается в класс `items.messagesItem`, и именно его метод `load()` вызывается. Если переименовать файл в `my-messages.java`, класс станет `my_messagesItem` — файл будет компилироваться, но его тексты **не применятся**.

Если метода `load()` не будет, плагин напишет в консоль:

```text
[Messages] items/messages.java was not applied: <причина>
```

и продолжит работать со встроенными текстами.

## Встроенные поля MessagesConfig

**Поле** — это именованная строка в классе `MessagesConfig`. Значение поля меняется в файле `messages.java`, а плагин читает поле в нужный момент.

### Общие

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `PREFIX` | `&8[&6DCI&8] &r` | Префикс перед сообщениями плагина |
| `NO_PERMISSION` | `PREFIX + "&cYou don't have permission!"` | Нет прав на действие |
| `PLAYER_NOT_FOUND` | `PREFIX + "&cPlayer not found!"` | Игрок не найден |
| `ITEM_NOT_FOUND` | `PREFIX + "&cItem '{item}' not found!"` | Предмет не найден. `{item}` — id предмета |
| `UNKNOWN_COMMAND` | `PREFIX + "&cUnknown command!"` | Неизвестная команда |
| `ONLY_FOR_PLAYERS` | `PREFIX + "&cPlayers only!"` | Команда только для игроков (консоль так не может) |

### Предметы

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `ITEM_USES_DEPLETED` | `&cThe item was used up and is gone!` | Предмет закончился (заряды кончились) |
| `ITEM_EXPIRED` | `&cItem {item} has expired!` | Истёкло время жизни предмета. `{item}` — id |
| `ITEM_WORLD_BLOCKED` | `&cItem {item} is banned in this world!` | Предмет запрещён в этом мире. `{item}` — id |
| `ITEM_NO_PERMISSION` | `&cYou don't have permission to use this item.` | Нет прав на использование предмета |

### Команды

Эти тексты показывают **ваши** команды (классы `extends CustomCommand`), см. [Команды](08-commands.md).

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `COMMAND_NOT_FOUND` | `&cCommand not found: {label}` | Команда не найдена. `{label}` — как её вызвали |
| `COMMAND_ERROR` | `&cError executing command: {error}` | Внутри команды случилось исключение. `{error}` — текст ошибки |
| `COMMAND_NO_PERMISSION` | `&cYou don't have permission!` | Нет прав на команду |
| `COMMAND_PLAYER_NOT_FOUND` | `&cPlayer {player} not found!` | Игрок из аргумента не найден. `{player}` — ник |
| `COMMAND_SPECIFY_PLAYER` | `&cSpecify a player: {usage}` | Нужно указать игрока. `{usage}` — подсказка по использованию |

### Команда ci

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `CI_HEADER` | `&6=== Artifex ===` | Заголовок встроенной справки `/ci` |
| `CI_HELP_RELOAD` | `&e/ci reload &7- Reload the plugin` | Строка справки про `/ci reload` |
| `CI_HELP_VALIDATE` | `&7/ci validate &8- validate YAML recipes` | Строка справки про `/ci validate` |
| `CI_HELP_DEBUG` | `&7/ci debug <id> &8- dump item + PDC` | Строка справки про `/ci debug` |
| `CI_HELP_BACKUP` | `&7/ci backup &8- back up items/` | Строка справки про `/ci backup` |
| `CI_MODULES` | `&7Modules: &e{count}` | Сколько модулей загружено (ответ `/ci reload`). `{count}` — число |
| `CI_VALIDATION_OK` | `&aValidation OK: no problems found.` | Проверка рецептов прошла без проблем |
| `CI_VALIDATION_PROBLEMS` | `&eValidation: problems found: {count}` | Найдено проблем. `{count}` — число |
| `CI_DEBUG_USAGE` | `&cUsage: /ci debug <id>` | Не хватает id предмета |
| `CI_DEBUG_ITEM_NOT_FOUND` | `&cItem not found: {item}` | Предмет для отладки не найден. `{item}` — id |
| `CI_BACKUP_CREATED` | `&aBackup created: &e{file}` | Резервная копия создана. `{file}` — имя папки копии |
| `CI_BACKUP_FAILED` | `&cBackup failed: {error}` | Не удалось создать копию. `{error}` — причина |

### Проверка обновлений

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `UPDATE_AVAILABLE` | `&e[CustomItems] &fA new version is available: &b{version}` | Есть новая версия. `{version}` — номер версии |
| `UPDATE_DOWNLOAD` | `&e[CustomItems] &fDownload the update from the plugin page` | Где скачать обновление |
| `UPDATE_NOTIFICATION_HEADER` | `&8&m …` | Разделительная линия в уведомлении |
| `UPDATE_NOTIFICATION_TITLE` | `&6&lArtifex &7- &aUpdate available!` | Заголовок уведомления |
| `UPDATE_NOTIFICATION_CURRENT` | `&7Current version: &e{version}` | Текущая версия. `{version}` — номер |
| `UPDATE_NOTIFICATION_LATEST` | `&7New version: &a{version}` | Новая версия. `{version}` — номер |
| `UPDATE_NOTIFICATION_DOWNLOAD` | `&7Download: &b{url}` | Ссылка на загрузку. `{url}` — адрес |

### Поля перезагрузки

| Поле | Значение по умолчанию | Что это |
|------|----------------------|---------|
| `RELOAD_SUCCESS` | `PREFIX + "&a&lPlugin reloaded!"` | Перезагрузка прошла успешно |
| `RELOAD_YAML` | `PREFIX + "&7YAML items: &e{count}"` | Сколько YAML-предметов. `{count}` — число |
| `RELOAD_JAVA_ITEMS` | `PREFIX + "&7Java items: &e{count}"` | Сколько Java-предметов. `{count}` — число |
| `RELOAD_JAVA_COMMANDS` | `PREFIX + "&7Java commands: &e{count}"` | Сколько команд. `{count}` — число |
| `RELOAD_JAVA_PLACEHOLDERS` | `PREFIX + "&7Java placeholders: &e{count}"` | Сколько плейсхолдеров. `{count}` — число |
| `RELOAD_ERROR` | `PREFIX + "&cError: {error}"` | Ошибка перезагрузки. `{error}` — текст ошибки |

## Пути сообщений

**Путь** (*path*) — это строковый адрес текста в системе сообщений, например `items.expired`. Он нужен, когда текст берётся не из поля, а по запросу: «дай мне текст по адресу `items.expired`».

Встроенные пути — все, что есть в плагине:

| Путь | Какой текст | Поле-источник |
|------|-------------|---------------|
| `plugin.no-permission` | Нет прав | `NO_PERMISSION` |
| `plugin.player-not-found` | Игрок не найден | `PLAYER_NOT_FOUND` |
| `plugin.item-not-found` | Предмет не найден | `ITEM_NOT_FOUND` |
| `plugin.unknown-command` | Неизвестная команда | `UNKNOWN_COMMAND` |
| `plugin.only-for-players` | Только для игроков | `ONLY_FOR_PLAYERS` |
| `plugin.update-available` | Доступно обновление | `UPDATE_AVAILABLE` |
| `plugin.update-download` | Где скачать | `UPDATE_DOWNLOAD` |
| `items.uses-depleted` | Предмет закончился | `ITEM_USES_DEPLETED` |
| `items.expired` | Время жизни истекло | `ITEM_EXPIRED` |
| `items.world-blocked` | Предмет запрещён в мире | `ITEM_WORLD_BLOCKED` |
| `items.no-permission` | Нет прав на предмет | `ITEM_NO_PERMISSION` |
| `commands.not-found` | Команда не найдена | `COMMAND_NOT_FOUND` |
| `commands.error` | Ошибка в команде | `COMMAND_ERROR` |
| `commands.no-permission` | Нет прав на команду | `COMMAND_NO_PERMISSION` |
| `commands.player-not-found` | Игрок не найден | `COMMAND_PLAYER_NOT_FOUND` |
| `commands.specify-player` | Надо указать игрока | `COMMAND_SPECIFY_PLAYER` |
| `ci.header` | Заголовок справки | `CI_HEADER` |
| `ci.help.reload` | Строка справки | `CI_HELP_RELOAD` |
| `ci.help.validate` | Строка справки | `CI_HELP_VALIDATE` |
| `ci.help.debug` | Строка справки | `CI_HELP_DEBUG` |
| `ci.help.backup` | Строка справки | `CI_HELP_BACKUP` |
| `ci.reload.success` | Успешная перезагрузка | `RELOAD_SUCCESS` |
| `ci.reload.yaml` | Счётчик YAML-предметов | `RELOAD_YAML` |
| `ci.reload.java-items` | Счётчик Java-предметов | `RELOAD_JAVA_ITEMS` |
| `ci.reload.java-commands` | Счётчик команд | `RELOAD_JAVA_COMMANDS` |
| `ci.reload.java-placeholders` | Счётчик плейсхолдеров | `RELOAD_JAVA_PLACEHOLDERS` |
| `ci.reload.error` | Ошибка перезагрузки | `RELOAD_ERROR` |
| `ci.modules` | Счётчик модулей | `CI_MODULES` |
| `ci.validation.ok` | Проверка прошла | `CI_VALIDATION_OK` |
| `ci.validation.problems` | Найдены проблемы | `CI_VALIDATION_PROBLEMS` |
| `ci.debug.usage` | Подсказка `/ci debug` | `CI_DEBUG_USAGE` |
| `ci.debug.item-not-found` | Предмет не найден | `CI_DEBUG_ITEM_NOT_FOUND` |
| `ci.backup.created` | Копия создана | `CI_BACKUP_CREATED` |
| `ci.backup.failed` | Копия не создана | `CI_BACKUP_FAILED` |
| `update.header` | Разделитель уведомления | `UPDATE_NOTIFICATION_HEADER` |
| `update.title` | Заголовок уведомления | `UPDATE_NOTIFICATION_TITLE` |
| `update.current-version` | Текущая версия | `UPDATE_NOTIFICATION_CURRENT` |
| `update.new-version` | Новая версия | `UPDATE_NOTIFICATION_LATEST` |
| `update.download` | Ссылка на загрузку | `UPDATE_NOTIFICATION_DOWNLOAD` |

Пути нужны, когда текст читает **ваш** код: команда или модуль. Встроенные пути можно переопределить — см. ниже.

## Как переопределить текст

Есть два способа. Первый — для встроенных текстов, второй — для любых, включая свои.

### Переопределение по полю

Меняете значение поля прямо в `messages.java`:

```java
MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cУ вас нет прав на это!";
```

Это работает для всех встроенных полей из таблиц выше.

Про `PREFIX` есть важная тонкость:

```java
MessagesConfig.PREFIX = "&8[&6DC&8] &r";   // ← сначала префикс
MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cНет прав!";  // ← потом текст
```

Поля вроде `NO_PERMISSION` не пересчитываются сами — они берут `PREFIX` в момент присваивания. Поэтому:

- если вы меняете `PREFIX` — перепишите и те строки, которые его используют (иначе в них останется старый префикс);
- не удаляйте такие строки, а меняйте их текст.

### Переопределение по пути

Для любого пути — встроенного или своего — есть универсальный способ:

```java
MessagesConfig.set("my-module.no-money", "&cНе хватает монет!");
```

`set(path, value)` кладёт текст в отдельную таблицу, которая имеет **наивысший приоритет**. Этим способом можно переопределить и встроенный путь:

```java
MessagesConfig.set("items.expired", "&cПредмет {item} вышел из строя!");
MessagesConfig.set("commands.no-permission", "&cНедостаточно прав!");
```

Что ещё умеет `set`:

| Вызов | Что делает |
|-------|-----------|
| `MessagesConfig.set(path, text)` | Задать текст по пути |
| `MessagesConfig.set(path, null)` | Убрать переопределение по этому пути |
| `MessagesConfig.get(path, default)` | Получить текст: сначала переопределение, потом встроенный, потом `default` |
| `MessagesConfig.getCustom(path)` | Только переопределение (или `null`) |
| `MessagesConfig.getBuiltIn(path)` | Только встроенный текст (или `null`) |

## Как модуль подключает свои тексты

Модуль отдаёт свой текст в общую систему, чтобы вы могли переопределить его в `messages.java`.

В модуле есть три помощника:

```java
@Override
protected void onEnable() {
    // 1. Регистрируем свои тексты
    registerMessage("my-module.no-money", "&cНе хватает монет!");
    registerMessage("my-module.purchased", "&aКуплено: {item} за {price}");
}

@Override
protected void onDisable() {
    // 2. Убираем их при выключении модуля
    unregisterMessage("my-module.no-money");
    unregisterMessage("my-module.purchased");
}
```

И читаем текст в нужном месте:

```java
player.sendMessage(getMessage("my-module.no-money", "&cНе хватает монет!"));
```

| Метод модуля | Что делает |
|--------------|-----------|
| `registerMessage(path, value)` | Регистрирует текст модуля под путём `path` |
| `getMessage(path, default)` | Возвращает текст по пути (с учётом всех приоритетов), а если путь неизвестен — `default` |
| `unregisterMessage(path)` | Убирает зарегистрированный текст |

Теперь администратор может написать в `messages.java`:

```java
MessagesConfig.set("my-module.no-money", "&aНе хватает монет! Докупите.");
```

и текст модуля изменится — без правок кода модуля.

> **Порядок регистрации.** Регистрируйте тексты в `onEnable()`, а снимайте в `onDisable()`. Иначе после перезагрузки в системе останутся тексты отключённого модуля.

## Как своя команда читает сообщение

У класса `CustomCommand` есть защищённый метод:

```java
protected String message(String path, String defaultMessage)
```

Пример:

```java
public class BuyCommand extends CustomCommand {

    public BuyCommand() {
        super("buy", "Купить предмет", "/buy <id>", "dci.buy");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg(sender, message("commands.usage", "&cИспользование: /buy <id>"));
            return true;
        }

        if (!hasMoney(sender)) {
            msg(sender, message("my-command.no-money", "&cНе хватает монет!"));
            return true;
        }

        msg(sender, message("my-command.bought", "&aКуплено: {item}")
                .replace("{item}", args[0]));
        return true;
    }
}
```

| Что происходит | Пояснение |
|----------------|-----------|
| Текст ищется по пути | Сначала ваше переопределение, потом текст модуля, потом встроенный, потом `default` |
| Цвета обрабатываются | `&` превращается в `§` автоматически |
| Плейсхолдеры PlaceholderAPI | Не подставляются: метод работает без игрока. Подставляйте вручную через `.replace("{player}", …)` |

Текст «нет прав» для команды тоже идёт через систему сообщений — путь `commands.no-permission`. Свой вариант можно задать через `setPermissionMessage(...)`.

## Порядок приоритетов

Когда один и тот же путь задан в нескольких местах, побеждает тот, кто выше в списке:

| № | Источник | Как задаётся | Пример |
|---|----------|--------------|--------|
| 1 | **Ваше переопределение** | `MessagesConfig.set(path, text)` в `items/messages.java` | `MessagesConfig.set("items.expired", "…")` |
| 2 | **Текст модуля** | `registerMessage(path, text)` в модуле | `registerMessage("vault.no-money", "…")` |
| 3 | **Встроенный текст** | Поле `MessagesConfig.*` | `MessagesConfig.ITEM_EXPIRED` |
| 4 | **Значение по умолчанию** | Второй аргумент вызова | `getMessage("x.y", "&cТекст")` |

Схема:

```text
   items/messages.java
   MessagesConfig.set("path", "…")          ← 1. сильнее всех
            │
            ▼
   модуль: registerMessage("path", "…")     ← 2.
            │
            ▼
   встроенное поле MessagesConfig.*         ← 3.
            │
            ▼
   значение по умолчанию в коде              ← 4. слабее всех
```

Два практических вывода:

| Ситуация | Что победит |
|----------|-------------|
| Вы написали `set("items.expired", …)`, а модуль тоже зарегистрировал этот путь | Ваш текст из `messages.java` |
| Модуль зарегистрировал путь, которого нет в `messages.java` | Текст модуля |
| Путь не известен системе вообще | Значение по умолчанию, переданное в коде |

## Что будет, если удалить строку

Ответ зависит от того, **что именно** вы удалили.

| Что удалили из `messages.java` | Что будет после `/ci reload` |
|--------------------------------|------------------------------|
| Строку `MessagesConfig.set("путь", "текст")` | Переопределение **исчезнет**. Перед применением файла плагин вызывает `MessagesConfig.clearCustom()` — оно стирает все переопределения из прошлой загрузки |
| Строку `MessagesConfig.NO_PERMISSION = "…"` | Значение поля **останется прежним**. Поле — это обычная статическая переменная: если строка в файле её не перезаписывает, переменная сохраняет то значение, которое получила в прошлый раз |

Почему так:

```java
// перед применением файла:
MessagesConfig.clearCustom();   // очищает ТОЛЬКО таблицу set()-переопределений
clazz.getMethod("load").invoke(null);   // перезаписывает те поля, для которых есть строки
```

| Что хотите сделать | Как правильно |
|--------------------|---------------|
| Убрать переопределение пути | Удалите строку `set(...)` — и после reload её уже не будет |
| Вернуть встроенный текст поля | Не удаляйте строку, а присвойте встроенное значение (например, `MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cYou don't have permission!";`). Либо перезапустите сервер — при старте поля создаются заново с встроенными значениями |

Коротко: **строки `set(...)` «живут» ровно до следующей перезагрузки; строки-присваивания полей переживают перезагрузку.**

## Полный пример переписанного messages.java

Файл `plugins/Artifex/items/messages.java`:

```java
import me.dcplugin.artifex.api.config.MessagesConfig;

/**
 * Все сообщения Artifex.
 *
 * Это единственный файл, который плагин создаёт при установке.
 * Меняйте значения и выполняйте /ci reload — изменения применятся сразу.
 *
 * Формат:
 *   &0-§f — цвета, &l — жирный, &n — подчёркнутый, &o — курсив, &r — сброс
 *
 * Подстановки:
 *   {item} {player} {error} {count} {version} {label} {usage} {file} {url}
 */
public class messages {

    public static void load() {

        // ===== Общие =====
        MessagesConfig.PREFIX = "&8[&6DC&8] &r";

        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cУ вас нет прав на это!";
        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + "&cИгрок не найден!";
        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + "&cПредмет '&e{item}&c' не найден!";
        MessagesConfig.UNKNOWN_COMMAND = MessagesConfig.PREFIX + "&cНеизвестная команда!";
        MessagesConfig.ONLY_FOR_PLAYERS = MessagesConfig.PREFIX + "&cТолько для игроков!";

        // ===== Предметы =====
        MessagesConfig.ITEM_USES_DEPLETED = "&cПредмет закончился!";
        MessagesConfig.ITEM_EXPIRED = "&cПредмет &e{item}&c вышел из строя!";
        MessagesConfig.ITEM_WORLD_BLOCKED = "&cПредмет &e{item}&c запрещён в этом мире!";
        MessagesConfig.ITEM_NO_PERMISSION = "&cУ вас нет прав на этот предмет.";

        // ===== Ваши команды (items/*.java, extends CustomCommand) =====
        MessagesConfig.COMMAND_NOT_FOUND = "&cКоманда не найдена: &e{label}";
        MessagesConfig.COMMAND_ERROR = "&cОшибка выполнения команды: &e{error}";
        MessagesConfig.COMMAND_NO_PERMISSION = "&cУ вас нет прав на эту команду!";
        MessagesConfig.COMMAND_PLAYER_NOT_FOUND = "&cИгрок &e{player}&c не найден!";
        MessagesConfig.COMMAND_SPECIFY_PLAYER = "&cУкажите игрока: &e{usage}";

        // ===== Команда /ci =====
        MessagesConfig.CI_HEADER = "&6=== Artifex ===";
        MessagesConfig.CI_HELP_RELOAD = "&e/ci reload &7- Перезагрузить плагин";
        MessagesConfig.CI_HELP_VALIDATE = "&7/ci validate &8- Проверить рецепты YAML";
        MessagesConfig.CI_HELP_DEBUG = "&7/ci debug <id> &8- Данные предмета";
        MessagesConfig.CI_HELP_BACKUP = "&7/ci backup &8- Резервная копия items/";

        MessagesConfig.CI_MODULES = "&7Модулей: &e{count}";
        MessagesConfig.CI_VALIDATION_OK = "&aПроверка пройдена: проблем нет.";
        MessagesConfig.CI_VALIDATION_PROBLEMS = "&eПроверка: найдено проблем: &e{count}";
        MessagesConfig.CI_DEBUG_USAGE = "&cИспользование: /ci debug <id>";
        MessagesConfig.CI_DEBUG_ITEM_NOT_FOUND = "&cПредмет не найден: &e{item}";
        MessagesConfig.CI_BACKUP_CREATED = "&aРезервная копия создана: &e{file}";
        MessagesConfig.CI_BACKUP_FAILED = "&cНе удалось создать копию: &e{error}";

        // ===== Перезагрузка =====
        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + "&a&lПлагин перезагружен!";
        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + "&7YAML-предметы: &e{count}";
        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + "&7Java-предметы: &e{count}";
        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + "&7Java-команды: &e{count}";
        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + "&7Java-плейсхолдеры: &e{count}";
        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + "&cОшибка: &e{error}";

        // ===== Проверка обновлений =====
        MessagesConfig.UPDATE_AVAILABLE = "&e[CustomItems] &fДоступна новая версия: &b{version}";
        MessagesConfig.UPDATE_DOWNLOAD = "&e[CustomItems] &fОбновление — на странице плагина";
        MessagesConfig.UPDATE_NOTIFICATION_HEADER = "&8&m                                                    ";
        MessagesConfig.UPDATE_NOTIFICATION_TITLE = "&6&lArtifex &7- &aДоступно обновление!";
        MessagesConfig.UPDATE_NOTIFICATION_CURRENT = "&7Текущая версия: &e{version}";
        MessagesConfig.UPDATE_NOTIFICATION_LATEST = "&7Новая версия: &a{version}";
        MessagesConfig.UPDATE_NOTIFICATION_DOWNLOAD = "&7Загрузка: &b{url}";

        // ===== Свои тексты модулей и плагинов =====
        // Любой путь подходит. Модуль читает его через getMessage("путь", "…").
        MessagesConfig.set("my-module.no-money", "&cНе хватает монет!");
        MessagesConfig.set("my-module.purchased", "&aКуплено: &e{item}&a за &e{price}");

        // Переопределение встроенного пути — тоже так можно:
        MessagesConfig.set("items.expired", "&cПредмет &e{item}&c вышел из строя!");
    }
}
```

После сохранения:

```bash
/ci reload
```

и проверка в чате:

```text
[DC] Плагин перезагружен!
[DC] YAML-предметы: 12
```

## Только существующие поля

В `MessagesConfig` есть **конечный** список полей. Если написать поле, которого нет, — код не скомпилируется.

| Что написали | Что будет |
|--------------|-----------|
| `MessagesConfig.NO_PERMISSION = "…";` | Работает |
| `MessagesConfig.NO_PERMISION = "…";` (опечатка) | **Ошибка компиляции** |

Ошибка выглядит в консоли так:

```text
[JavaCompiler] ❌ messages.java:42 - cannot find symbol
[JavaCompiler] ❌ messages.java:42 -   symbol:   variable NO_PERMISION
[JavaCompiler] ❌ messages.java:42 -   location: class MessagesConfig
```

Что происходит дальше:

| Последствие | Пояснение |
|-------------|-----------|
| Файл **не применяется** | Класс не собрался — значит, переопределений из него нет |
| Плагин **продолжает работать** | Он не падает: используются встроенные тексты |
| Встроенные тексты **не меняются** | Ваши правки в этом файле не вступят в силу, пока ошибка не исправлена |

Поэтому: после правки `messages.java` всегда смотрите консоль. Успех выглядит так:

```text
[Messages] items/messages.java applied (runtime load())
```

Если этой строки нет — правка не применилась.

Ещё два вспомогательных метода класса `MessagesConfig`, которые пригодятся:

| Метод | Что делает |
|-------|-----------|
| `MessagesConfig.colorize(String)` | Заменяет `&` на `§` |
| `MessagesConfig.format(String message, String... replacements)` | Подставляет пары «что заменить → на что» и раскрашивает текст |

```java
String text = MessagesConfig.format("&aКуплено: &e{count} &7штук", "{count}", "3");
// → "§aКуплено: §e3 §7штук"
```

## Частые ошибки

| Симптом | Причина | Что делать |
|---------|---------|------------|
| Правки в `messages.java` не видны | Файл не применился (ошибка компиляции) | Посмотрите консоль: ищите `[JavaCompiler] ❌ messages.java:…` |
| Правки не видны, хотя ошибок нет | Не выполнен `/ci reload` | Выполните `/ci reload` |
| Правки не видны после reload | В файле нет метода `load()` | Добавьте `public static void load() { … }` |
| В консоли `[Messages] items/messages.java was not applied` | Файл собрался, но метода `load()` нет или он не `static` | Проверьте сигнатуру: `public static void load()` |
| Префикс изменился не везде | Строки, использующие `PREFIX`, не переписаны | Перепишите и их: `MessagesConfig.X = MessagesConfig.PREFIX + "…"` |
| Текст модуля не переопределяется | Путь в `set()` отличается от пути в модуле | Пути должны совпадать символ в символ |
| Текст модуля пропал после reload | Модуль не снял текст в `onDisable()` | Добавьте `unregisterMessage(path)` |
| `cannot find symbol` | Опечатка в имени поля | Сверьтесь со списком полей выше |
| Вместо русского текста «кракозябры» | Файл сохранён не в UTF-8 | Сохраните файл в UTF-8 (в VS Code — «Save with encoding → UTF-8») |
| Файл переименовали — правки не действуют | Плагин ищет класс `items.messagesItem` — имя файла важно | Верните имя `items/messages.java` |

---

Смотрите также:

- [Команды](08-commands.md) — как команда читает сообщение через `message(path, default)`.
- [Модули](10-modules.md) — `registerMessage` / `getMessage` в модуле.
- [Плейсхолдеры](09-placeholders.md) — подстановки `%dci_…%` внутри сообщений.
- [Конфигурация](14-configuration.md) — остальные настройки плагина.
