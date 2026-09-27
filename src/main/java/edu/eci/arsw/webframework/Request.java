package edu.eci.arsw.webframework;

import edu.eci.arsw.networking.http.HttpRequest;

public final class Request {

    private final HttpRequest raw;

    public Request(HttpRequest raw) {
        this.raw = raw;
    }

    public String getValue(String name) {
        return raw.queryParams().get(name);
    }

    public String path() {
        return raw.path();
    }
}
