package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSnapshotTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());

    @Test
    void snapshotCarriesExplicitStatesAndValues() {
        ProfileSnapshot snapshot = new ProfileSnapshot(
                SYNTH.toString(), "READY", 123L,
                null, "UNAVAILABLE",
                7, 25L, 325L, "VERIFIED",
                "humans", "human_will", "VERIFIED",
                List.of("human_will"), "SYN", 2, "VERIFIED", "database");

        assertEquals("READY", snapshot.state());
        assertEquals(325L, snapshot.totalXp());
        assertEquals("UNAVAILABLE", snapshot.usernameState());
        assertEquals("database", snapshot.source());
    }

    @Test
    void unavailableStateUsesNoInventedValues() {
        ProfileSnapshot snapshot = ProfileSnapshot.unavailable(SYNTH);

        assertEquals("UNAVAILABLE", snapshot.state());
        assertNull(snapshot.username());
        assertNull(snapshot.level());
        assertNull(snapshot.totalXp());
        assertTrue(snapshot.activeCooldowns().isEmpty());
        assertTrue(snapshot.toJson().contains("\"state\":\"UNAVAILABLE\""));
        assertTrue(snapshot.toJson().contains("\"totalXp\":null"));
    }

    @Test
    void jsonEscapesQuotesAndBackslashes() {
        ProfileSnapshot snapshot = new ProfileSnapshot(
                SYNTH.toString(), "READY", 1L,
                "a\"b\\c", "VERIFIED", 1, 1L, 1L, "VERIFIED",
                "r", "s", "VERIFIED", List.of(), "g", 1, "VERIFIED", "database");

        String json = snapshot.toJson();

        assertTrue(json.contains("\"username\":\"a\\\"b\\\\c\""));
    }
}
