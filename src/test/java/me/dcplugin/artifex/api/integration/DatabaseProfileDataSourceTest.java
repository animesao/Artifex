package me.dcplugin.artifex.api.integration;

import me.dcplugin.artifex.api.database.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseProfileDataSourceTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());

    private final List<Map<String, Object>> levels = List.of(
            Map.of("level", 7, "xp", 25L, "total_xp", 325L));
    private final List<Map<String, Object>> state = List.of(
            Map.of("race_id", "humans", "selected_skill_id", "human_will"));
    private final List<Map<String, Object>> cooldowns = List.of(Map.of("skill_id", "human_will"));
    private final List<Map<String, Object>> guild = List.of(Map.of("id", 2, "tag", "SYN"));

    @Test
    void mapsDatabaseRowsIntoProfileSnapshot() {
        ProfileSnapshot profile = DatabaseProfileDataSource.buildProfile(
                SYNTH, null, levels, state, cooldowns, guild);

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
    void missingRowsUseExplicitUnavailableStates() {
        ProfileSnapshot profile = DatabaseProfileDataSource.buildProfile(
                SYNTH, null, levels, List.of(), List.of(), List.of());

        assertEquals(ProfileSnapshot.READY, profile.state());
        assertEquals(ProfileSnapshot.UNAVAILABLE, profile.raceState());
        assertEquals(ProfileSnapshot.UNAVAILABLE, profile.guildState());
        assertNull(profile.raceId());
        assertNull(profile.guildTag());
        assertTrue(profile.activeCooldowns().isEmpty());
    }

    @Test
    void disconnectedDatabaseReturnsUnavailable() {
        DatabaseProfileDataSource source = new DatabaseProfileDataSource((DatabaseManager) null);

        assertEquals(ProfileSnapshot.UNAVAILABLE, source.load(SYNTH).join().state());
    }

    @Test
    void queriesAreParameterized() {
        List<String> sqls = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        DatabaseProfileDataSource.AsyncQuery capture = (sql, values) -> {
            sqls.add(sql);
            params.add(values);
            return CompletableFuture.completedFuture(List.of());
        };

        new DatabaseProfileDataSource(capture).load(SYNTH).join();

        assertEquals(4, sqls.size());
        assertTrue(sqls.stream().allMatch(sql -> sql.contains("?")));
        assertTrue(sqls.stream().noneMatch(sql -> sql.contains(SYNTH.toString())));
        assertEquals(SYNTH.toString(), params.get(0)[0]);
        assertEquals(SYNTH.toString(), params.get(1)[0]);
        assertEquals(SYNTH.toString(), params.get(2)[0]);
        assertEquals(SYNTH.toString(), params.get(3)[0]);
    }

    @Test
    void absentLevelRowReturnsNotFound() {
        DatabaseProfileDataSource source = new DatabaseProfileDataSource(
                (sql, values) -> CompletableFuture.completedFuture(List.of()));

        assertEquals(ProfileSnapshot.NOT_FOUND, source.load(SYNTH).join().state());
    }

    @Test
    void queryFailureReturnsUnavailable() {
        DatabaseProfileDataSource source = new DatabaseProfileDataSource(
                (sql, values) -> CompletableFuture.failedFuture(new IllegalStateException("db down")));

        assertEquals(ProfileSnapshot.UNAVAILABLE, source.load(SYNTH).join().state());
    }
}
