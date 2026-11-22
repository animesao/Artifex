package me.dcplugin.artifex.api.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class HttpIntegrationServerTest {
    private static final UUID SYNTH = UUID.nameUUIDFromBytes("SYNTH-OP-01".getBytes());
    private static final String DISCORD = "100000000000000001";
    private static final String TOKEN = "Bearer test-service-token";

    private HttpServer delegate;
    private HttpIntegrationServer server;
    private int port;

    @BeforeEach
    void start() throws Exception {
        ProfileSnapshot profile = new ProfileSnapshot(
                SYNTH.toString(), "READY", 123L,
                null, "UNAVAILABLE", 7, 25L, 325L, "VERIFIED",
                "humans", "human_will", "VERIFIED", List.of(), "SYN", 2, "VERIFIED", "database");
        ProfileQueryService service = new ProfileQueryService(
                new IntegrationTokenVerifier("test-service-token"),
                new LinkedIdentityStore(Map.of(DISCORD, SYNTH)),
                uuid -> CompletableFuture.completedFuture(profile), true);

        delegate = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server = new HttpIntegrationServer(delegate, service);
        server.start();
        port = delegate.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void servesReadOnlyProfileOverLoopback() throws Exception {
        HttpResponse<String> response = send(TOKEN, DISCORD, HttpRequest.newBuilder().GET());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"state\":\"READY\""));
        assertTrue(response.body().contains("\"totalXp\":325"));
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(null));
    }

    @Test
    void rejectsNonGetMethods() throws Exception {
        HttpResponse<String> response = send(TOKEN, DISCORD,
                HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.noBody()));

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("method_not_allowed"));
    }

    @Test
    void rejectsMissingToken() throws Exception {
        HttpResponse<String> response = send(null, DISCORD, HttpRequest.newBuilder().GET());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("unauthorized"));
    }

    @Test
    void bindsToLoopbackOnly() {
        assertEquals("127.0.0.1", delegate.getAddress().getAddress().getHostAddress());
    }

    private HttpResponse<String> send(String token, String discordId, HttpRequest.Builder builder)
            throws Exception {
        HttpRequest.Builder request = builder.uri(URI.create(
                "http://127.0.0.1:" + port + "/v1/player/" + SYNTH));
        if (token != null) request.header("Authorization", token);
        request.header("X-Discord-User-Id", discordId);
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
