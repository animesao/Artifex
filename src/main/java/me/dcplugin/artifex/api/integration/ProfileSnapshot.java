package me.dcplugin.artifex.api.integration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProfileSnapshot(
        String uuid,
        String state,
        long observedAt,
        String username,
        String usernameState,
        Integer level,
        Long currentXp,
        Long totalXp,
        String progressionState,
        String raceId,
        String selectedSkillId,
        String raceState,
        List<String> activeCooldowns,
        String guildTag,
        Integer guildId,
        String guildState,
        String source) {

    public static final String READY = "READY";
    public static final String STALE = "STALE";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String VERIFIED = "VERIFIED";

    public static ProfileSnapshot unavailable(UUID uuid) {
        return new ProfileSnapshot(uuid.toString(), UNAVAILABLE, 0L,
                null, UNAVAILABLE, null, null, null, UNAVAILABLE,
                null, null, UNAVAILABLE, List.of(), null, null, UNAVAILABLE, "none");
    }

    public static ProfileSnapshot notFound(UUID uuid) {
        return new ProfileSnapshot(uuid.toString(), NOT_FOUND, 0L,
                null, UNAVAILABLE, null, null, null, UNAVAILABLE,
                null, null, UNAVAILABLE, List.of(), null, null, UNAVAILABLE, "none");
    }

    public String toJson() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("uuid", Json.string(uuid));
        fields.put("state", Json.string(state));
        fields.put("observedAt", Json.number(observedAt));
        fields.put("username", Json.string(username));
        fields.put("usernameState", Json.string(usernameState));
        fields.put("level", Json.number(level));
        fields.put("currentXp", Json.number(currentXp));
        fields.put("totalXp", Json.number(totalXp));
        fields.put("progressionState", Json.string(progressionState));
        fields.put("raceId", Json.string(raceId));
        fields.put("selectedSkillId", Json.string(selectedSkillId));
        fields.put("raceState", Json.string(raceState));
        fields.put("activeCooldowns", Json.strings(activeCooldowns));
        fields.put("guildTag", Json.string(guildTag));
        fields.put("guildId", Json.number(guildId));
        fields.put("guildState", Json.string(guildState));
        fields.put("source", Json.string(source));
        return Json.object(fields);
    }
}
