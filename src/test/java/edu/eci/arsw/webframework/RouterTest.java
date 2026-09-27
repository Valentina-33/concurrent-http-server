package edu.eci.arsw.webframework;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    @Test
    void matchReturnsRegisteredHandler() {
        Router router = new Router();
        RouteHandler handler = (req, resp) -> "ok";
        router.addGetRoute("/hello", handler);

        Optional<RouteHandler> found = router.match("/hello");

        assertTrue(found.isPresent());
        assertEquals(handler, found.get());
    }

    @Test
    void matchReturnsEmptyForUnregisteredPath() {
        Router router = new Router();

        assertTrue(router.match("/unknown").isEmpty());
    }
}
