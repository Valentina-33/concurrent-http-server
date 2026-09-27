package edu.eci.arsw.networking.http;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable representation of the single line of an HTTP request this lab
 * server cares about: method, path, and decoded query parameters. Headers
 * and body are intentionally not modeled since every hardcoded service only
 * needs GET + query string.
 */
public final class HttpRequest {

    private final HttpMethod method;
    private final String rawMethod;
    private final String path;
    private final Map<String, String> queryParams;
    private final String version;

    public HttpRequest(HttpMethod method, String rawMethod, String path,
                        Map<String, String> queryParams, String version) {
        this.method = method;
        this.rawMethod = rawMethod;
        this.path = path;
        this.queryParams = Collections.unmodifiableMap(queryParams);
        this.version = version;
    }

    public HttpMethod method() {
        return method;
    }

    public String rawMethod() {
        return rawMethod;
    }

    public String path() {
        return path;
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public String version() {
        return version;
    }
}
