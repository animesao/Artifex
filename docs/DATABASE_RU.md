# Хранение данных: SQLite, MySQL и YAML

Этот раздел объясняет, где хранить данные механики: промокоды, балансы, квесты, статистику и настройки.

## 1. Как выбрать хранилище

| Вариант | Когда использовать | Файл/сервер |
|---|---|---|
| SQLite | Один сервер, небольшие данные, самый простой старт | `plugins/Artifex/data.db` |
| MySQL | Несколько серверов, общий баланс, много игроков | Отдельный MySQL-сервер |
| YAML | Небольшие настройки и списки, которые удобно редактировать вручную | `plugins/Artifex/storage/*.yml` |

**Рекомендация новичку:** начните с SQLite. Код Java использует один и тот же `DatabaseManager`, поэтому переход на MySQL обычно требует только изменения `config.yml`.

## 2. SQLite по умолчанию

Ничего дополнительно устанавливать не нужно:

```yaml
database:
  type: sqlite
```

Плагин создаёт `data.db` автоматически. SQLite подходит для локальных промокодов, статистики, квестов и небольшого сервера.

## 3. Подключение MySQL

### Шаг 1. Создайте базу и пользователя

Пример SQL для администратора MySQL:

```sql
CREATE DATABASE dcustomitems CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dcustomitems'@'localhost' IDENTIFIED BY 'СЛОЖНЫЙ_ПАРОЛЬ';
GRANT ALL PRIVILEGES ON dcustomitems.* TO 'dcustomitems'@'localhost';
FLUSH PRIVILEGES;
```

Если MySQL находится на другом сервере, вместо `localhost` используйте разрешённый адрес или настройте отдельного пользователя с ограниченным доступом. Не открывайте MySQL всему интернету без firewall и SSL.

### Шаг 2. Настройте `config.yml`

Откройте:

```text
plugins/Artifex/config.yml
```

Укажите:

```yaml
database:
  type: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: dcustomitems
    username: dcustomitems
    password: 'СЛОЖНЫЙ_ПАРОЛЬ'
    use-ssl: false
    pool-size: 5
    connection-timeout-ms: 5000
    leak-detection-ms: 0
```

Для production-сервера рекомендуется:

```yaml
use-ssl: true
```

После изменения типа базы нужен полный перезапуск сервера. `/ci reload` не пересоздаёт подключение к базе.

### Шаг 3. Проверьте лог

При успешном подключении будет сообщение примерно такого вида:

```text
Database connected: MySQL (127.0.0.1:3306/dcustomitems)
```

Если подключение не удалось, проверьте host, порт, имя базы, пароль, firewall и права пользователя.

## 4. Безопасность MySQL

- Не публикуйте пароль в GitHub. Для production можно задать пароль через переменную окружения `DCI_MYSQL_PASSWORD`; она имеет приоритет над `database.mysql.password`.
- Не вставляйте пароль в Java-файл предмета.
- Используйте отдельного пользователя только для базы плагина.
- Не выдавайте пользователю MySQL-права администратора.
- Используйте prepared statements с `?`.
- Для удалённой базы включайте SSL и firewall.
- Делайте резервные копии базы.

## 5. DatabaseManager в Java

Получение менеджера:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

Основные методы:

```java
db.createTable("player_stats", "uuid VARCHAR(36) PRIMARY KEY, kills INT NOT NULL DEFAULT 0");
db.execute("INSERT INTO player_stats(uuid) VALUES(?)", uuid);
db.increment("player_stats", "kills", "uuid = ?", uuid);
int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid);
```

Для совместимости SQLite и MySQL старайтесь использовать простой общий SQL. Если нужен upsert, используйте `db.upsertSql(...)` или отдельный SQL для каждого типа базы.

Метод `getConnection()` предназначен для продвинутого кода. Для MySQL он выдаёт соединение из пула, поэтому его обязательно закрывать:

```java
try (java.sql.Connection connection = db.getConnection()) {
    // Подготовленный SQL-запрос
}
```

Для новичков безопаснее использовать `executeAsync`, `queryIntAsync` и `queryAllAsync`. Не оставляйте MySQL-соединение открытым — это постепенно исчерпает пул.

## 6. Асинхронные запросы

JDBC нельзя выполнять в главном потоке Paper. Используйте async-методы:

```java
db.queryIntAsync("SELECT uses FROM promo_codes WHERE code = ?", code)
    .thenAccept(uses ->
        Main.getInstance().getServer().getScheduler().runTask(
            Main.getInstance(),
            () -> player.sendMessage("Использований: " + uses)
        )
    );
```

Правило простое:

1. SQL выполняется асинхронно.
2. Bukkit API вызывается только через `runTask` на главном потоке.
3. Не делайте SQL-запрос на каждый `PlayerMoveEvent`.
4. Не запускайте бесконечные async-задачи без ограничения.

Доступны:

- `executeAsync`;
- `executeUpdateAsync`;
- `queryIntAsync`;
- `queryStringAsync`;
- `queryAllAsync`.

`executeUpdate` возвращает число изменённых строк. Это важно для промокодов: значение `1` означает успешную активацию, `0` — код уже закончился или не найден.

## 7. Полный пример промокодов

Готовый пример находится в JAR-проекте:

```text
samples/EXAMPLE-promo-code.java
```

Скопируйте его на сервер без префикса `EXAMPLE-`:

```text
plugins/Artifex/items/promo-code.java
```

Выполните:

```text
/ci reload
```

После успешной компиляции появится команда:

```text
/promo DEMO2026
```

Добавить промокод можно SQL-запросом:

```sql
INSERT INTO promo_codes(code, reward_command, max_uses, uses)
VALUES ('DEMO2026', 'give %player% diamond 3', 100, 0);
```

Важно: этот пример показывает основу. Для реального проекта добавьте таблицу использований по UUID, чтобы один игрок не активировал код несколько раз.

## 8. YAML storage

Для маленьких данных можно использовать:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.YamlStorage;

YamlStorage storage = Main.getInstance().createYamlStorage("promo.yml");
storage.set("codes.DEMO2026.reward", "diamond:3");
storage.set("codes.DEMO2026.enabled", true);
storage.save();

String reward = storage.getString("codes.DEMO2026.reward", "");
```

Файл будет создан здесь:

```text
plugins/Artifex/storage/promo.yml
```

YAML удобен для ручного редактирования, но не подходит для частых записей, большого онлайна и сложной статистики. Методы `YamlStorage` синхронные, поэтому не вызывайте `save()` каждый тик.

## 9. Перенос SQLite → MySQL

1. Остановите сервер.
2. Сделайте копию `data.db`.
3. Создайте MySQL-базу и пользователя.
4. Измените `database.type` на `mysql`.
5. Запустите сервер.
6. Таблицы, которые создаёт ваша Java-механика, создадутся в MySQL.
7. Старые данные SQLite автоматически не переносятся — нужен отдельный migration-скрипт.

## 10. Типичные ошибки

| Ошибка | Решение |
|---|---|
| `Communications link failure` | Проверьте host, порт и firewall |
| `Access denied` | Проверьте username, password и GRANT |
| `Unknown database` | Сначала создайте базу |
| `Table doesn't exist` | Вызовите `createTable` до запросов |
| Лаги при использовании MySQL | Перенесите JDBC в async-методы |
| Пароль виден в GitHub | Немедленно смените пароль и удалите его из истории/файлов |
| MySQL не используется | Проверьте `database.type: mysql` и перезапустите сервер |
