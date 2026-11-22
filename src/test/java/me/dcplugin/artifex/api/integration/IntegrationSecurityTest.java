package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationSecurityTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());

    @Test
    void acceptsOnlyExactBearerToken() {
        IntegrationTokenVerifier verifier = new IntegrationTokenVerifier("test-service-token");
        assertTrue(verifier.isValid("Bearer test-service-token"));
        assertFalse(verifier.isValid("Bearer test-service-tokenx"));
        assertFalse(verifier.isValid("Basic test-service-token"));
        assertFalse(verifier.isValid(null));
    }

    @Test
    void linkStoreRequiresMatchingDiscordAndMinecraftIdentity() {
        LinkedIdentityStore store = new LinkedIdentityStore(Map.of("DISCORD-01", SYNTH));
        assertTrue(store.isLinked("DISCORD-01", SYNTH));
        assertFalse(store.isLinked("DISCORD-02", SYNTH));
        assertFalse(store.isLinked("DISCORD-01", UUID.randomUUID()));
        assertFalse(store.isLinked("", SYNTH));
    }
}
