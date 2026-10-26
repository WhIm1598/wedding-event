package com.weddingevent.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Local stand-in for Zalo's OAuth v4 token endpoint and ZNS API. Records every request; responses can be swapped per
 * test with {@link #onToken} / {@link #onZns}.
 */
public final class ZaloStub {

    public record Request(String path, Map<String, List<String>> headers, String body) {
        public String header(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(e -> e.getValue().getFirst()).findFirst().orElse(null);
        }
    }

    public static final String TOKEN_OK = """
            {"access_token":"stub-access","refresh_token":"stub-refresh-next","expires_in":"90000"}""";
    public static final String ZNS_OK = """
            {"error":0,"message":"Success","data":{"msg_id":"stub-msg","sent_time":"1626926349402"}}""";

    private final HttpServer server;
    public final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<>();
    private volatile Function<Request, String> token = r -> TOKEN_OK;
    private volatile Function<Request, String> zns = r -> ZNS_OK;

    private ZaloStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/oauth", ex -> answer(ex, token));
        server.createContext("/zns", ex -> answer(ex, zns));
        server.start();
    }

    public static ZaloStub start() {
        return new ZaloStub();
    }

    public String oauthUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/oauth";
    }

    public String znsUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/zns";
    }

    public void onToken(Function<Request, String> handler) {
        token = handler;
    }

    public void onZns(Function<Request, String> handler) {
        zns = handler;
    }

    public void reset() {
        requests.clear();
        token = r -> TOKEN_OK;
        zns = r -> ZNS_OK;
    }

    public List<Request> requestsTo(String path) {
        return requests.stream().filter(r -> r.path().equals(path)).toList();
    }

    private void answer(HttpExchange ex, Function<Request, String> handler) throws IOException {
        Request req = new Request(ex.getRequestURI().getPath(), Map.copyOf(ex.getRequestHeaders()),
                new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        requests.add(req);
        byte[] body = handler.apply(req).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }
}
