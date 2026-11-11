# Messages and Database — English Guide

## 1. Messages in version 1.320.282

Built-in messages are stored in:

```text
src/main/java/me/dcplugin/dcustomitems/api/config/MessagesConfig.java
```

On first start, the plugin creates:

```text
plugins/Artifex/items/messages.java
```

That file is a working message config: the plugin compiles it together with the other Java files and calls its static `load()` on startup and on every `/ci reload`. Changes take effect without rebuilding the JAR.

### How to change a message

1. Open `plugins/Artifex/items/messages.java`.
2. Change the desired field inside the `load()` method (e.g. `MessagesConfig.PREFIX = "...";`).
3. Run `/ci reload`.
4. Verify the result; on a compile error the plugin reports it in the console and keeps the built-in messages.

Example (inside `load()`):

```java
MessagesConfig.PREFIX = "&8[&bMy server&8] &r";
MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + "&aReceived: &e{item}";
```

If the file was deleted, it is recreated on the next start.

Keep `{item}`, `{player}`, `{count}`, `{error}`, and other placeholders when the code supplies those values.

### Colors

Use Bukkit legacy color codes:

```text
&a green   &c red   &e yellow   &b aqua
&l bold    &o italic   &r reset
```

## 2. Troubleshooting messages

If your change has no effect:

- confirm the change is inside the `load()` method in `items/messages.java`;
- run `/ci reload` and read the console: the compiler logs Java source errors;
- confirm the field still exists in your plugin version (see `MessagesConfig.java` in the source);
- if the file is missing or not applied, restart the server.

## 3. SQLite and MySQL storage

For the complete SQLite/MySQL/YAML, MySQL setup, and promo-code guide, see [DATABASE_EN.md](DATABASE_EN.md).

By default, the plugin uses:

```text
plugins/Artifex/data.db
```

Do not edit it with a normal text editor. Stop the server before experimenting and make a copy:

```bash
cp plugins/Artifex/data.db plugins/Artifex/data.db.backup
```

Do not delete `data.db` if player progress must be preserved.

## 4. DatabaseManager in Java modules

A Java mechanic can access the manager through the plugin instance:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

The main operations are:

- `connect()` and `disconnect()`;
- `reconnect()`;
- `createTable(table, columns)`;
- `execute(sql, params...)`;
- `insert(table, data)`;
- `update(table, data, where, params...)`;
- `delete(table, where, params...)`;
- `queryInt`, `queryString`, `queryDouble`, `queryBoolean`;
- `queryOne` and `queryAll`;
- `increment` and `add`.

Example statistics table:

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

Use `?` parameters instead of concatenating user input into SQL. If the row may already exist, handle that case explicitly or use `upsertSql()` with syntax appropriate for the selected database.

## 5. Database performance

Do not query SQLite every tick or write on every player movement. Prefer to:

- keep frequently used values in memory;
- save at meaningful events;
- use one table for related data;
- avoid large queries while many players join;
- close resources when a module is disabled;
- test against a copy of `data.db`.

## 6. Backup before an update

```bash
cp -a plugins/Artifex plugins/Artifex.backup
```

Keep separate backups of:

- `items/`;
- `config.yml`;
- `data.db`;
- module and menu folders.
