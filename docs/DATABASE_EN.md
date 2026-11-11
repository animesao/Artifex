# Data Storage: SQLite, MySQL, and YAML

This guide explains where to store data for promo codes, balances, quests, statistics, and settings.

## 1. Choose a storage type

| Option | Best for | Location |
|---|---|---|
| SQLite | One server, small data, easiest setup | `plugins/Artifex/data.db` |
| MySQL | Multiple servers, shared balances, many players | Separate MySQL server |
| YAML | Small editable settings and lists | `plugins/Artifex/storage/*.yml` |

**Beginner recommendation:** start with SQLite. Java mechanics use the same `DatabaseManager` API, so moving to MySQL normally only requires changing `config.yml`.

## 2. SQLite by default

No extra service is required:

```yaml
database:
  type: sqlite
```

The plugin creates `data.db` automatically. SQLite is suitable for local promo codes, statistics, quests, and a small server.

## 3. Connect MySQL

### Step 1: Create the database and user

Example SQL for a MySQL administrator:

```sql
CREATE DATABASE dcustomitems CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dcustomitems'@'localhost' IDENTIFIED BY 'A_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON dcustomitems.* TO 'dcustomitems'@'localhost';
FLUSH PRIVILEGES;
```

For a remote MySQL server, use an allowed address or a restricted user. Do not expose MySQL to the public internet without a firewall and SSL.

### Step 2: Configure `config.yml`

Open:

```text
plugins/Artifex/config.yml
```

Set:

```yaml
database:
  type: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: dcustomitems
    username: dcustomitems
    password: 'A_STRONG_PASSWORD'
    use-ssl: false
    pool-size: 5
    connection-timeout-ms: 5000
    leak-detection-ms: 0
```

For production, prefer:

```yaml
use-ssl: true
```

Changing the database type requires a full server restart. `/ci reload` does not recreate the database connection.

### Step 3: Check the log

A successful connection looks like:

```text
Database connected: MySQL (127.0.0.1:3306/dcustomitems)
```

If it fails, check the host, port, database name, password, firewall, and user grants.

## 4. MySQL security

- Never publish the password to GitHub. For production, you can set `DCI_MYSQL_PASSWORD` as an environment variable; it takes priority over `database.mysql.password`.
- Do not put the password inside a Java item source file.
- Use a dedicated database user for the plugin.
- Do not grant that user administrator privileges.
- Use prepared statements with `?` parameters.
- Enable SSL and a firewall for remote databases.
- Back up the database.

## 5. DatabaseManager in Java

Get the manager:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

Common methods:

```java
db.createTable("player_stats", "uuid VARCHAR(36) PRIMARY KEY, kills INT NOT NULL DEFAULT 0");
db.execute("INSERT INTO player_stats(uuid) VALUES(?)", uuid);
db.increment("player_stats", "kills", "uuid = ?", uuid);
int kills = db.queryInt("SELECT kills FROM player_stats WHERE uuid = ?", uuid);
```

For SQLite/MySQL compatibility, prefer simple shared SQL. For upserts, use `db.upsertSql(...)` or provide database-specific SQL.

`getConnection()` is an advanced method. For MySQL it leases a connection from the pool, so always close it:

```java
try (java.sql.Connection connection = db.getConnection()) {
    // Prepared SQL query
}
```

Beginners should prefer `executeAsync`, `queryIntAsync`, and `queryAllAsync`. Leaving a MySQL connection open will eventually exhaust the pool.

## 6. Asynchronous queries

JDBC must not run on the Paper main thread. Use async methods:

```java
db.queryIntAsync("SELECT uses FROM promo_codes WHERE code = ?", code)
    .thenAccept(uses ->
        Main.getInstance().getServer().getScheduler().runTask(
            Main.getInstance(),
            () -> player.sendMessage("Uses: " + uses)
        )
    );
```

The rule is:

1. Run SQL asynchronously.
2. Call Bukkit API through `runTask` on the main thread.
3. Do not query SQL on every `PlayerMoveEvent`.
4. Do not create unbounded async tasks.

Available async methods:

- `executeAsync`;
- `executeUpdateAsync`;
- `queryIntAsync`;
- `queryStringAsync`;
- `queryAllAsync`.

`executeUpdate` returns affected rows. This matters for promo codes: `1` means activation succeeded, while `0` means the code is exhausted or missing.

## 7. Complete promo-code example

The ready-made example is:

```text
samples/EXAMPLE-promo-code.java
```

Copy it to the server without the `EXAMPLE-` prefix:

```text
plugins/Artifex/items/promo-code.java
```

Run:

```text
/ci reload
```

After successful compilation, the command is:

```text
/promo DEMO2026
```

Insert a promo code with SQL:

```sql
INSERT INTO promo_codes(code, reward_command, max_uses, uses)
VALUES ('DEMO2026', 'give %player% diamond 3', 100, 0);
```

This is a foundation example. For a production system, add a redemption table keyed by player UUID so one player cannot redeem the same code twice.

## 8. YAML storage

For small data, use:

```java
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.database.YamlStorage;

YamlStorage storage = Main.getInstance().createYamlStorage("promo.yml");
storage.set("codes.DEMO2026.reward", "diamond:3");
storage.set("codes.DEMO2026.enabled", true);
storage.save();

String reward = storage.getString("codes.DEMO2026.reward", "");
```

The file is created at:

```text
plugins/Artifex/storage/promo.yml
```

YAML is convenient for manual editing, but it is not suitable for frequent writes, high player counts, or complex statistics. `YamlStorage` is synchronous, so do not call `save()` every tick.

## 9. Migrate SQLite to MySQL

1. Stop the server.
2. Back up `data.db`.
3. Create the MySQL database and user.
4. Change `database.type` to `mysql`.
5. Start the server.
6. Tables created by your Java mechanic will be created in MySQL.
7. Existing SQLite data is not migrated automatically; write a migration script if needed.

## 10. Common errors

| Error | Solution |
|---|---|
| `Communications link failure` | Check host, port, and firewall |
| `Access denied` | Check username, password, and grants |
| `Unknown database` | Create the database first |
| `Table doesn't exist` | Call `createTable` before queries |
| MySQL causes lag | Move JDBC work to async methods |
| Password appears on GitHub | Change it immediately and remove it from files/history |
| MySQL is not used | Check `database.type: mysql` and restart the server |
