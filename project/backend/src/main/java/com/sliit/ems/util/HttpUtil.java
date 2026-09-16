package com.sliit.ems.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared helpers for reading requests and writing JSON responses from
 * com.sun.net.httpserver.HttpHandler implementations, plus small utilities
 * for query-string parsing and CORS so every controller doesn't repeat
 * this boilerplate.
 */
public class HttpUtil {

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        if (body == null || body.isBlank()) return new LinkedHashMap<>();
        return Json.parseObject(body);
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = Json.write(payload).getBytes(StandardCharsets.UTF_8);
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = Json.obj();
        err.put("error", message);
        sendJson(exchange, statusCode, err);
    }

    public static void setCorsHeaders(HttpExchange exchange) {
        // Vite dev server default port
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    public static Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        URI uri = exchange.getRequestURI();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = java.net.URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    /** Extracts the trailing numeric path segment, e.g. /api/employees/12 -> 12. Returns -1 if not numeric. */
    public static int extractIdFromPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length == 0) return -1;
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getBearerToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring("Bearer ".length()).trim();
    }
}
