package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ProfileQueryServiceTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());
    private static final String DISCORD = "100000000000000001";
    private static final String TOKEN = "Bearer test-service-token";

    private final ProfileSnapshot snapshot = new ProfileSnapshot(
            SYNTH.toString(), "READY", 123L,
            null, "UNAVAILABLE", 7, 25L, 325L, "VERIFIED",
            "humans", "human_will", "VERIFIED",
            List.of("human_will"), "SYN", 2, "VERIFIED", "database");

    private ProfileQueryService service(boolean requireLink, ProfileSnapshot data) {
        return new ProfileQueryService(
                new IntegrationTokenVerifier("test-service-token"),
                new LinkedIdentityStore(Map.of(DISCORD, SYNTH)),
                uuid -> CompletableFuture.completedFuture(data), requireLink);
    }

    @Test
    void rejectsMissingServiceToken() {
        var result = service(true, snapshot).query(null, DISCORD, SYNTH.toString()).join();

        assertEquals(401, result.status());
        assertTrue(result.bodyJson().contains("unauthorized"));
    }

    @Test
    void rejectsMalformedUuid() {
        var result = service(true, snapshot).query(TOKEN, DISCORD, "not-a-uuid").join();

        assertEquals(400, result.status());
        assertTrue(result.bodyJson().contains("invalid_uuid"));
    }

    @Test
    void rejectsMalformedDiscordId() {
        var result = service(true, snapshot).query(TOKEN, "not-a-snowflake", SYNTH.toString()).join();

        assertEquals(400, result.status());
        assertTrue(result.bodyJson().contains("invalid_discord_id"));
    }

    @Test
    void rejectsUnlinkedDiscordIdentity() {
        var result = service(true, snapshot).query(TOKEN, "100000000000000002", SYNTH.toString()).join();

        assertEquals(403, result.status());
        assertTrue(result.bodyJson().contains("not_linked"));
    }

    @Test
    void returnsDatabaseSnapshotForLinkedIdentity() {
        var result = service(true, snapshot).query(TOKEN, DISCORD, SYNTH.toString()).join();

        assertEquals(200, result.status());
        assertEquals(snapshot.toJson(), result.bodyJson());
    }

    @Test
    void propagatesExplicitUnavailableState() {
        var result = service(true, ProfileSnapshot.unavailable(SYNTH))
                .query(TOKEN, DISCORD, SYNTH.toString()).join();

        assertEquals(200, result.status());
        assertTrue(result.bodyJson().contains("\"state\":\"UNAVAILABLE\""));
    }

    @Test
    void dataFailureBecomesUnavailable() {
        ProfileQueryService failing = new ProfileQueryService(
                new IntegrationTokenVerifier("test-service-token"),
                new LinkedIdentityStore(Map.of(DISCORD, SYNTH)),
                uuid -> CompletableFuture.failedFuture(new IllegalStateException("db down")), true);

        var result = failing.query(TOKEN, DISCORD, SYNTH.toString()).join();

        assertEquals(503, result.status());
        assertTrue(result.bodyJson().contains("\"state\":\"UNAVAILABLE\""));
    }

    @Test
    void linkRequirementCanBeDisabledButDiscordIdStillValidated() {
        var permitted = service(false, snapshot).query(TOKEN, "100000000000000099", SYNTH.toString()).join();
        var rejected = service(false, snapshot).query(TOKEN, "bad", SYNTH.toString()).join();

        assertEquals(200, permitted.status());
        assertEquals(400, rejected.status());
    }
}
