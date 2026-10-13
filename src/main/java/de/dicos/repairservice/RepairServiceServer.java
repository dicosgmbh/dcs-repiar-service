package de.dicos.repairservice;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public final class RepairServiceServer {

    private static final Logger log = System.getLogger(RepairServiceServer.class.getName());
    private static final String INDEX_PAGE = loadIndexPage();

    private RepairServiceServer() {
    }

    public static void main(String[] args)
            throws IOException {
        Optional<String> profile = Optional.ofNullable(System.getProperty("app.profile", System.getenv("APP_PROFILE")));
        Config config = Config.load(profile);
        HttpServer server = start(config);
        log.log(Level.INFO, "Started on port {0} (profile: {1})", String.valueOf(server.getAddress().getPort()), profile.orElse("default"));
        log.log(Level.INFO, "Index page: http://localhost:{0}{1}/index.html", String.valueOf(server.getAddress().getPort()), config.contextPath());
    }

    public static HttpServer start(Config config)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        Authenticator auth = basicAuth(config);
        String base = config.contextPath();

        register(server, base + "/rest/ok", null, getOnly(base + "/rest/ok",
                ex -> send(ex, 200, "text/plain", "OK")));
        register(server, base + "/actuator/health", auth, getOnly(base + "/actuator/health",
                ex -> send(ex, 200, "application/json", "{\"status\":\"UP\"}")));
        register(server, base + "/rest/repair-request/estimate", null, getOnly(base + "/rest/repair-request/estimate",
                ex -> RepairRequestHandler.handleEstimation(ex)));
        register(server, base + "/rest/repair-request/create", null, getOnly(base + "/rest/repair-request/create",
                new RepairRequestHandler()));
        if (config.isTest()) {
            register(server, "/repair-request", null,
                    getOnly("/repair-request", ex -> handleTestRepairRequest(ex)));
        }
        register(server, base + "/index.html", null, getOnly(base + "/index.html",
                ex -> send(ex, 200, "text/html", INDEX_PAGE)));
        register(server, base + "/", auth, ex -> send(ex, 404, "text/plain", "Not Found"));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        return server;
    }

    private static void register(HttpServer server, String path, Authenticator auth, HttpHandler handler) {
        HttpContext ctx = server.createContext(path, handler);
        ctx.getFilters().add(accessLog());
        Optional.ofNullable(auth).ifPresent(ctx::setAuthenticator);
    }

    // HttpServer matches contexts by prefix, so exact paths have to be enforced
    // here
    private static HttpHandler getOnly(String path, HttpHandler handler) {
        return ex -> {
            if (!ex.getRequestURI().getPath().equals(path)) {
                send(ex, 404, "text/plain", "Not Found");
            } else if (!"GET".equals(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Allow", "GET");
                send(ex, 405, "text/plain", "Method Not Allowed");
            } else {
                handler.handle(ex);
            }
        };
    }

    private static void handleTestRepairRequest(HttpExchange ex) {
        try {
            send(ex, 200, "text/plain", """
							{
							  "customerNumber": 100452,
							  "carModel": "VW Golf VII",
							  "numberPlate": "B-XY 1234",
							  "preferredDate": "2026-10-05",
							  "repairOperations": [
							    {
							      "description": "Bremsbeläge vorne wechseln",
							      "priceEstimation": 189.90
							    },
							    {
							      "description": "Ölwechsel inkl. Filter",
							      "priceEstimation": 79.50
							    }
							  ]
							}
														""");
        } catch (IOException x) {
            // TODO Auto-generated catch block
            log.log(Level.ERROR, "IOException", x);
        }
    }

    private static Authenticator basicAuth(Config config) {
        return new BasicAuthenticator("repair-service") {
            @Override
            public boolean checkCredentials(String user, String password) {
                return equalsConstantTime(user, config.user()) & equalsConstantTime(password, config.password());
            }
        };
    }

    private static boolean equalsConstantTime(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static Filter accessLog() {
        return Filter.afterHandler("access log", ex -> log.log(Level.INFO, "{0} {1} {2} {3}",
                ex.getRemoteAddress().getAddress().getHostAddress(), ex.getRequestMethod(), ex.getRequestURI(),
                ex.getResponseCode()));
    }

    private static String loadIndexPage() {
        try (InputStream in = RepairServiceServer.class.getClassLoader().getResourceAsStream("webapp/index.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void send(HttpExchange ex, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().put("Content-Type", List.of(contentType + ";charset=UTF-8"));
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
