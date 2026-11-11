# Сообщения и база данных — русский справочник

## 1. Сообщения в версии 1.320.282

Встроенные сообщения хранятся в классе:

```text
src/main/java/me/dcplugin/dcustomitems/api/config/MessagesConfig.java
```

При первом запуске плагин создаёт:

```text
plugins/Artifex/items/messages.java
```

Этот файл — рабочий конфиг сообщений: плагин компилирует его вместе с остальными Java-файлами и вызывает статический `load()` при старте и при каждом `/ci reload`. Изменения вступают в силу без пересборки JAR.

### Как изменить сообщение

1. Откройте `plugins/Artifex/items/messages.java`.
2. Измените нужное поле внутри метода `load()` (например `MessagesConfig.PREFIX = "...";`).
3. Выполните `/ci reload`.
4. Проверьте результат; при ошибке компиляции плагин сообщит об этом в консоли и оставит стандартные сообщения.

Пример (внутри `load()`):

```java
MessagesConfig.PREFIX = "&8[&bМой сервер&8] &r";
MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + "&aПолучено: &e{item}";
```

Если файл был удалён — он создастся заново при следующем старте.

Не удаляйте `{item}`, `{player}`, `{count}`, `{error}` и другие placeholders, если код должен подставлять значение.

### Цвета

Используйте старый формат Bukkit:

```text
&a зелёный   &c красный   &e жёлтый   &b бирюзовый
&l жирный    &o курсив    &r сброс цвета
```

## 2. Где смотреть ошибки сообщений

Если после изменения нет результата:

- проверьте, что изменения внутри метода `load()` в `items/messages.java`;
- выполните `/ci reload` и посмотрите консоль: компилятор пишет ошибки Java-файлов;
- проверьте, что поле существует в вашей версии плагина (см. `MessagesConfig.java` в исходниках);
- если файл не создаётся/не применяется — перезапустите сервер.

## 3. SQLite и MySQL-файл

Полная инструкция по выбору SQLite/MySQL/YAML, подключению MySQL и промокодам: [DATABASE_RU.md](DATABASE_RU.md).

По умолчанию плагин использует SQLite-файл:

```text
plugins/Artifex/data.db
```

Не редактируйте его обычным текстовым редактором. Перед экспериментами остановите сервер и скопируйте файл:

```bash
cp plugins/Artifex/data.db plugins/Artifex/data.db.backup
```

Не удаляйте `data.db`, если нужно сохранить прогресс игроков.

## 4. DatabaseManager для Java-модулей

В Java-механике можно получить менеджер через главный класс плагина:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

Доступны основные методы:

- `connect()` и `disconnect()`;
- `reconnect()`;
- `createTable(table, columns)`;
- `execute(sql, params...)`;
- `insert(table, data)`;
- `update(table, data, where, params...)`;
- `delete(table, where, params...)`;
- `queryInt`, `queryString`, `queryDouble`, `queryBoolean`;
- `queryOne` и `queryAll`;
- `increment` и `add`.

Пример таблицы статистики:

```java
db.createTable("player_stats", "uuid TEXT PRIMARY KEY, kills INTEGER DEFAULT 0");
db.execute(
    "INSERT INTO player_stats(uuid, kills) VALUES(?, 0)",
    player.getUniqueId().toString()
);
db.increment(
    "player_stats",
    "kills",
    "uuid = ?",
    player.getUniqueId().toString()
);
int kills = db.queryInt(
    "SELECT kills FROM player_stats WHERE uuid = ?",
    player.getUniqueId().toString()
);
```

Параметры передавайте через `?`, а не вставляйте пользовательский текст прямо в SQL. Если строка уже существует, обработайте это отдельно или используйте `upsertSql()` с SQL, подходящим для выбранной базы.

## 5. Производительность базы

Не выполняйте SQL-запросы каждый тик и не записывайте данные при каждом движении игрока. Лучше:

- хранить часто используемое значение в памяти;
- сохранять изменения при важных событиях;
- использовать одну таблицу для связанных данных;
- не делать большие запросы во время входа всех игроков;
- закрывать соединение при выключении модуля;
- проверять работу на копии `data.db`.

## 6. Резервная копия перед обновлением

```bash
cp -a plugins/Artifex plugins/Artifex.backup
```

Сохраняйте отдельно:

- `items/`;
- `config.yml`;
- `data.db`;
- папки модулей и меню.
