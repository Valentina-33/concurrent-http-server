package edu.eci.arsw.webframework;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts a real HttpServer on a free local port and drives it with real HTTP
 * requests, exactly like a browser would: lambda routes, static files, and
 * the 404 fallback.
 */
class HttpServerIntegrationTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static int port;
    private static HttpServer server;
    private static Thread serverThread;

    @BeforeAll
    static void startServer() throws IOException, InterruptedException {
        port = freePort();

        Router router = new Router();
        router.addGetRoute("/hello", (req, resp) -> {
            String name = req.getValue("name");
            return "Hello " + (name == null ? "world" : name);
        });
        router.addGetRoute("/pi", (req, resp) -> String.valueOf(Math.PI));

        server = new HttpServer(router, new StaticFileService("/webroot"), 8);
        serverThread = new Thread(() -> {
            try {
                server.start(port);
            } catch (IOException ignored) {
                // thrown only when the socket is closed while stopping
            }
        });
        serverThread.start();
        Thread.sleep(500); // give the socket time to start listening
    }

    @AfterAll
    static void stopServer() throws Exception {
        // stop() closes the listening socket directly, which unblocks the
        // accept loop immediately -- no extra "wake-up" request needed.
        server.stop();
        serverThread.join(2000);
    }

    @Test
    void helloReturnsGreetingUsingQueryParam() throws Exception {
        HttpResponse<String> response = get("/hello?name=Pedro");

        assertEquals(200, response.statusCode());
        assertEquals("Hello Pedro", response.body());
    }

    @Test
    void helloFallsBackToWorldWhenNameIsMissing() throws Exception {
        HttpResponse<String> response = get("/hello");

        assertEquals(200, response.statusCode());
        assertEquals("Hello world", response.body());
    }

    @Test
    void piReturnsMathPiAsPlainText() throws Exception {
        HttpResponse<String> response = get("/pi");

        assertEquals(200, response.statusCode());
        assertEquals(String.valueOf(Math.PI), response.body());
    }

    @Test
    void staticIndexHtmlIsServed() throws Exception {
        HttpResponse<String> response = get("/index.html");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<html"));
    }

    @Test
    void unknownRouteAndUnknownFileReturn404() throws Exception {
        HttpResponse<String> response = get("/does-not-exist");

        assertEquals(404, response.statusCode());
    }

    static HttpResponse<String> get(String path) throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static int currentPort() {
        return port;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
