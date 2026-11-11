package me.dcplugin.artifex.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.dcplugin.artifex.Main;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Database manager for the plugin's default SQLite storage and optional MySQL storage.
 *
 * Existing synchronous methods are kept for compatibility. New Java mechanics should
 * prefer the async methods so JDBC work never blocks the Paper main thread.
 */
public class DatabaseManager {

    private final Main plugin;
    private final String dbPath;
    private final String type;
    private Connection sqliteConnection;
    private HikariDataSource mysqlDataSource;
    private final Object sqliteLock = new Object();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "Artifex-Database");
        thread.setDaemon(true);
        return thread;
    });

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "data.db";
        this.type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase(Locale.ROOT);
    }

    /** Connect using database.type from config.yml. */
    public boolean connect() {
        if ("mysql".equals(type)) return connectMySql();
        return connectSqlite();
    }

    private boolean connectSqlite() {
        try {
            Class.forName("org.sqlite.JDBC");
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            plugin.getLogger().info("Database connected: SQLite (" + dbPath + ")");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite driver is not available in the plugin JAR.");
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "SQLite connection error: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean connectMySql() {
        try {
            String host = plugin.getConfig().getString("database.mysql.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "artifex");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");
            String envPassword = System.getenv("ARTIFEX_MYSQL_PASSWORD");
            if (envPassword == null || envPassword.isBlank()) envPassword = System.getenv("DCI_MYSQL_PASSWORD");
            if (envPassword != null && !envPassword.isBlank()) password = envPassword;
            boolean ssl = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
            boolean verifyCert = plugin.getConfig().getBoolean("database.mysql.verify-server-certificate", false);
            int poolSize = Math.max(1, plugin.getConfig().getInt("database.mysql.pool-size", 5));
            long timeout = Math.max(1000L, plugin.getConfig().getLong("database.mysql.connection-timeout-ms", 5000L));

            // sslMode:
            //   VERIFY_IDENTITY — шифрование + проверка сертификата и имени хоста
            //   REQUIRED       — шифрование без проверки сертификата (MITM возможен)
            //   DISABLED       — без шифрования
            String sslMode;
            if (!ssl) {
                sslMode = "DISABLED";
                plugin.getLogger().warning("[DB] MySQL: TLS выключен (database.mysql.use-ssl: false) — "
                        + "пароль и игровые данные передаются открытым текстом.");
            } else if (verifyCert) {
                sslMode = "VERIFY_IDENTITY";
            } else {
                sslMode = "REQUIRED";
                plugin.getLogger().warning("[DB] MySQL: TLS включён, но сертификат сервера НЕ проверяется — "
                        + "включи database.mysql.verify-server-certificate: true, чтобы закрыть MITM.");
            }

            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&sslMode=" + sslMode);
            hikari.setUsername(username);
            hikari.setPassword(password);
            hikari.setMaximumPoolSize(poolSize);
            hikari.setMinimumIdle(Math.min(1, poolSize));
            hikari.setConnectionTimeout(timeout);
            hikari.setPoolName("Artifex-MySQL");
            hikari.setLeakDetectionThreshold(Math.max(0L,
                plugin.getConfig().getLong("database.mysql.leak-detection-ms", 0L)));

            mysqlDataSource = new HikariDataSource(hikari);
            try (Connection test = mysqlDataSource.getConnection()) {
                if (!test.isValid((int) Math.min(Integer.MAX_VALUE, timeout / 1000L + 1))) {
                    throw new SQLException("MySQL connection is not valid");
                }
            }
            plugin.getLogger().info("Database connected: MySQL (" + host + ":" + port + "/" + database + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "MySQL connection error: " + e.getMessage(), e);
            if (mysqlDataSource != null) {
                mysqlDataSource.close();
                mysqlDataSource = null;
            }
            return false;
        }
    }

    /** Close SQLite connection or the MySQL pool. */
    public void disconnect() {
        if (mysqlDataSource != null) {
            mysqlDataSource.close();
            mysqlDataSource = null;
            plugin.getLogger().info("Database disconnected: MySQL");
        }
        if (sqliteConnection != null) {
            try {
                if (!sqliteConnection.isClosed()) sqliteConnection.close();
                plugin.getLogger().info("Database disconnected: SQLite");
            } catch (SQLException e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Database disconnect error: " + e.getMessage(), e);
            } finally {
                sqliteConnection = null;
            }
        }
    }

    public boolean isConnected() {
        try {
            if (mysqlDataSource != null) return !mysqlDataSource.isClosed() && mysqlDataSource.getHikariPoolMXBean() != null;
            return sqliteConnection != null && !sqliteConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    /** Returns "sqlite" or "mysql". */
    public String getType() {
        return type;
    }

    public boolean isMySql() {
        return "mysql".equals(type);
    }

    /**
     * Returns the SQLite connection or a leased MySQL pool connection.
     * Callers receiving a MySQL connection must close it; prefer async helpers below.
     *
     * For SQLite a thread-safe wrapper is returned: the single
     * connection is used by all threads, and access to it is
     * serialised (otherwise SQLite reported "database is locked" or
     * corrupted data during concurrent queries).
     */
    public Connection getConnection() {
        try {
            Connection connection = acquireConnection();
            if (connection == null) return null;
            if (isMySql()) return connection;
            return synchronizedConnection(connection);
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not acquire database connection: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Wrapper that serialises access to the shared SQLite connection.
     *
     * <p>Exceptions are propagated as they are: the reflective wrapper would otherwise
     * turn an SQLException into an InvocationTargetException, and the calling
     * code with {@code catch (SQLException)} would stop catching database errors.
     *
     * <p>{@code close()} is a no-op here: this is the <b>shared</b> connection,
     * and the usual {@code try (Connection c = db.getConnection())} pattern would
     * close it for the whole plugin. MySQL returns a pooled connection — that
     * one has to be closed.
     */
    private Connection synchronizedConnection(Connection delegate) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        // The shared connection lives until disconnect()
                        return null;
                    }
                    synchronized (sqliteLock) {
                        try {
                            return method.invoke(delegate, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof Exception exception) throw exception;
                            if (cause instanceof Error error) throw error;
                            throw e;
                        }
                    }
                });
    }

    private Connection acquireConnection() throws SQLException {
        if (isMySql()) {
            if (mysqlDataSource == null) throw new SQLException("MySQL is not connected");
            return mysqlDataSource.getConnection();
        }
        if (sqliteConnection == null || sqliteConnection.isClosed()) {
            throw new SQLException("SQLite is not connected");
        }
        return sqliteConnection;
    }

    private void releaseConnection(Connection connection) {
        if (isMySql() && connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    public boolean createTable(String tableName, String columns) {
        String safeTable = safeIdentifier(tableName);
        if (safeTable == null) return false;
        return execute("CREATE TABLE IF NOT EXISTS " + safeTable + " (" + columns + ")");
    }

    public boolean tableExists(String tableName) {
        if (!isMySql()) {
            synchronized (sqliteLock) {
                return tableExistsInternal(tableName);
            }
        }
        return tableExistsInternal(tableName);
    }

    private boolean tableExistsInternal(String tableName) {
        Connection connection = null;
        try {
            connection = acquireConnection();
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        } finally {
            releaseConnection(connection);
        }
    }

    public boolean execute(String sql, Object... params) {
        return executeUpdate(sql, params) >= 0;
    }

    /** Execute a write and return the affected-row count, or -1 on SQL error. */
    public int executeUpdate(String sql, Object... params) {
        if (!isMySql()) {
            synchronized (sqliteLock) {
                return executeUpdateInternal(sql, params);
            }
        }
        return executeUpdateInternal(sql, params);
    }

    private int executeUpdateInternal(String sql, Object... params) {
        Connection connection = null;
        try {
            connection = acquireConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setParams(statement, params);
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "SQL error: " + e.getMessage(), e);
            return -1;
        } finally {
            releaseConnection(connection);
        }
    }

    public boolean insert(String table, Map<String, Object> data) {
        if (data == null || data.isEmpty()) return false;
        String safeTable = safeIdentifier(table);
        if (safeTable == null) return false;
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String safeColumn = safeIdentifier(entry.getKey());
            if (safeColumn == null) return false;
            if (columns.length() > 0) { columns.append(", "); placeholders.append(", "); }
            columns.append(safeColumn);
            placeholders.append("?");
            values.add(entry.getValue());
        }
        return execute("INSERT INTO " + safeTable + " (" + columns + ") VALUES (" + placeholders + ")",
            values.toArray());
    }

    public boolean update(String table, Map<String, Object> data, String where, Object... whereParams) {
        if (data == null || data.isEmpty()) return false;
        String safeTable = safeIdentifier(table);
        if (safeTable == null) return false;
        StringBuilder sets = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String safeColumn = safeIdentifier(entry.getKey());
            if (safeColumn == null) return false;
            if (sets.length() > 0) sets.append(", ");
            sets.append(safeColumn).append(" = ?");
            values.add(entry.getValue());
        }
        Collections.addAll(values, whereParams);
        return execute("UPDATE " + safeTable + " SET " + sets + " WHERE " + where, values.toArray());
    }

    public boolean delete(String table, String where, Object... params) {
        String safeTable = safeIdentifier(table);
        if (safeTable == null) return false;
        return execute("DELETE FROM " + safeTable + " WHERE " + where, params);
    }

    public int queryInt(String sql, Object... params) {
        Object value = queryValue(sql, params);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public String queryString(String sql, Object... params) {
        Object value = queryValue(sql, params);
        return value == null ? null : String.valueOf(value);
    }

    public double queryDouble(String sql, Object... params) {
        Object value = queryValue(sql, params);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    public boolean queryBoolean(String sql, Object... params) {
        Object value = queryValue(sql, params);
        return value instanceof Boolean ? (Boolean) value : value instanceof Number && ((Number) value).intValue() != 0;
    }

    private Object queryValue(String sql, Object... params) {
        if (!isMySql()) {
            synchronized (sqliteLock) {
                return queryValueInternal(sql, params);
            }
        }
        return queryValueInternal(sql, params);
    }

    private Object queryValueInternal(String sql, Object... params) {
        Connection connection = null;
        try {
            connection = acquireConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setParams(statement, params);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getObject(1) : null;
                }
            }
        } catch (SQLException e) {
            return null;
        } finally {
            releaseConnection(connection);
        }
    }

    public List<Map<String, Object>> queryAll(String sql, Object... params) {
        if (!isMySql()) {
            synchronized (sqliteLock) {
                return queryAllInternal(sql, params);
            }
        }
        return queryAllInternal(sql, params);
    }

    private List<Map<String, Object>> queryAllInternal(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection connection = null;
        try {
            connection = acquireConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setParams(statement, params);
                try (ResultSet result = statement.executeQuery()) {
                    ResultSetMetaData meta = result.getMetaData();
                    int count = meta.getColumnCount();
                    while (result.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= count; i++) row.put(meta.getColumnName(i), result.getObject(i));
                        results.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Query error: " + e.getMessage(), e);
        } finally {
            releaseConnection(connection);
        }
        return results;
    }

    public Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> results = queryAll(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean increment(String table, String column, String where, Object... whereParams) {
        String safeTable = safeIdentifier(table);
        String safeColumn = safeIdentifier(column);
        if (safeTable == null || safeColumn == null) return false;
        return execute("UPDATE " + safeTable + " SET " + safeColumn + " = " + safeColumn + " + 1 WHERE " + where,
            whereParams);
    }

    public boolean add(String table, String column, double amount, String where, Object... whereParams) {
        String safeTable = safeIdentifier(table);
        String safeColumn = safeIdentifier(column);
        if (safeTable == null || safeColumn == null) return false;
        List<Object> params = new ArrayList<>();
        params.add(amount);
        Collections.addAll(params, whereParams);
        return execute("UPDATE " + safeTable + " SET " + safeColumn + " = " + safeColumn + " + ? WHERE " + where,
            params.toArray());
    }

    /** Run a write asynchronously. The callback must not touch Bukkit API directly. */
    public CompletableFuture<Boolean> executeAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> execute(sql, params), asyncExecutor);
    }

    /** Run a write asynchronously and return the affected-row count. */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params), asyncExecutor);
    }

    public CompletableFuture<Integer> queryIntAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryInt(sql, params), asyncExecutor);
    }

    public CompletableFuture<String> queryStringAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryString(sql, params), asyncExecutor);
    }

    public CompletableFuture<List<Map<String, Object>>> queryAllAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryAll(sql, params), asyncExecutor);
    }

    /** Stop the database worker threads during full plugin shutdown. */
    public void shutdownAsync() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncExecutor.shutdownNow();
        }
    }

    /** Convert a generic insert to the correct upsert syntax for the selected database. */
    public String upsertSql(String table, String columns, String values, String updateClause) {
        String safeTable = validateIdentifier(table);
        if (isMySql()) {
            return "INSERT INTO " + safeTable + " (" + columns + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE " + updateClause;
        }
        return "INSERT INTO " + safeTable + " (" + columns + ") VALUES (" + values + ") ON CONFLICT DO UPDATE SET " + updateClause;
    }

    /**
     * Checks that a table/column name is a valid SQL identifier.
     *
     * Table and column names are interpolated into the SQL as they are (PreparedStatement
     * parameters do not work for them), so an arbitrary string must not be allowed —
     * that would be a path to SQL injection through module code.
     *
     * @throws IllegalArgumentException if the name is invalid
     */
    private static String validateIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }

    /**
     * The same, but without throwing: returns null and writes to the log.
     * Used in public methods where an exception would break the
     * "return true/false" contract.
     */
    private String safeIdentifier(String identifier) {
        try {
            return validateIdentifier(identifier);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("SQL: " + e.getMessage());
            return null;
        }
    }

    private static final java.util.regex.Pattern IDENTIFIER_PATTERN =
            java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

    private void setParams(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
    }
}
