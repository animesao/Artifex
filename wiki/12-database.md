# База данных

## Что ты узнаешь

- зачем плагину вообще база данных, если сам он ничего не хранит;
- как устроен SQLite (файл `data.db`) и почему для старта ничего настраивать не нужно;
- все ключи секции `database.mysql.*` и переменная окружения `DCI_MYSQL_PASSWORD`;
- почему после смены типа базы нужен **перезапуск** сервера, а не `/ci reload`;
- полный API `DatabaseManager` для авторов модулей: подключение, запросы, асинхронные варианты, upsert;
- почему имена таблиц и колонок проверяются и что будет, если имя неправильное;
- `YamlStorage` — когда вместо базы достаточно обычного YAML-файла;
- готовый пример модуля, который хранит статистику игроков (SQLite и MySQL);
- где лежат резервные копии и как их делать.

## Предусловия

- Плагин Artifex установлен, сервер запускается — см. [Установка](01-getting-started.md).
- Вы понимаете, что такое SQL-запрос и таблица — хотя бы на уровне `SELECT` и `INSERT`.
- Для модулей на Java вам понадобится [Предметы в Java](04-java-items.md) и [Модули](10-modules.md).
- Общий список страниц — в [Содержании вики](README.md).

## Содержание

- [Зачем плагину база данных](#зачем-плагину-база-данных)
- [SQLite по умолчанию](#sqlite-по-умолчанию)
- [MySQL](#mysql)
  - [Ключи database.mysql](#ключи-databasemysql)
  - [Пароль в переменной окружения](#пароль-в-переменной-окружения)
  - [Безопасность MySQL](#безопасность-mysql)
- [Переключение базы](#переключение-базы)
- [Что лежит в базе, а что нет](#что-лежит-в-базе-а-что-нет)
- [API DatabaseManager](#api-databasemanager)
  - [Подключение и состояние](#подключение-и-состояние)
  - [Сырой SQL](#сырой-sql)
  - [Готовые операции с таблицей](#готовые-операции-с-таблицей)
  - [Чтение данных](#чтение-данных)
  - [Асинхронные методы](#асинхронные-методы)
  - [Upsert: вставка или обновление](#upsert-вставка-или-обновление)
  - [Почему имена таблиц и колонок проверяются](#почему-имена-таблиц-и-колонок-проверяются)
- [YamlStorage: маленькие данные в YAML](#yamlstorage-маленькие-данные-в-yaml)
- [Готовый пример: статистика игроков](#готовый-пример-статистика-игроков)
  - [Версия для SQLite](#версия-для-sqlite)
  - [Версия для MySQL](#версия-для-mysql)
- [Резервные копии](#резервные-копии)
- [Подводные камни](#подводные-камни)
- [Смотрите также](#смотрите-также)

## Зачем плагину база данных

Само ядро Artifex **не хранит** данные игроков. Предметы лежат в YAML-файлах, сообщения — в `items/messages.java`, кулдауны — в `cooldowns.yml`. Ядро работает как «правила»: оно читает описания предметов и реагирует на события.

База данных нужна **вам** — авторам модулей и Java-предметов. Типичные задачи:

| Задача | Что нужно |
|--------|-----------|
| Статистика игроков (убийства, смерти, время игры) | Таблица с UUID игрока и числами |
| Промокоды с ограничением по числу активаций | Таблица кодов и счётчик использований |
| Баланс собственной валюты | Таблица балансов (или модуль `vault` с `economy.storage: own`) |
| Своя мини-игра или квест | Любые свои таблицы |
| Список зарегистрированных игроков | Таблица регистраций |

Ключевая мысль: **база данных — это ваше личное хранилище**. Плагин даёт вам удобную обёртку над SQLite/MySQL, но не создаёт за вас никаких таблиц и не решает, что хранить.

## SQLite по умолчанию

**SQLite** — это файловая база данных: вся база лежит в одном файле, никакого отдельного сервера не нужно.

```yaml
database:
  type: sqlite
```

Что происходит:

| Что | Подробности |
|-----|--------------|
| Файл базы | `plugins/Artifex/data.db` |
| Когда создаётся | Автоматически при первом подключении |
| Что нужно ставить | Ничего — драйвер SQLite уже лежит внутри jar плагина |
| Где живёт | Рядом с плагином, на том же диске |
| Ограничения | Один сервер. Один процесс пишет в файл последовательно, поэтому тяжёлые запросы лучше делать асинхронно |

Это оптимальный вариант для большинства серверов: игроков до нескольких сотен, модулей — два-три.

> Файл `data.db` попадает в резервную копию автоматически — см. [Резервные копии](#резервные-копии).

## MySQL

**MySQL** — это отдельный сервер базы данных. Он нужен, если:

- серверов несколько, а данные должны быть общими (например, общий баланс на сетке серверов);
- игроков тысячи и запросов много;
- у вас уже есть свой MySQL и вы хотите использовать его.

### Ключи database.mysql

Все ключи лежат в секции `database.mysql` файла `plugins/Artifex/config.yml`.

| Ключ | Тип | По умолчанию | Что делает |
|------|-----|--------------|------------|
| `database.type` | строка | `sqlite` | `sqlite` или `mysql`. Выбор хранилища |
| `database.mysql.host` | строка | `127.0.0.1` | Адрес сервера MySQL. `127.0.0.1` — тот же компьютер |
| `database.mysql.port` | число | `3306` | Порт MySQL |
| `database.mysql.database` | строка | `artifex` | Имя базы данных (её нужно создать заранее) |
| `database.mysql.username` | строка | `artifex` | Логин пользователя MySQL |
| `database.mysql.password` | строка | `'change-me'` | Пароль пользователя. **Замените** на свой |
| `database.mysql.use-ssl` | булево | `false` | Шифровать соединение. Для удалённой базы — `true` |
| `database.mysql.verify-server-certificate` | булево | `false` | Проверять сертификат сервера MySQL (и имя хоста). Включай вместе с `use-ssl: true` |
| `database.mysql.pool-size` | число | `5` | Размер пула соединений (сколько соединений держим открытыми) |
| `database.mysql.connection-timeout-ms` | число | `5000` | Сколько миллисекунд ждать соединения, прежде чем признать ошибку |
| `database.mysql.leak-detection-ms` | число | `0` | Через сколько мс «зависшего» соединения писать предупреждение. `0` — выключено |

> **Шифрование и проверка сертификата.** `use-ssl: true` включает TLS, но сам
> по себе сертификат сервера **не проверяется** — шифрование есть, а
> подмену сервера (MITM) поймать нечем. Для этого нужен
> `verify-server-certificate: true` (тогда используется `sslMode=VERIFY_IDENTITY`
> — проверяются и сертификат, и имя хоста).
>
> При `use-ssl: false` пароль и игровые данные передаются **открытым текстом** —
> плагин пишет об этом предупреждение в лог при подключении.
> Пароль можно не хранить в `config.yml`, а задать переменной окружения
> `DCI_MYSQL_PASSWORD`.

Пример полной настройки:
```yaml
database:
  type: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: artifex
    username: artifex
    password: 'change-me'
    use-ssl: true
    verify-server-certificate: true
    pool-size: 5
    connection-timeout-ms: 5000
    leak-detection-ms: 0
```

Базу и пользователя создаёт администратор MySQL:

```sql
CREATE DATABASE artifex CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'artifex'@'localhost' IDENTIFIED BY 'СЛОЖНЫЙ_ПАРОЛЬ';
GRANT ALL PRIVILEGES ON artifex.* TO 'artifex'@'localhost';
FLUSH PRIVILEGES;
```

### Пароль в переменной окружения

Чтобы не держать пароль в файле, есть переменная окружения **`DCI_MYSQL_PASSWORD`**.

| Правило | Значение |
|---------|----------|
| Что делает | Подставляется вместо `database.mysql.password` |
| Приоритет | Переменная окружения **важнее** значения в `config.yml` |
| Когда берётся | Только если переменная задана и не пустая (`isBlank()`) |
| Где задаётся | В окружении процесса сервера |

Пример для Linux (systemd-сервис):

```bash
# Переменная для конкретного сервиса
sudo systemctl edit myserver
# В открывшемся файле:
[Service]
Environment=DCI_MYSQL_PASSWORD=СЛОЖНЫЙ_ПАРОЛЬ
```

Пример для Windows (PowerShell, до запуска):

```bash
$env:DCI_MYSQL_PASSWORD = "СЛОЖНЫЙ_ПАРОЛЬ"
.\start.bat
```

Если переменная задана, значение `password` в `config.yml` можно оставить заглушкой — оно не будет использоваться.

### Безопасность MySQL

| Правило | Почему |
|---------|--------|
| Не выдавайте пользователю права администратора MySQL | Ему нужны только операции с таблицами своей базы |
| Не открывайте MySQL всему интернету | Закройте порт firewall-ом, для удалённой базы включите `use-ssl: true` |
| Не публикуйте пароль в Git | Используйте `DCI_MYSQL_PASSWORD` |
| Не пишите пароль в Java-файл предмета | Файл компилируется и исполняется на сервере — пароль утечёт |
| Делайте бэкапы базы | Копию делает MySQL, а не плагин |

## Переключение базы

> **Нужен полный перезапуск сервера.** Команда `/ci reload` подключение к базе не пересоздаёт.

Причина простая: тип базы читается **один раз**, при создании `DatabaseManager` — в конструкторе:

```java
this.type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase(Locale.ROOT);
```

Поэтому порядок такой:

1. Остановите сервер.
2. Скопируйте `data.db`, если он там есть.
3. Создайте базу MySQL и пользователя.
4. Поменяйте `database.type: mysql` и заполните `database.mysql.*`.
5. Запустите сервер.
6. Проверьте в консоли: `Database connected: MySQL (127.0.0.1:3306/artifex)`.

Данные из старой SQLite-базы **не переносятся автоматически**. Если они нужны, переносите их скриптом (экспорт в CSV/SQL, импорт в MySQL).

Обратное переключение (MySQL → SQLite) — то же самое: перезапуск, `database.type: sqlite`.

## Что лежит в базе, а что нет

Это важно, чтобы не искать данные не там.

| Данные | Где хранится | Файл |
|---------|--------------|------|
| Ваши таблицы (статистика, промокоды, балансы) | SQLite или MySQL | `data.db` / ваша база MySQL |
| Описания YAML-предметов | Файлы | `items/*.yml` |
| Java-предметы, команды, плейсхолдеры | Файлы + скомпилированные классы | `items/*.java`, `compiled/`, `cache/` |
| Тексты сообщений | Файл | `items/messages.java` |
| Кулдауны игроков | YAML-файл | `plugins/Artifex/cooldowns.yml` |
| Настройки плагина | YAML-файл | `config.yml` |
| Мелкие данные модуля (списки, промо-настройки) | YAML-файл | `plugins/Artifex/storage/*.yml` |

Про **кулдауны** стоит знать отдельно. Их плагин хранит не в базе, а в `cooldowns.yml`: файл читается при старте сервера, а записи с истёкшим временем (`expires`) отбрасываются. Поэтому после перезапуска сервера активные кулдауны сохраняются, а просроченные — нет.

## API DatabaseManager

Менеджер базы доступен из любого места:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

В модуле у вас уже есть поле `plugin` — можно писать `plugin.getDatabaseManager()`.

### Подключение и состояние

| Метод | Возвращает | Что делает |
|-------|------------|------------|
| `connect()` | `boolean` | Подключается к базе, взятой из `database.type`. `true` — успех |
| `disconnect()` | — | Закрывает соединение (SQLite) или пул (MySQL) |
| `isConnected()` | `boolean` | Соединение живое? |
| `reconnect()` | — | Переподключиться: сначала отключиться, потом подключиться |
| `getType()` | `String` | `"sqlite"` или `"mysql"` |
| `isMySql()` | `boolean` | `true`, если выбран MySQL |
| `getConnection()` | `java.sql.Connection` | «Сырое» соединение JDBC. Для MySQL его **нужно закрыть** |
| `shutdownAsync()` | — | Останавливает фоновые потоки запросов (вызывается при выключении плагина) |

Подключение плагин выполняет сам при старте. Вам вызывать `connect()` не нужно — только если вы переносите базу «на лету».

### Сырой SQL

| Метод | Возвращает | Что делает |
|-------|------------|------------|
| `execute(String sql, Object... params)` | `boolean` | Выполняет любой SQL. `true`, если ошибок не было |
| `executeUpdate(String sql, Object... params)` | `int` | Выполняет SQL и возвращает число затронутых строк. `-1` — ошибка SQL |

Значения передаются параметрами `?` — так безопаснее, чем вставлять их в строку:

```java
db.execute("INSERT INTO player_stats(uuid, name) VALUES (?, ?)", uuid, name);
```

Число затронутых строк важно, когда нужно понять, сработало ли условие:

```java
int rows = db.executeUpdate(
    "UPDATE player_stats SET kills = kills + 1 WHERE uuid = ? AND kills < ?",
    uuid, 100);
if (rows == 1) {
    // запись действительно обновилась
}
```

### Готовые операции с таблицей

Эти методы сами собирают `INSERT` / `UPDATE` / `DELETE` — писать SQL не нужно.

| Метод | Возвращает | Что делает |
|-------|------------|------------|
| `createTable(String table, String columns)` | `boolean` | `CREATE TABLE IF NOT EXISTS`. `columns` — строка с описанием колонок |
| `tableExists(String table)` | `boolean` | Проверяет, есть ли таблица |
| `insert(String table, Map<String, Object> data)` | `boolean` | Вставляет строку. Ключи карты — колонки, значения — данные |
| `update(String table, Map<String, Object> data, String where, Object... whereParams)` | `boolean` | Обновляет колонки из карты по условию `where` |
| `delete(String table, String where, Object... params)` | `boolean` | Удаляет строки по условию |
| `increment(String table, String column, String where, Object... whereParams)` | `boolean` | Увеличивает колонку на 1 |
| `add(String table, String column, double amount, String where, Object... whereParams)` | `boolean` | Прибавляет к колонке число |

Пример:

```java
Map<String, Object> row = new HashMap<>();
row.put("uuid", uuid.toString());
row.put("name", player.getName());
row.put("kills", 0);
db.insert("player_stats", row);

db.increment("player_stats", "kills", "uuid = ?", uuid.toString());
db.add("player_stats", "points", 25.0, "uuid = ?", uuid.toString());
db.update("player_stats", Map.of("name", "NewName"), "uuid = ?", uuid.toString());
db.delete("player_stats", "uuid = ?", uuid.toString());
```

### Чтение данных

| Метод | Возвращает | Что делает |
|-------|------------|------------|
| `queryInt(String sql, Object... params)` | `int` | Первое значение первой строки как целое. `0`, если пусто |
| `queryString(String sql, Object... params)` | `String` | Первое значение первой строки как строка. `null`, если пусто |
| `queryDouble(String sql, Object... params)` | `double` | Первое значение первой строки как число. `0.0`, если пусто |
| `queryBoolean(String sql, Object... params)` | `boolean` | Значение как `true/false`. Число `!= 0` — `true` |
| `queryAll(String sql, Object... params)` | `List<Map<String, Object>>` | Все строки. Каждая строка — карта «колонка → значение» |
| `queryOne(String sql, Object... params)` | `Map<String, Object>` | Первая строка или `null` |

```java
int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid);
String name = db.queryString("SELECT name FROM player_stats WHERE uuid = ?", uuid);
boolean active = db.queryBoolean("SELECT active FROM accounts WHERE uuid = ?", uuid);

List<Map<String, Object>> top = db.queryAll(
    "SELECT name, kills FROM player_stats ORDER BY kills DESC LIMIT 10");

for (Map<String, Object> row : top) {
    String playerName = String.valueOf(row.get("name"));
    int playerKills = ((Number) row.get("kills")).intValue();
    // ...
}
```

### Асинхронные методы

JDBC-запросы нельзя выполнять в главном потоке Paper — сервер начнёт «дёргаться». Для этого есть асинхронные варианты: SQL выполняется в отдельном потоке `Artifex-Database`.

| Метод | Возвращает |
|-------|------------|
| `executeAsync(String sql, Object... params)` | `CompletableFuture<Boolean>` |
| `executeUpdateAsync(String sql, Object... params)` | `CompletableFuture<Integer>` |
| `queryIntAsync(String sql, Object... params)` | `CompletableFuture<Integer>` |
| `queryStringAsync(String sql, Object... params)` | `CompletableFuture<String>` |
| `queryAllAsync(String sql, Object... params)` | `CompletableFuture<List<Map<String, Object>>>` |

Правило: **SQL — асинхронно, Bukkit API — только в главном потоке**.

```java
db.queryIntAsync("SELECT uses FROM promo_codes WHERE code = ?", code)
    .thenAccept(uses -> Main.getInstance().getServer().getScheduler().runTask(
        Main.getInstance(),
        () -> player.sendMessage("Осталось использований: " + uses)
    ));
```

Что нельзя делать в асинхронном потоке:

- `player.sendMessage(...)`, `player.teleport(...)`, `Bukkit.getScheduler().runTask(...)` напрямую — только через `runTask`;
- `Inventory` игрока;
- любые обращения к миру.

### Upsert: вставка или обновление

**Upsert** — это «вставить, а если такая строка уже есть — обновить». Синтаксис в SQLite и MySQL разный, поэтому плагин даёт готовый генератор запроса.

```java
public String upsertSql(String table, String columns, String values, String updateClause)
```

| База | Что вернёт метод |
|------|-----------------|
| SQLite | `INSERT INTO t (cols) VALUES (vals) ON CONFLICT DO UPDATE SET ...` |
| MySQL | `INSERT INTO t (cols) VALUES (vals) ON DUPLICATE KEY UPDATE ...` |

Использование:

```java
String sql = db.upsertSql(
    "player_stats",
    "uuid, name, kills",
    "?, ?, ?",
    "name = VALUES(name), kills = VALUES(kills)");
db.execute(sql, uuid.toString(), name, kills);
```

### Почему имена таблиц и колонок проверяются

Значения в запрос передаются параметрами `?` — это безопасно. А вот **имена таблиц и колонок** в SQL подставляются только строкой: параметр там недопустим.

Если бы плагин подставлял любое имя как есть, модуль (или данные из модуля) мог бы подсунуть в SQL что угодно — это путь к SQL-инъекции через код модуля. Поэтому имена проверяются по жёсткому шаблону:

```
[A-Za-z_][A-Za-z0-9_]{0,63}
```

| Правило | Значение |
|---------|----------|
| Первый символ | Только латинская буква `A-Z`/`a-z` или подчёркивание `_` |
| Остальные символы | Латинские буквы, цифры `0-9` и подчёркивание `_` |
| Длина | От 1 до 64 символов |
| Другие символы | Запрещены: пробелы, точка, кавычки, дефис, кириллица |

Что будет, если имя не прошло проверку:

| Метод | Что произойдёт |
|-------|----------------|
| `createTable`, `insert`, `update`, `delete`, `increment`, `add` | В лог уходит предупреждение `SQL: Invalid SQL identifier: <имя>`, метод возвращает `false`. Запрос **не выполняется** |
| `upsertSql` | Бросается исключение `IllegalArgumentException` с тем же текстом |

Практические советы:

```java
// Плохо: точка и дефис запрещены
db.queryAll("SELECT * FROM my-table");      // таблица не создастся
db.insert("player.stats", data);            // вернёт false

// Хорошо: только буквы, цифры и подчёркивание
db.insert("player_stats", data);
db.queryAll("SELECT * FROM dci_vault_balances");
```

Имя таблицы в SQL-запросе (`execute`, `queryAll` и т. д.) можно писать как угодно — проверяются только имена, которые вы передаёте отдельным аргументом в `createTable` / `insert` / `update` / `delete` / `increment` / `add` / `upsertSql`.

## YamlStorage: маленькие данные в YAML

**`YamlStorage`** — простая обёртка над YAML-файлом в папке `storage/`. Это обычный файл, который можно открыть и отредактировать руками.

Когда она удобнее базы:

- список промокодов или промо-версий, который вы правите вручную;
- настройки модуля, которые хочется видеть в текстовом редакторе;
- прототип, который точно не вырастет до тысяч строк.

Когда база лучше: много игроков, частые записи, сложная статистика.

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.YamlStorage;

YamlStorage storage = new YamlStorage(Main.getInstance(), "promo.yml");

storage.set("codes.DEMO2026.reward", "diamond:3");
storage.set("codes.DEMO2026.enabled", true);
storage.save();

String reward = storage.getString("codes.DEMO2026.reward", "");
boolean enabled = storage.getBoolean("codes.DEMO2026.enabled", false);
```

| Метод | Возвращает | Что делает |
|-------|------------|------------|
| `new YamlStorage(Main plugin, String fileName)` | — | Создаёт объект. Файл лежит в `plugins/Artifex/storage/<fileName>` |
| `reload()` | — | Перечитывает файл с диска |
| `contains(String path)` | `boolean` | Есть ли путь |
| `getString(String path, String def)` | `String` | Значение или `def` |
| `getInt(String path, int def)` | `int` | Значение или `def` |
| `getBoolean(String path, boolean def)` | `boolean` | Значение или `def` |
| `getStringList(String path)` | `List<String>` | Список строк |
| `set(String path, Object value)` | — | Записывает значение в память |
| `save()` | `boolean` | Сохраняет файл на диск |
| `getFile()` | `File` | Файл, где всё это лежит |

Важно: все методы `YamlStorage` **синхронные** и должны вызываться в главном потоке сервера. Не вызывайте `save()` каждый тик — это запись файла на диск.

## Готовый пример: статистика игроков

Модуль, который считает убийства игроков и хранит их в базе. Класс можно положить в `items/stats-module/stats-module.java` — плагин скомпилирует его сам.

### Версия для SQLite

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

import java.util.UUID;

/**
 * Статистика игроков в базе данных плагина.
 * Работает и с SQLite, и с MySQL — различия закрыты внутри DatabaseManager.
 */
public final class PlayerStats {

    private PlayerStats() {}

    private static DatabaseManager db() {
        return Main.getInstance().getDatabaseManager();
    }

    /** Создаёт таблицу, если её ещё нет. Вызовите один раз при старте модуля. */
    public static void createTable() {
        DatabaseManager db = db();
        if (!db.tableExists("player_stats")) {
            db.createTable("player_stats",
                "uuid VARCHAR(36) PRIMARY KEY, "
              + "name VARCHAR(32) NOT NULL, "
              + "kills INT NOT NULL DEFAULT 0, "
              + "deaths INT NOT NULL DEFAULT 0");
        }
    }

    /** Добавляет убийство игроку. */
    public static void addKill(UUID uuid, String name) {
        DatabaseManager db = db();

        // Строка должна существовать — создаём её, если её ещё нет.
        // Синтаксис "вставить, если нет" у SQLite и MySQL различается.
        String insert = db.isMySql()
            ? "INSERT IGNORE INTO player_stats (uuid, name, kills, deaths) VALUES (?, ?, 0, 0)"
            : "INSERT OR IGNORE INTO player_stats (uuid, name, kills, deaths) VALUES (?, ?, 0, 0)";
        db.execute(insert, uuid.toString(), name);

        // Теперь увеличиваем счётчик.
        db.increment("player_stats", "kills", "uuid = ?", uuid.toString());
    }

    /** Добавляет смерть игроку. */
    public static void addDeath(UUID uuid) {
        db().increment("player_stats", "deaths", "uuid = ?", uuid.toString());
    }

    /** Сколько убийств у игрока. */
    public static int getKills(UUID uuid) {
        return db().queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid.toString());
    }

    /** Сколько смертей у игрока. */
    public static int getDeaths(UUID uuid) {
        return db().queryInt("SELECT deaths FROM player_stats WHERE uuid = ?", uuid.toString());
    }
}
```

Вызов из обработчика событий (например, из Java-предмета или модуля):

```java
// Асинхронно: запрос не должен блокировать главный поток сервера.
plugin.getDatabaseManager().executeUpdateAsync(
    "UPDATE player_stats SET kills = kills + 1 WHERE uuid = ?",
    player.getUniqueId().toString()
);
```

### Версия для MySQL

Код выше **уже работает с MySQL** — `DatabaseManager` сам выбирает правильный диалект. Но если вам нужна настоящая разница, посмотрите на две вещи:

**1. Автоинкремент.** В MySQL это `AUTO_INCREMENT`, в SQLite — `INTEGER PRIMARY KEY`:

```java
String idColumns = db.isMySql()
    ? "id BIGINT PRIMARY KEY AUTO_INCREMENT"
    : "id INTEGER PRIMARY KEY";
db.createTable("player_history", idColumns + ", uuid VARCHAR(36), ts BIGINT");
```

**2. Upsert через `upsertSql`.** Одна строка кода работает с обеими базами:

```java
DatabaseManager db = plugin.getDatabaseManager();

// Один и тот же код для SQLite и MySQL:
String sql = db.upsertSql(
    "player_stats",
    "uuid, name, kills",
    "?, ?, ?",
    "name = VALUES(name), kills = VALUES(kills)");

db.execute(sql, uuid.toString(), name, 0);
```

Что здесь происходит: если строки с таким `uuid` ещё нет — она создаётся; если есть — обновляются `name` и `kills`.

> В MySQL конструкция `VALUES(col)` внутри `ON DUPLICATE KEY UPDATE` считается устаревшей начиная с MySQL 8.0.20 (там рекомендуют алиас строки `AS new`). Если ваш сервер её отвергает — используйте вариант из [Версия для SQLite](#версия-для-sqlite) с `INSERT IGNORE` + `increment()`.

## Резервные копии

Плагин умеет делать бэкап **всего содержимого** плагина: папки `items/`, файла `config.yml` и базы `data.db`.

| Что | Команда / настройка | Что делает |
|-----|---------------------|------------|
| Ручной бэкап | `/ci backup` | Создаёт копию прямо сейчас |
| Автобэкап | `backup.on-reload: true` | Копия создаётся перед каждым `/ci reload` |
| Сколько хранить | `backup.keep: 5` | Столько последних копий остаётся, остальные удаляются (минимум 1) |

Куда складываются копии:

```text
plugins/Artifex/backups/
├── items-20261021-142530/
│   ├── runes.yml
│   ├── armor.yml
│   ├── messages.java
│   ├── config.yml
│   └── data.db
└── items-20261021-151200/
    └── ...
```

Внутри копии — **полная копия папки `items/`** (со всеми подпапками и модулями), плюс `config.yml` и `data.db`, если они есть.

Как вернуться к копии:

```bash
# 1. Остановите сервер
# 2. Скопируйте нужную папку обратно
cp -r plugins/Artifex/backups/items-20261021-142530/items/. plugins/Artifex/items/
# 3. Запустите сервер
```

Что бэкап **не** покрывает:

| Что | Почему |
|-----|--------|
| Данные в MySQL | Они лежат на сервере MySQL — бэкапите их средствами MySQL (`mysqldump`) |
| Классы в `compiled/` и `cache/` | Это кэш компилятора — он пересоздастся сам |
| `storage/*.yml` | Эти файлы тоже можно копировать вручную |
| `cooldowns.yml` | Кулдауны — временные данные, их сброс безопасен |

## Подводные камни

| Проблема | Причина | Что делать |
|----------|---------|------------|
| `MySQL connection error: ...` | Неверные хост, порт, база, пользователь или пароль | Проверьте `database.mysql.*`, firewall и права пользователя MySQL |
| `Unknown database 'artifex'` | База не создана | Создайте её через SQL-команду администратора |
| `Access denied for user` | Неверный пароль или нет `GRANT` | Пересоздайте пользователя и выдайте права на базу |
| После смены `database.type` ничего не изменилось | Тип базы читается только при старте | Перезапустите сервер полностью |
| `Table 'x' doesn't exist` | Таблицу создают один раз, при старте модуля | Вызовите `createTable()` до первого запроса |
| `SQL: Invalid SQL identifier: ...` | В имени таблицы или колонки есть запрещённые символы | Только буквы, цифры и `_`, начиная с буквы или `_` |
| `database is locked` | Два потока пишут в один SQLite-файл | Используйте асинхронные методы; проверьте другие плагины |
| Лагает при `queryAll` | Тяжёлый запрос выполняется в главном потоке | Перенесите в `queryAllAsync` |
| `NullPointerException` при чтении `row.get("kills")` | Колонки может не быть | Проверяйте `instanceof Number` и на `null` |
| `data.db` не создан | SQLite-файл создаётся при первом подключении, а не при старте | Проверьте права на запись в папку плагина |
| Пароль MySQL виден в Git | Он лежит в `config.yml` | Используйте `DCI_MYSQL_PASSWORD` и смените пароль |

## Смотрите также

- [Конфигурация](14-configuration.md) — все ключи `config.yml`, включая секцию `database`.
- [Модули](10-modules.md) — как устроен модуль, в котором живёт ваш код.
- [Предметы в Java](04-java-items.md) — как писать логику на Java.
- [Плейсхолдеры](09-placeholders.md) — как показать данные из базы в тексте.
- [Решение проблем](17-troubleshooting.md) — если база не подключается.
- [Содержание вики](README.md).
