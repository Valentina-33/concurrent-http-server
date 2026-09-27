package edu.eci.arsw.webframework;

@FunctionalInterface
public interface RouteHandler {
    Object handle(Request request, Response response);
}
