package me.dcplugin.artifex.api.database;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseManagerTest {

    private Path tempDir;
    private DatabaseManager database;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("artifex-db-test");
        Main plugin = mock(Main.class);
        FileConfiguration config = mock(FileConfiguration.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ArtifexDatabaseTest"));
        when(config.getString(eq("database.type"), anyString())).thenReturn("sqlite");
        database = new DatabaseManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.disconnect();
        if (tempDir != null) {
            try (var paths = Files.walk(tempDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                });
            }
        }
    }

    @Test
    void connectsToSqliteAndReportsReady() {
        assertTrue(database.connect());
        assertTrue(database.isConnected());
        assertEquals("sqlite", database.getType());
        assertFalse(database.isMySql());
        assertNotNull(database.getConnection());
        assertTrue(Files.exists(tempDir.resolve("data.db")));
    }

    @Test
    void disconnectMakesDatabaseUnavailable() {
        assertTrue(database.connect());
        database.disconnect();
        assertFalse(database.isConnected());
        assertNull(database.getConnection());
        assertEquals(0, database.queryInt("SELECT 1"));
    }

    @Test
    void createsSchemaAndPersistsRowsWithParameterizedQueries() {
        assertTrue(database.connect());
        assertTrue(database.createTable("core_profile", "uuid TEXT PRIMARY KEY, level INTEGER"));
        assertTrue(database.tableExists("core_profile"));

        Map<String, Object> row = Map.of("uuid", "synthetic-uuid", "level", 7);
        assertTrue(database.insert("core_profile", row));
        assertEquals(7, database.queryInt("SELECT level FROM core_profile WHERE uuid = ?", "synthetic-uuid"));
        assertEquals("synthetic-uuid", database.queryString("SELECT uuid FROM core_profile WHERE level = ?", 7));
    }

    @Test
    void repeatedSchemaInitializationIsIdempotent() {
        assertTrue(database.connect());
        assertTrue(database.createTable("core_profile", "uuid TEXT PRIMARY KEY, level INTEGER"));
        assertTrue(database.insert("core_profile", Map.of("uuid", "synthetic-uuid", "level", 1)));

        assertTrue(database.createTable("core_profile", "uuid TEXT PRIMARY KEY, level INTEGER"));
        assertTrue(database.tableExists("core_profile"));
        assertEquals(1, database.queryInt("SELECT COUNT(*) FROM core_profile"));
        assertEquals(1, database.queryInt("SELECT level FROM core_profile WHERE uuid = ?", "synthetic-uuid"));
    }

    @Test
    void parameterizedQueriesDoNotInterpretInputAsSql() {
        assertTrue(database.connect());
        assertTrue(database.createTable("core_profile", "uuid TEXT PRIMARY KEY"));
        String payload = "synthetic' OR '1'='1";
        assertTrue(database.insert("core_profile", Map.of("uuid", payload)));
        assertEquals(1, database.queryInt("SELECT COUNT(*) FROM core_profile WHERE uuid = ?", payload));
        assertEquals(0, database.queryInt("SELECT COUNT(*) FROM core_profile WHERE uuid = ?", "other"));
    }

    @Test
    void asyncQueryReturnsPersistedValue() throws Exception {
        assertTrue(database.connect());
        assertTrue(database.createTable("core_profile", "uuid TEXT PRIMARY KEY, level INTEGER"));
        assertTrue(database.insert("core_profile", Map.of("uuid", "synthetic-uuid", "level", 25)));
        assertEquals(25, database.queryIntAsync("SELECT level FROM core_profile WHERE uuid = ?", "synthetic-uuid").get());
    }

    @Test
    void invalidIdentifierIsRejected() {
        assertTrue(database.connect());
        assertFalse(database.createTable("bad; DROP TABLE core_profile", "uuid TEXT"));
        assertFalse(database.tableExists("bad; DROP TABLE core_profile"));
    }

    @Test
    void shutdownCompletesPendingAsyncWaitersAndDisconnects() throws Exception {
        assertTrue(database.connect());
        CompletableFuture<Integer> pending = new CompletableFuture<>();
        pendingAsyncOperations().add(pending);

        database.shutdownAsync();

        assertTrue(pending.isCompletedExceptionally());
        assertTrue(pending.handle((value, error) -> error instanceof CancellationException).join());
        assertFalse(database.isConnected());
        assertEquals(0, pendingAsyncOperations().size());
    }

    @SuppressWarnings("unchecked")
    private Set<CompletableFuture<?>> pendingAsyncOperations() throws Exception {
        Field field = DatabaseManager.class.getDeclaredField("pendingAsyncOperations");
        field.setAccessible(true);
        return (Set<CompletableFuture<?>>) field.get(database);
    }
}
