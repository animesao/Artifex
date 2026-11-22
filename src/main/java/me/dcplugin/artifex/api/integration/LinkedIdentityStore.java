package me.dcplugin.artifex.api.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LinkedIdentityStore {
    private final Map<String, UUID> links;

    public LinkedIdentityStore(Map<String, UUID> links) {
        this.links = new HashMap<>(links);
    }

    public boolean isLinked(String discordId, UUID uuid) {
        if (discordId == null || discordId.isBlank() || uuid == null) return false;
        UUID linked = links.get(discordId);
        return linked != null && linked.equals(uuid);
    }
}
