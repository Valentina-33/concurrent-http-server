package edu.eci.arsw.networking.http;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable HTTP response: a status line, a content type, and a body already
 * turned into bytes (so text and binary resources go through the same
 * response path, as required by the lab). Most responses need no extra
 * headers beyond Content-Type/Content-Length; the few that do (a 405
 * advertising the methods it does accept) carry them in {@link #headers()}.
 */
public final class HttpResponse {

    private final int statusCode;
    private final String reasonPhrase;
    private final String contentType;
    private final byte[] body;
    private final Map<String, String> headers;

    private HttpResponse(int statusCode, String reasonPhrase, String contentType, byte[] body,
                          Map<String, String> headers) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.contentType = contentType;
        this.body = body;
        this.headers = headers;
    }

    public static HttpResponse of(int statusCode, String reasonPhrase, String contentType, byte[] body) {
        return new HttpResponse(statusCode, reasonPhrase, contentType, body, Collections.emptyMap());
    }

    public static HttpResponse ok(byte[] body, String contentType) {
        return new HttpResponse(200, "OK", contentType, body, Collections.emptyMap());
    }

    public static HttpResponse json(int statusCode, String reasonPhrase, String jsonBody) {
        return new HttpResponse(statusCode, reasonPhrase, "application/json; charset=utf-8",
                jsonBody.getBytes(StandardCharsets.UTF_8), Collections.emptyMap());
    }

    public static HttpResponse badRequest(String jsonBody) {
        return json(400, "Bad Request", jsonBody);
    }

    public static HttpResponse notFound(String path) {
        return plainText(404, "Not Found", "404 Not Found: " + path);
    }

    public static HttpResponse methodNotAllowed(String method) {
        // RFC 7231 Sec. 6.5.5 recommends a 405 response name the methods the
        // resource *does* accept, via the Allow header. This lab only ever
        // accepts GET, so the header is always the same fixed value.
        return new HttpResponse(405, "Method Not Allowed", "text/plain; charset=utf-8",
                ("405 Method Not Allowed: " + method).getBytes(StandardCharsets.UTF_8),
                Map.of("Allow", "GET"));
    }

    public static HttpResponse badRequestPlain(String message) {
        return plainText(400, "Bad Request", message);
    }

    public static HttpResponse internalError() {
        return plainText(500, "Internal Server Error", "500 Internal Server Error");
    }

    private static HttpResponse plainText(int statusCode, String reasonPhrase, String message) {
        return new HttpResponse(statusCode, reasonPhrase, "text/plain; charset=utf-8",
                message.getBytes(StandardCharsets.UTF_8), Collections.emptyMap());
    }

    public int statusCode() {
        return statusCode;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }
}
