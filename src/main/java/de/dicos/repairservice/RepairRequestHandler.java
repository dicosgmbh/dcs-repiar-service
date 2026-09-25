package de.dicos.repairservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RepairRequestHandler
        implements
        HttpHandler {

    private static final Logger log = System.getLogger(RepairRequestHandler.class.getName());
    private static final URI ADMINISTRATION_URI = URI.create("http://localhost:8081/repair-request");
    private static final Map<String, Map<String, Double>> PRICES = load();

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
            ex.sendResponseHeaders(201, -1);
        } catch (Exception e) {
            log.log(Level.WARNING, "cannot send request to administration");
            ex.sendResponseHeaders(500, -1);
        }
    }

    public static void handleEstimation(HttpExchange ex)
            throws IOException {
        Map<String, List<String>> params = RepairRequestHandler.parseQuery(ex.getRequestURI().getQuery());
        String carModel = RepairRequestHandler.first(params, "carModel");
        Map<String, Double> pricesForModel = carModel == null ? null : PRICES.get(carModel);

        if (pricesForModel == null) {
            send(ex, 404, "{\"error\":\"unknown carModel\"}");
            return;
        }

        List<String> operations = params.getOrDefault("repairOperations", List.of());
        List<String> items = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        double total = 0;
        for (String operation : operations) {
            Double price = pricesForModel.get(operation);
            if (price == null) {
                unmatched.add(operation);
            } else {
                items.add("{\"description\":\"" + RepairRequestHandler.escape(operation) + "\",\"priceEstimation\":" + price + "}");
                total += price;
            }
        }

        String json = "{\"carModel\":\"" + RepairRequestHandler.escape(carModel) + "\"" + ",\"operations\":"
                + items.stream().collect(Collectors.joining(",", "[", "]")) + ",\"total\":" + round(total) + ",\"unmatched\":"
                + unmatched.stream()
                        .map(u -> "\"" + RepairRequestHandler.escape(u) + "\"")
                        .collect(Collectors.joining(",", "[", "]"))
                + "}";
        send(ex, 200, json);
    }

    // customerNumber, carModel, numberPlate, preferredDate, repairOperations
    // (repeated, "description:price")
    public static String toJson(Map<String, List<String>> params) {
        String operations = params.getOrDefault("repairOperations", List.of()).stream()
                .map(op -> op.split(":", 2))
                .map(parts -> "{\"description\":\"" + escape(parts[0]) + "\",\"priceEstimation\":" + parts[1] + "}")
                .collect(Collectors.joining(",", "[", "]"));

        return "{\"customerNumber\":" + first(params, "customerNumber") + ",\"carModel\":\"" + escape(first(params, "carModel")) + "\""
                + ",\"numberPlate\":\"" + escape(first(params, "numberPlate")) + "\"" + ",\"preferredDate\":\""
                + escape(first(params, "preferredDate")) + "\"" + ",\"repairOperations\":" + operations + "}";
    }

    public static String first(Map<String, List<String>> params, String name) {
        List<String> values = params.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static Map<String, List<String>> parseQuery(String query) {
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

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static void send(HttpExchange ex, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().put("Content-Type", List.of("application/json;charset=UTF-8"));
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // CSV columns: Car Model, Repair Action, Price Estimation
    private static Map<String, Map<String, Double>> load() {
        Map<String, Map<String, Double>> result = new HashMap<>();
        try (InputStream in = RepairRequestHandler.class.getClassLoader().getResourceAsStream("price_estimations.csv"); BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("utf-8")))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", 3);
                result.computeIfAbsent(parts[0], k -> new HashMap<>()).put(parts[1], Double.parseDouble(parts[2]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }
}
