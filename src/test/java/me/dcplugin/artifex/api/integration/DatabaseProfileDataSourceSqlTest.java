package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseProfileDataSourceSqlTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());
    private static final UUID EMPTY = UUID.nameUUIDFromBytes("SYNTH-EMPTY-01".getBytes());

    private Path dbFile;
    private Connection connection;
    private DatabaseProfileDataSource source;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("artifex-profile-", ".db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE artifex_levels (uuid VARCHAR(36) PRIMARY KEY, level INTEGER NOT NULL, "
                    + "xp BIGINT NOT NULL, total_xp BIGINT NOT NULL, updated_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE artifex_race_skill_state (uuid VARCHAR(36) PRIMARY KEY, "
                    + "race_id VARCHAR(32) NOT NULL, selected_skill_id VARCHAR(64) NOT NULL, updated_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE artifex_race_skill_cooldowns (uuid VARCHAR(36) NOT NULL, "
                    + "skill_id VARCHAR(64) NOT NULL, cooldown_until BIGINT NOT NULL, PRIMARY KEY (uuid, skill_id))");
            statement.execute("CREATE TABLE artifex_guilds (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name VARCHAR(48) NOT NULL, tag VARCHAR(16) NOT NULL, leader_uuid VARCHAR(36) NOT NULL, "
                    + "created_at BIGINT NOT NULL, description TEXT NOT NULL DEFAULT '')");
            statement.execute("CREATE TABLE artifex_guild_members (guild_id INTEGER NOT NULL, "
                    + "player_uuid TEXT NOT NULL, rank TEXT NOT NULL, joined_at INTEGER NOT NULL, "
                    + "PRIMARY KEY(guild_id, player_uuid))");
        }
        insert("INSERT INTO artifex_levels VALUES (?,?,?,?,?)", SYNTH.toString(), 7, 25L, 325L, 0L);
        insert("INSERT INTO artifex_race_skill_state VALUES (?,?,?,?)", SYNTH.toString(), "humans", "human_will", 0L);
        insert("INSERT INTO artifex_race_skill_cooldowns VALUES (?,?,?)",
                SYNTH.toString(), "human_will", System.currentTimeMillis() + 600_000L);
        insert("INSERT INTO artifex_guilds (id, name, tag, leader_uuid, created_at) VALUES (?,?,?,?,?)",
                2, "SYNTH-GUILD", "SYN", SYNTH.toString(), 0L);
        insert("INSERT INTO artifex_guild_members VALUES (?,?,?,?)", 2, SYNTH.toString(), "member", 0L);

        source = new DatabaseProfileDataSource((sql, params) -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
                try (ResultSet result = statement.executeQuery()) {
                    ResultSetMetaData meta = result.getMetaData();
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (result.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.put(meta.getColumnName(i), result.getObject(i));
                        }
                        rows.add(row);
                    }
                    return CompletableFuture.completedFuture(rows);
                }
            } catch (SQLException e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
        Files.deleteIfExists(dbFile);
    }

    @Test
    void readsPersistedSyntheticProfile() {
        ProfileSnapshot profile = source.load(SYNTH).join();

        assertEquals(ProfileSnapshot.READY, profile.state());
        assertEquals(7, profile.level());
        assertEquals(25L, profile.currentXp());
        assertEquals(325L, profile.totalXp());
        assertEquals("humans", profile.raceId());
        assertEquals("human_will", profile.selectedSkillId());
        assertEquals(List.of("human_will"), profile.activeCooldowns());
        assertEquals("SYN", profile.guildTag());
        assertEquals(2, profile.guildId());
        assertEquals("database", profile.source());
    }

    @Test
    void absentUuidReturnsNotFound() {
        assertEquals(ProfileSnapshot.NOT_FOUND, source.load(EMPTY).join().state());
    }

    private void insert(String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
            statement.executeUpdate();
        }
    }
}
