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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the server handles multiple clients in parallel instead of
 * queueing them behind each other: N clients each hit a route that blocks
 * for a fixed duration, and the whole batch finishes in roughly one
 * blocking period, not N of them back to back.
 */
class ConcurrencyTest {

    private static final int REQUEST_COUNT = 6;
    private static final long ROUTE_DELAY_MILLIS = 1000;

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static int port;
    private static HttpServer server;
    private static Thread serverThread;

    @BeforeAll
    static void startServer() throws IOException, InterruptedException {
        port = freePort();

        Router router = new Router();
        router.addGetRoute("/slow", (req, resp) -> {
            try {
                Thread.sleep(ROUTE_DELAY_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "done";
        });

        // Pool large enough to run every test request at once, isolating
        // this test from thread-pool-exhaustion behaviour.
        server = new HttpServer(router, new StaticFileService("/webroot"), REQUEST_COUNT);
        serverThread = new Thread(() -> {
            try {
                server.start(port);
            } catch (IOException ignored) {
                // expected once stop() closes the listening socket
            }
        });
        serverThread.start();
        Thread.sleep(300);
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.stop();
        serverThread.join(2000);
    }

    @Test
    void concurrentRequestsAreServedInParallelNotSequentially() throws Exception {
        ExecutorService clientPool = Executors.newFixedThreadPool(REQUEST_COUNT);
        List<Callable<HttpResponse<String>>> calls = new java.util.ArrayList<>();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            calls.add(() -> CLIENT.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/slow")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()));
        }

        long start = System.currentTimeMillis();
        List<Future<HttpResponse<String>>> results = clientPool.invokeAll(calls);
        long elapsedMillis = System.currentTimeMillis() - start;
        clientPool.shutdown();
        clientPool.awaitTermination(5, TimeUnit.SECONDS);

        for (Future<HttpResponse<String>> result : results) {
            assertEquals(200, result.get().statusCode());
            assertEquals("done", result.get().body());
        }

        // Sequential handling would take at least REQUEST_COUNT * ROUTE_DELAY_MILLIS.
        // Parallel handling should finish well under half of that.
        long sequentialLowerBound = REQUEST_COUNT * ROUTE_DELAY_MILLIS;
        assertTrue(elapsedMillis < sequentialLowerBound / 2,
                "Expected requests to run in parallel (elapsed " + elapsedMillis
                        + "ms should be well under the sequential lower bound of "
                        + sequentialLowerBound + "ms)");
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
