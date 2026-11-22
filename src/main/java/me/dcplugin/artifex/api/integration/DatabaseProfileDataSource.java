package me.dcplugin.artifex.api.integration;

import me.dcplugin.artifex.api.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public final class DatabaseProfileDataSource implements ProfileDataSource {
    private static final String LEVELS_SQL =
            "SELECT level, xp, total_xp FROM artifex_levels WHERE uuid = ?";
    private static final String RACE_SQL =
            "SELECT race_id, selected_skill_id FROM artifex_race_skill_state WHERE uuid = ?";
    private static final String COOLDOWNS_SQL =
            "SELECT skill_id FROM artifex_race_skill_cooldowns WHERE uuid = ? AND cooldown_until > ?";
    private static final String GUILD_SQL =
            "SELECT g.id, g.tag FROM artifex_guild_members m "
                    + "JOIN artifex_guilds g ON g.id = m.guild_id WHERE m.player_uuid = ?";

    @FunctionalInterface
    public interface AsyncQuery {
        CompletableFuture<List<Map<String, Object>>> queryAll(String sql, Object... params);
    }

    private final BooleanSupplier ready;
    private final AsyncQuery queries;

    public DatabaseProfileDataSource(DatabaseManager database) {
        this(() -> database != null && database.isConnected(),
                (sql, params) -> database.queryAllAsync(sql, params));
    }

    public DatabaseProfileDataSource(AsyncQuery queries) {
        this(() -> true, queries);
    }

    private DatabaseProfileDataSource(BooleanSupplier ready, AsyncQuery queries) {
        this.ready = ready;
        this.queries = queries;
    }

    @Override
    public CompletableFuture<ProfileSnapshot> load(UUID uuid) {
        if (!ready.getAsBoolean()) {
            return CompletableFuture.completedFuture(ProfileSnapshot.unavailable(uuid));
        }
        String id = uuid.toString();
        long now = System.currentTimeMillis();
        CompletableFuture<List<Map<String, Object>>> levels = queries.queryAll(LEVELS_SQL, id);
        CompletableFuture<List<Map<String, Object>>> race = queries.queryAll(RACE_SQL, id);
        CompletableFuture<List<Map<String, Object>>> cooldowns = queries.queryAll(COOLDOWNS_SQL, id, now);
        CompletableFuture<List<Map<String, Object>>> guild = queries.queryAll(GUILD_SQL, id);

        return CompletableFuture.allOf(levels, race, cooldowns, guild).thenApply(ignored -> {
            List<Map<String, Object>> levelRows = levels.join();
            if (levelRows.isEmpty()) return ProfileSnapshot.notFound(uuid);
            return buildProfile(uuid, null, levelRows, race.join(), cooldowns.join(), guild.join());
        }).exceptionally(failure -> ProfileSnapshot.unavailable(uuid));
    }

    static ProfileSnapshot buildProfile(UUID uuid,
                                        String username,
                                        List<Map<String, Object>> levels,
                                        List<Map<String, Object>> race,
                                        List<Map<String, Object>> cooldowns,
                                        List<Map<String, Object>> guild) {
        Map<String, Object> levelRow = levels.isEmpty() ? Map.of() : levels.get(0);
        Map<String, Object> raceRow = race.isEmpty() ? Map.of() : race.get(0);
        Map<String, Object> guildRow = guild.isEmpty() ? Map.of() : guild.get(0);

        boolean hasLevels = levelRow.get("level") instanceof Number;
        boolean hasRace = raceRow.get("race_id") != null;
        boolean hasGuild = guildRow.get("tag") != null;

        List<String> activeCooldowns = new ArrayList<>();
        for (Map<String, Object> row : cooldowns) {
            Object skill = row.get("skill_id");
            if (skill != null) activeCooldowns.add(String.valueOf(skill));
        }

        return new ProfileSnapshot(
                uuid.toString(),
                hasLevels ? ProfileSnapshot.READY : ProfileSnapshot.UNAVAILABLE,
                System.currentTimeMillis(),
                username,
                username == null ? ProfileSnapshot.UNAVAILABLE : ProfileSnapshot.VERIFIED,
                integer(levelRow.get("level")),
                number(levelRow.get("xp")),
                number(levelRow.get("total_xp")),
                hasLevels ? ProfileSnapshot.VERIFIED : ProfileSnapshot.UNAVAILABLE,
                text(raceRow.get("race_id")),
                text(raceRow.get("selected_skill_id")),
                hasRace ? ProfileSnapshot.VERIFIED : ProfileSnapshot.UNAVAILABLE,
                activeCooldowns,
                text(guildRow.get("tag")),
                integer(guildRow.get("id")),
                hasGuild ? ProfileSnapshot.VERIFIED : ProfileSnapshot.UNAVAILABLE,
                "database");
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
