package me.dcplugin.artifex.api.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class IntegrationTokenVerifier {
    private final byte[] expected;

    public IntegrationTokenVerifier(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token required");
        this.expected = token.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return false;
        byte[] supplied = authorizationHeader.substring(7).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, supplied);
    }
}
