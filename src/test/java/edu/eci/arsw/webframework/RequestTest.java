package edu.eci.arsw.webframework;

import edu.eci.arsw.networking.http.HttpMethod;
import edu.eci.arsw.networking.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestTest {

    @Test
    void getValueReturnsQueryParam() {
        HttpRequest raw = new HttpRequest(HttpMethod.GET, "GET", "/hello",
                Map.of("name", "Pedro"), "HTTP/1.1");

        Request request = new Request(raw);

        assertEquals("Pedro", request.getValue("name"));
    }

    @Test
    void getValueReturnsNullWhenParamIsMissing() {
        HttpRequest raw = new HttpRequest(HttpMethod.GET, "GET", "/hello",
                Map.of(), "HTTP/1.1");

        Request request = new Request(raw);

        assertNull(request.getValue("name"));
    }
}
