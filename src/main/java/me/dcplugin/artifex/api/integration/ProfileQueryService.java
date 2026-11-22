package me.dcplugin.artifex.api.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProfileQueryService {
    public record QueryResult(int status, String bodyJson) {
    }

    private static final String DISCORD_ID_PATTERN = "\\d{17,20}";

    private final IntegrationTokenVerifier verifier;
    private final LinkedIdentityStore links;
    private final ProfileDataSource source;
    private final boolean requireLink;

    public ProfileQueryService(IntegrationTokenVerifier verifier,
                               LinkedIdentityStore links,
                               ProfileDataSource source,
                               boolean requireLink) {
        this.verifier = verifier;
        this.links = links;
        this.source = source;
        this.requireLink = requireLink;
    }

    public CompletableFuture<QueryResult> query(String authorization, String discordId, String rawUuid) {
        if (!verifier.isValid(authorization)) {
            return completed(401, error("unauthorized"));
        }
        if (discordId == null || !discordId.matches(DISCORD_ID_PATTERN)) {
            return completed(400, error("invalid_discord_id"));
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(rawUuid);
        } catch (RuntimeException e) {
            return completed(400, error("invalid_uuid"));
        }
        if (requireLink && !links.isLinked(discordId, uuid)) {
            return completed(403, error("not_linked"));
        }
        return source.load(uuid)
                .thenApply(snapshot -> new QueryResult(200,
                        snapshot == null ? ProfileSnapshot.unavailable(uuid).toJson() : snapshot.toJson()))
                .exceptionally(failure -> new QueryResult(503, ProfileSnapshot.unavailable(uuid).toJson()));
    }

    private static CompletableFuture<QueryResult> completed(int status, String bodyJson) {
        return CompletableFuture.completedFuture(new QueryResult(status, bodyJson));
    }

    private static String error(String code) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("state", Json.string(ProfileSnapshot.UNAVAILABLE));
        fields.put("error", Json.string(code));
        return Json.object(fields);
    }
}
