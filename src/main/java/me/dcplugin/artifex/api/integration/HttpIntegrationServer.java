package me.dcplugin.artifex.api.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpIntegrationServer {
    private static final String PREFIX = "/v1/player/";

    private final HttpServer delegate;
    private final ProfileQueryService service;
    private final ExecutorService executor;

    public HttpIntegrationServer(HttpServer delegate, ProfileQueryService service) {
        this(delegate, service, null);
    }

    private HttpIntegrationServer(HttpServer delegate, ProfileQueryService service, ExecutorService executor) {
        this.delegate = delegate;
        this.service = service;
        this.executor = executor;
    }

    public static HttpIntegrationServer create(String host, int port, ProfileQueryService service) throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "Artifex-Integration-Http");
            thread.setDaemon(true);
            return thread;
        });
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(executor);
        return new HttpIntegrationServer(server, service, executor);
    }

    public void start() {
        delegate.createContext(PREFIX, this::handle);
        delegate.start();
    }

    public void stop() {
        delegate.stop(0);
        if (executor != null) executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, error("method_not_allowed"));
                return;
            }
            String rawUuid = exchange.getRequestURI().getPath().substring(PREFIX.length());
            service.query(
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("X-Discord-User-Id"),
                    rawUuid).whenComplete((result, failure) -> {
                        if (failure != null) respond(exchange, 503, unavailable());
                        else respond(exchange, result.status(), result.bodyJson());
                    });
        } catch (RuntimeException e) {
            respond(exchange, 400, error("bad_request"));
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ignored) {
        } finally {
            exchange.close();
        }
    }

    private static String error(String code) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("state", Json.string(ProfileSnapshot.UNAVAILABLE));
        fields.put("error", Json.string(code));
        return Json.object(fields);
    }

    private static String unavailable() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("state", Json.string(ProfileSnapshot.UNAVAILABLE));
        return Json.object(fields);
    }
}
