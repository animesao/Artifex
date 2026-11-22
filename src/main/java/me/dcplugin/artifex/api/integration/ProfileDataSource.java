package me.dcplugin.artifex.api.integration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ProfileDataSource {
    CompletableFuture<ProfileSnapshot> load(UUID uuid);
}
