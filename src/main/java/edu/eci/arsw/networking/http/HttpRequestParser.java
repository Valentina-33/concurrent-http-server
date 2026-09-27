package edu.eci.arsw.networking.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the request line and headers of one HTTP request from a socket's
 * input stream. Only the request line is turned into an {@link HttpRequest};
 * headers are read (to keep the stream in a consistent state) and discarded,
 * since none of the hardcoded services need them.
 */
public final class HttpRequestParser {

    private HttpRequestParser() {
    }

    /**
     * Reads and parses one request. Returns {@code null} when the client
     * closed the connection before sending anything (a common, harmless
     * occurrence with keep-alive-less browsers probing a socket).
     */
    public static HttpRequest parse(BufferedReader reader) throws IOException, MalformedRequestException {
        String requestLine = reader.readLine();
        if (requestLine == null) {
            return null;
        }
        if (requestLine.isBlank()) {
            throw new MalformedRequestException("Empty request line");
        }

        consumeHeaders(reader);

        String[] parts = requestLine.trim().split("\\s+");
        if (parts.length != 3) {
            throw new MalformedRequestException("Request line must have method, path and version: " + requestLine);
        }

        String rawMethod = parts[0];
        String target = parts[1];
        String version = parts[2];

        int queryIndex = target.indexOf('?');
        String path = queryIndex >= 0 ? target.substring(0, queryIndex) : target;
        String query = queryIndex >= 0 ? target.substring(queryIndex + 1) : "";

        String decodedPath = decode(path);
        Map<String, String> queryParams = parseQuery(query);

        return new HttpRequest(HttpMethod.parse(rawMethod), rawMethod, decodedPath, queryParams, version);
    }

    private static void consumeHeaders(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // Headers are intentionally ignored: this lab server only needs
            // the request line to select and run a hardcoded route.
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(decode(key), decode(value));
        }
        return params;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new UncheckedIOException(new IOException("Invalid percent-encoding: " + value, e));
        }
    }
}
