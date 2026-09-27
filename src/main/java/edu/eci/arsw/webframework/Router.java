package edu.eci.arsw.webframework;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Router {

    private final Map<String, RouteHandler> getRoutes = new LinkedHashMap<>();

    public void addGetRoute(String path, RouteHandler handler) {
        getRoutes.put(path, handler);
    }

    public Optional<RouteHandler> match(String path) {
        return Optional.ofNullable(getRoutes.get(path));
    }
}
