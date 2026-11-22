# Discord Read-Only HTTP API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose an authenticated, read-only, loopback-only Minecraft-side HTTP API returning verified Artifex profile fields for Discord, without giving the bot direct SQLite access.

**Architecture:** Build a small JDK `HttpServer` service in Artifex. A service token is read only from `ARTIFEX_INTEGRATION_TOKEN`. Requests must supply the token and, by default, a Discord user ID that is mapped to the requested Minecraft UUID through a local link store. All profile values are fetched on demand through `DatabaseManager`, not from unverified module caches. Responses carry explicit readiness states.

**Tech Stack:** Java 21, JDK `com.sun.net.httpserver`, JUnit 5, Maven. No new runtime dependencies.

## Review adjustments applied during execution

- `ProfileSnapshot` carries per-field states (`VERIFIED`/`UNAVAILABLE`) and nullable values. Empty strings
  are never used as substitutes for missing data, and offline caches are never reported as current.
- `ProfileDataSource.load` returns a snapshot whose own `state` is `READY`, `NOT_FOUND`, or `UNAVAILABLE`,
  instead of `Optional`; this removes the ambiguity between "database down" and "player has no row".
- `STALE` is enforced by `ProfileQueryService` using `observedAt` and `config.integration.http.max-age-ms`.
- Discord IDs must match `\\d{17,20}`; `links.yml` keys are validated the same way by `LinkConfigReader`.
- Host binding is forced to `127.0.0.1` regardless of configuration.
- A real SQLite integration test and a real loopback HTTP test were added, plus `LinkConfigReader`.

---

## File structure

- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileSnapshot.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileDataSource.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/LinkedIdentityStore.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/IntegrationTokenVerifier.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileQueryService.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/HttpIntegrationServer.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/DatabaseProfileDataSource.java`
- Modify: `src/main/java/me/dcplugin/artifex/bootstrap/PluginBootstrap.java`
- Modify: `src/main/java/me/dcplugin/artifex/Main.java`
- Create: `docs/DISCORD_READONLY_HTTP_API.md`

## Tasks

### Task 1: Token verifier and link store

**Files:**
- Create: `src/main/java/me/dcplugin/artifex/api/integration/IntegrationTokenVerifier.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/LinkedIdentityStore.java`
- Test: `src/test/java/me/dcplugin/artifex/api/integration/IntegrationSecurityTest.java`

- [ ] **Step 1: Write failing tests**

```java
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
```

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -q -Dtest=IntegrationSecurityTest test`
Expected: compilation failure because the classes do not exist.

- [ ] **Step 3: Implement minimal classes**

`IntegrationTokenVerifier.java`:

```java
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
```

`LinkedIdentityStore.java`:

```java
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
```

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -q -Dtest=IntegrationSecurityTest test`
Expected: PASS.

### Task 2: Profile DTO and data-source contract

**Files:**
- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileSnapshot.java`
- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileDataSource.java`

- [ ] **Step 1: Write the failing test**

```java
package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSnapshotTest {
    @Test
    void snapshotCarriesExplicitStatesAndValues() {
        ProfileSnapshot snapshot = new ProfileSnapshot(
                "READY", "VERIFIED", "SYNTH-PLAYER",
                7, 25, 325, "humans", "human_will",
                List.of("human_will"), "SYNTH", 2, "database");

        assertEquals("READY", snapshot.state());
        assertEquals(325, snapshot.totalXp());
        assertEquals("database", snapshot.source());
    }
}
```

- [ ] **Step 2: Run and verify RED**

Run: `mvn -q -Dtest=ProfileSnapshotTest test`
Expected: compilation failure because `ProfileSnapshot` does not exist.

- [ ] **Step 3: Implement DTO and contract**

```java
package me.dcplugin.artifex.api.integration;

import java.util.List;

public record ProfileSnapshot(
        String state,
        String usernameState,
        String username,
        int level,
        long currentXp,
        long totalXp,
        String raceId,
        String selectedSkillId,
        List<String> activeCooldowns,
        String guildTag,
        Integer guildId,
        String source) {
}
```

```java
package me.dcplugin.artifex.api.integration;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileDataSource {
    CompletableFuture<Optional<ProfileSnapshot>> load(UUID uuid);
}
```

- [ ] **Step 4: Run and verify GREEN**

Run: `mvn -q -Dtest=ProfileSnapshotTest test`
Expected: PASS.

### Task 3: Query service with authorization and states

**Files:**
- Create: `src/main/java/me/dcplugin/artifex/api/integration/ProfileQueryService.java`
- Test: `src/test/java/me/dcplugin/artifex/api/integration/ProfileQueryServiceTest.java`

- [ ] **Step 1: Write failing tests**

```java
package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ProfileQueryServiceTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());

    private final ProfileSnapshot snapshot = new ProfileSnapshot(
            "READY", "VERIFIED", "SYNTH-PLAYER", 7, 25, 325,
            "humans", "human_will", List.of("human_will"), "SYN", 2, "database");

    private ProfileQueryService service(boolean requireLink, Optional<ProfileSnapshot> data) {
        ProfileDataSource source = uuid -> CompletableFuture.completedFuture(data);
        return new ProfileQueryService(
                new IntegrationTokenVerifier("test-service-token"),
                new LinkedIdentityStore(Map.of("DISCORD-01", SYNTH)),
                source, requireLink);
    }

    @Test
    void rejectsMissingServiceToken() {
        assertEquals(401, service(true, Optional.of(snapshot))
                .query(null, "DISCORD-01", SYNTH.toString()).join().status());
    }

    @Test
    void rejectsMalformedUuid() {
        assertEquals(400, service(true, Optional.of(snapshot))
                .query("Bearer test-service-token", "DISCORD-01", "not-a-uuid").join().status());
    }

    @Test
    void rejectsUnlinkedDiscordIdentity() {
        assertEquals(403, service(true, Optional.of(snapshot))
                .query("Bearer test-service-token", "DISCORD-02", SYNTH.toString()).join().status());
    }

    @Test
    void returnsDatabaseSnapshotForLinkedIdentity() {
        assertEquals(snapshot, service(true, Optional.of(snapshot))
                .query("Bearer test-service-token", "DISCORD-01", SYNTH.toString()).join().body());
    }

    @Test
    void returnsNotFoundStateWhenNoRowExists() {
        var result = service(true, Optional.empty())
                .query("Bearer test-service-token", "DISCORD-01", SYNTH.toString()).join();
        assertEquals(200, result.status());
        assertEquals("NOT_FOUND", ((Map<?, ?>) result.body()).get("state"));
    }
}
```

- [ ] **Step 2: Run and verify RED**

Run: `mvn -q -Dtest=ProfileQueryServiceTest test`
Expected: compilation failure because `ProfileQueryService` does not exist.

- [ ] **Step 3: Implement query service**

```java
package me.dcplugin.artifex.api.integration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProfileQueryService {
    public record QueryResult(int status, Object body) {
    }

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

    public CompletableFuture<QueryResult> query(String authorization,
                                                String discordId,
                                                String rawUuid) {
        if (!verifier.isValid(authorization)) {
            return CompletableFuture.completedFuture(new QueryResult(401, Map.of("error", "unauthorized")));
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(rawUuid);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(new QueryResult(400, Map.of("error", "invalid_uuid")));
        }
        if (requireLink && !links.isLinked(discordId, uuid)) {
            return CompletableFuture.completedFuture(new QueryResult(403, Map.of("error", "not_linked")));
        }
        return source.load(uuid).thenApply(optional -> optional
                .map(profile -> new QueryResult(200, profile))
                .orElseGet(() -> new QueryResult(200, Map.of("state", "NOT_FOUND", "uuid", uuid.toString()))));
    }
}
```

- [ ] **Step 4: Run and verify GREEN**

Run: `mvn -q -Dtest=ProfileQueryServiceTest test`
Expected: PASS.

### Task 4: Loopback HTTP server

**Files:**
- Create: `src/main/java/me/dcplugin/artifex/api/integration/HttpIntegrationServer.java`
- Test: `src/test/java/me/dcplugin/artifex/api/integration/HttpIntegrationServerTest.java`

- [ ] **Step 1: Write a real loopback integration test**

```java
package me.dcplugin.artifex.api.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class HttpIntegrationServerTest {
    @Test
    void servesReadOnlyProfileOverLoopback() throws Exception {
        UUID synth = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());
        ProfileSnapshot profile = new ProfileSnapshot(
                "READY", "VERIFIED", "SYNTH-PLAYER", 7, 25, 325,
                "humans", "human_will", List.of(), "SYN", 2, "database");
        ProfileQueryService service = new ProfileQueryService(
                new IntegrationTokenVerifier("test-service-token"),
                new LinkedIdentityStore(Map.of("DISCORD-01", synth)),
                uuid -> CompletableFuture.completedFuture(Optional.of(profile)), true);

        HttpServer delegate = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpIntegrationServer server = new HttpIntegrationServer(delegate, service);
        server.start();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + delegate.getAddress().getPort()
                            + "/v1/player/" + synth))
                    .header("Authorization", "Bearer test-service-token")
                    .header("X-Discord-User-Id", "DISCORD-01")
                    .GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"state\":\"READY\""));
            assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(null));
        } finally {
            server.stop();
        }
    }
}
```

- [ ] **Step 2: Run and verify RED**

Run: `mvn -q -Dtest=HttpIntegrationServerTest test`
Expected: compilation failure because `HttpIntegrationServer` does not exist.

- [ ] **Step 3: Implement HTTP server**

```java
package me.dcplugin.artifex.api.integration;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class HttpIntegrationServer {
    private final HttpServer delegate;
    private final ProfileQueryService service;

    public HttpIntegrationServer(HttpServer delegate, ProfileQueryService service) {
        this.delegate = delegate;
        this.service = service;
    }

    public static HttpIntegrationServer create(String host, int port, ProfileQueryService service) throws IOException {
        return new HttpIntegrationServer(HttpServer.create(new InetSocketAddress(host, port), 0), service);
    }

    public void start() {
        delegate.createContext("/v1/player/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            String uuid = exchange.getRequestURI().getPath().substring("/v1/player/".length());
            service.query(
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("X-Discord-User-Id"),
                    uuid).whenComplete((result, error) -> {
                        if (error != null) {
                            respond(exchange, 503, "{\"state\":\"UNAVAILABLE\"}");
                        } else {
                            respond(exchange, result.status(), serialize(result.body()));
                        }
                    });
        });
        delegate.start();
    }

    public void stop() {
        delegate.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ignored) {
        } finally {
            exchange.close();
        }
    }

    private static String serialize(Object body) {
        if (body instanceof ProfileSnapshot p) {
            return "{\"state\":\"" + p.state() + "\",\"usernameState\":\"" + p.usernameState()
                    + "\",\"username\":\"" + escape(p.username()) + "\",\"level\":" + p.level()
                    + ",\"currentXp\":" + p.currentXp() + ",\"totalXp\":" + p.totalXp()
                    + ",\"raceId\":\"" + escape(p.raceId()) + "\",\"selectedSkillId\":\""
                    + escape(p.selectedSkillId()) + "\",\"guildTag\":\"" + escape(p.guildTag())
                    + "\",\"guildId\":" + p.guildId() + ",\"source\":\"" + p.source() + "\"}";
        }
        if (body instanceof java.util.Map<?, ?> map) {
            return "{\"state\":\"" + map.get("state") + "\",\"error\":\"" + map.get("error") + "\"}";
        }
        return "{\"state\":\"UNAVAILABLE\"}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
```

- [ ] **Step 4: Run and verify GREEN**

Run: `mvn -q -Dtest=HttpIntegrationServerTest test`
Expected: PASS.

### Task 5: Database-backed on-demand data source

**Files:**
- Create: `src/main/java/me/dcplugin/artifex/api/integration/DatabaseProfileDataSource.java`
- Test: `src/test/java/me/dcplugin/artifex/api/integration/DatabaseProfileDataSourceTest.java`

- [ ] **Step 1: Write failing mapping test**

```java
package me.dcplugin.artifex.api.integration;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseProfileDataSourceTest {
    @Test
    void mapsDatabaseRowsIntoProfileSnapshot() {
        List<Map<String, Object>> levels = List.of(Map.of("level", 7, "xp", 25L, "total_xp", 325L));
        List<Map<String, Object>> state = List.of(Map.of("race_id", "humans", "selected_skill_id", "human_will"));
        List<Map<String, Object>> cooldowns = List.of(Map.of("skill_id", "human_will"));
        List<Map<String, Object>> guild = List.of(Map.of("id", 2, "tag", "SYN"));

        ProfileSnapshot profile = DatabaseProfileDataSource.buildProfile(
                "SYNTH-PLAYER", levels, state, cooldowns, guild);

        assertEquals("READY", profile.state());
        assertEquals(7, profile.level());
        assertEquals(325, profile.totalXp());
        assertEquals("human_will", profile.selectedSkillId());
        assertEquals("SYN", profile.guildTag());
    }
}
```

- [ ] **Step 2: Run and verify RED**

Run: `mvn -q -Dtest=DatabaseProfileDataSourceTest test`
Expected: compilation failure because the class does not exist.

- [ ] **Step 3: Implement data source**

```java
package me.dcplugin.artifex.api.integration;

import me.dcplugin.artifex.api.database.DatabaseManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DatabaseProfileDataSource implements ProfileDataSource {
    private final DatabaseManager database;

    public DatabaseProfileDataSource(DatabaseManager database) {
        this.database = database;
    }

    @Override
    public CompletableFuture<Optional<ProfileSnapshot>> load(UUID uuid) {
        if (database == null || !database.isConnected()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        String id = uuid.toString();
        CompletableFuture<List<Map<String, Object>>> levels = database.queryAllAsync(
                "SELECT level, xp, total_xp FROM artifex_levels WHERE uuid = ?", id);
        CompletableFuture<List<Map<String, Object>>> state = database.queryAllAsync(
                "SELECT race_id, selected_skill_id FROM artifex_race_skill_state WHERE uuid = ?", id);
        CompletableFuture<List<Map<String, Object>>> cooldowns = database.queryAllAsync(
                "SELECT skill_id FROM artifex_race_skill_cooldowns WHERE uuid = ? AND cooldown_until > ?",
                id, System.currentTimeMillis());
        CompletableFuture<List<Map<String, Object>>> guild = database.queryAllAsync(
                "SELECT g.id, g.tag FROM artifex_guild_members m "
                        + "JOIN artifex_guilds g ON g.id = m.guild_id WHERE m.player_uuid = ?", id);

        return CompletableFuture.allOf(levels, state, cooldowns, guild).thenApply(ignored -> {
            if (levels.join().isEmpty()) return Optional.empty();
            return Optional.of(buildProfile(null, levels.join(), state.join(), cooldowns.join(), guild.join()));
        }).exceptionally(error -> Optional.empty());
    }

    static ProfileSnapshot buildProfile(String username,
                                        List<Map<String, Object>> levels,
                                        List<Map<String, Object>> state,
                                        List<Map<String, Object>> cooldowns,
                                        List<Map<String, Object>> guild) {
        Map<String, Object> levelRow = levels.get(0);
        Map<String, Object> raceRow = state.isEmpty() ? Map.of() : state.get(0);
        Map<String, Object> guildRow = guild.isEmpty() ? Map.of() : guild.get(0);
        List<String> activeCooldowns = new ArrayList<>();
        for (Map<String, Object> row : cooldowns) {
            Object skill = row.get("skill_id");
            if (skill != null) activeCooldowns.add(String.valueOf(skill));
        }
        return new ProfileSnapshot(
                "READY",
                username == null ? "UNAVAILABLE" : "OFFLINE_CACHE",
                username,
                ((Number) levelRow.get("level")).intValue(),
                ((Number) levelRow.get("xp")).longValue(),
                ((Number) levelRow.get("total_xp")).longValue(),
                text(raceRow.get("race_id")),
                text(raceRow.get("selected_skill_id")),
                activeCooldowns,
                text(guildRow.get("tag")),
                guildRow.get("id") == null ? null : ((Number) guildRow.get("id")).intValue(),
                "database");
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
```

- [ ] **Step 4: Run and verify GREEN**

Run: `mvn -q -Dtest=DatabaseProfileDataSourceTest test`
Expected: PASS.

### Task 6: Bootstrap wiring and lifecycle

**Files:**
- Modify: `src/main/java/me/dcplugin/artifex/bootstrap/PluginBootstrap.java`
- Modify: `src/main/java/me/dcplugin/artifex/Main.java`

- [ ] **Step 1: Add field and getter**

In `PluginBootstrap`, add:

```java
    private HttpIntegrationServer integrationServer;
```

and:

```java
    public HttpIntegrationServer getHttpIntegrationServer() { return integrationServer; }
```

In `Main`, add:

```java
    public HttpIntegrationServer getHttpIntegrationServer() { return bootstrap.getHttpIntegrationServer(); }
```

- [ ] **Step 2: Start the server at the end of enable()**

Add before `logStartupInfo()`:

```java
        startIntegrationServer();
```

Add these methods:

```java
    private void startIntegrationServer() {
        if (!plugin.getConfig().getBoolean("integration.http.enabled", false)) return;
        String token = System.getenv("ARTIFEX_INTEGRATION_TOKEN");
        if (token == null || token.isBlank()) {
            plugin.getLogger().warning("[Integration] HTTP API disabled: ARTIFEX_INTEGRATION_TOKEN is not set");
            return;
        }
        String host = plugin.getConfig().getString("integration.http.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("integration.http.port", 25570);
        boolean requireLink = plugin.getConfig().getBoolean("integration.http.require-player-link", true);
        Map<String, UUID> links = readLinks();
        ProfileQueryService service = new ProfileQueryService(
                new IntegrationTokenVerifier(token),
                new LinkedIdentityStore(links),
                new DatabaseProfileDataSource(databaseManager),
                requireLink);
        try {
            integrationServer = HttpIntegrationServer.create(host, port, service);
            integrationServer.start();
            plugin.getLogger().info("[Integration] Read-only HTTP API started");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[Integration] Could not start HTTP API: " + e.getMessage(), e);
        }
    }

    private Map<String, UUID> readLinks() {
        Map<String, UUID> result = new HashMap<>();
        org.bukkit.configuration.file.YamlConfiguration links = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(new java.io.File(plugin.getDataFolder(), "links.yml"));
        for (String key : links.getKeys(false)) {
            try {
                result.put(key, UUID.fromString(links.getString(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }
```

- [ ] **Step 3: Stop the server in disable()**

Add after `Artifex disabling...` log:

```java
        if (integrationServer != null) {
            integrationServer.stop();
            integrationServer = null;
        }
```

- [ ] **Step 4: Add config defaults**

Add to `src/main/resources/config.yml`:

```yaml
integration:
  http:
    enabled: false
    host: 127.0.0.1
    port: 25570
    require-player-link: true
```

- [ ] **Step 5: Run full tests**

Run: `mvn -q test`
Expected: PASS.

### Task 7: Documentation and final verification

**Files:**
- Create: `docs/DISCORD_READONLY_HTTP_API.md`

- [ ] **Step 1: Document the contract**

Include: endpoint `GET /v1/player/{uuid}`, headers `Authorization: Bearer ...` and `X-Discord-User-Id`, token only from `ARTIFEX_INTEGRATION_TOKEN`, `links.yml` format, states `READY`, `NOT_FOUND`, `UNAVAILABLE`, `403 not_linked`, `400 invalid_uuid`, `401 unauthorized`, `405 method_not_allowed`, and that all profile values come from on-demand DB reads.

- [ ] **Step 2: Build**

Run: `mvn -q -DskipTests package`
Expected: JAR builds successfully.

- [ ] **Step 3: Disposable validation only**

Copy the built JAR and config into the disposable test server, set a synthetic token in the environment, add only synthetic Discord-to-Minecraft links, start through the isolated launcher, and verify:
- unauthenticated request returns 401;
- wrong Discord ID returns 403;
- linked synthetic UUID returns persisted synthetic values;
- POST returns 405;
- no direct SQLite access is granted to the Discord bot.

Do not deploy to production.
