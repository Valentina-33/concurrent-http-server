package edu.eci.arsw.networking.http;

/**
 * HTTP methods recognized by the lab server. Only GET is supported by any
 * route; every other method reaches {@link HttpResponse#methodNotAllowed()}.
 */
public enum HttpMethod {
    GET,
    OTHER;

    public static HttpMethod parse(String token) {
        if ("GET".equalsIgnoreCase(token)) {
            return GET;
        }
        return OTHER;
    }
}
