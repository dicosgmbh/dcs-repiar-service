package de.dicos.repairservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RepairRequestHandler
        implements
        HttpHandler {

    private static final Logger log = System.getLogger(RepairRequestHandler.class.getName());
    private static final URI ADMINISTRATION_URI = URI.create("http://localhost:8093/repair-request");

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public void handle(HttpExchange ex)
            throws IOException {
        log.log(Level.INFO, "sending new repair request to administration");

        String body = toJson(parseQuery(ex.getRequestURI().getQuery()));
        HttpRequest request = HttpRequest.newBuilder(ADMINISTRATION_URI)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(1L))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
            ex.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            log.log(Level.WARNING, "cannot send request to administration");
            ex.sendResponseHeaders(500, -1);
        }
    }

    // customerNumber, carModel, numberPlate, preferredDate, repairOperations
    // (repeated, "description:price")
    static String toJson(Map<String, List<String>> params) {
        String operations = params.getOrDefault("repairOperations", List.of()).stream()
                .map(op -> op.split(":", 2))
                .map(parts -> "{\"description\":\"" + escape(parts[0]) + "\",\"priceEstimation\":" + parts[1] + "}")
                .collect(Collectors.joining(",", "[", "]"));

        return "{\"customerNumber\":" + first(params, "customerNumber") + ",\"carModel\":\"" + escape(first(params, "carModel")) + "\""
                + ",\"numberPlate\":\"" + escape(first(params, "numberPlate")) + "\"" + ",\"preferredDate\":\""
                + escape(first(params, "preferredDate")) + "\"" + ",\"repairOperations\":" + operations + "}";
    }

    private static String first(Map<String, List<String>> params, String name) {
        List<String> values = params.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = decode(separator < 0 ? pair : pair.substring(0, separator));
            String value = separator < 0 ? "" : decode(pair.substring(separator + 1));
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
