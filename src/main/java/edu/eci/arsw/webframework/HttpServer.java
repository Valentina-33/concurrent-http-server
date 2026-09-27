package edu.eci.arsw.webframework;

import edu.eci.arsw.networking.http.HttpMethod;
import edu.eci.arsw.networking.http.HttpRequest;
import edu.eci.arsw.networking.http.HttpRequestParser;
import edu.eci.arsw.networking.http.HttpResponse;
import edu.eci.arsw.networking.http.HttpResponseWriter;
import edu.eci.arsw.networking.http.MalformedRequestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Accepts client connections and hands each one to a worker thread from a
 * bounded pool, so multiple clients are served in parallel instead of one at
 * a time. The accept loop itself stays single-threaded: its only job is to
 * accept a socket and submit it, never to run application code, so it is
 * never the bottleneck.
 *
 * <p>Shutdown is graceful: {@link #stop()} stops new connections from being
 * accepted, but every request already handed to the pool is allowed to run
 * to completion (up to {@link #SHUTDOWN_TIMEOUT_SECONDS}) before the process
 * exits, so a client mid-request never has its connection cut.</p>
 */
public final class HttpServer {

    private static final Logger LOGGER = Logger.getLogger(HttpServer.class.getName());
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final Router router;
    private final StaticFileService staticFileService;
    private final ExecutorService workerPool;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public HttpServer(Router router, StaticFileService staticFileService, int poolSize) {
        this.router = router;
        this.staticFileService = staticFileService;
        this.workerPool = Executors.newFixedThreadPool(poolSize);
    }

    public void start(int port) throws IOException {
        running = true;

        try (ServerSocket socket = new ServerSocket(port)) {
            this.serverSocket = socket;
            LOGGER.info(() -> "Concurrent web framework server listening on port " + port
                    + " (worker pool ready)");
            while (running) {
                try {
                    Socket clientSocket = socket.accept();
                    workerPool.submit(() -> handleConnection(clientSocket));
                } catch (SocketException e) {
                    if (running) {
                        LOGGER.log(Level.WARNING, "Accept loop error", e);
                    }
                    // else: stop() closed the socket on purpose to unblock accept() -- expected.
                }
            }
        } finally {
            awaitInFlightRequests();
        }

        LOGGER.info("Server stopped gracefully: all in-flight requests finished.");
    }

    /**
     * Stops accepting new connections and closes the listening socket, which
     * unblocks the accept loop. Requests already submitted to the worker pool
     * keep running; {@link #awaitInFlightRequests()} waits for them.
     */
    public void stop() {
        running = false;
        ServerSocket socket = this.serverSocket;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing the server socket during shutdown", e);
            }
        }
    }

    private void awaitInFlightRequests() {
        workerPool.shutdown(); // stop accepting new tasks; lets submitted ones finish
        try {
            if (!workerPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warning("Worker pool did not finish within " + SHUTDOWN_TIMEOUT_SECONDS
                        + "s; forcing remaining requests to stop.");
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            HttpRequest httpRequest;
            try {
                httpRequest = HttpRequestParser.parse(in);
            } catch (MalformedRequestException e) {
                HttpResponseWriter.write(out, HttpResponse.badRequestPlain("400 Bad Request"));
                return;
            }
            if (httpRequest == null) {
                return; // client closed the socket without sending anything
            }

            HttpResponseWriter.write(out, dispatch(httpRequest));

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "I/O error while handling a connection", e);
        }
    }

    private HttpResponse dispatch(HttpRequest httpRequest) {
        if (httpRequest.method() != HttpMethod.GET) {
            return HttpResponse.methodNotAllowed(httpRequest.rawMethod());
        }

        Optional<RouteHandler> route = router.match(httpRequest.path());
        if (route.isPresent()) {
            return runRoute(route.get(), httpRequest);
        }

        return staticFileService.serve(httpRequest.path());
    }

    private HttpResponse runRoute(RouteHandler handler, HttpRequest httpRequest) {
        try {
            Request request = new Request(httpRequest);
            Response response = new Response();
            Object result = handler.handle(request, response);
            String body = result == null ? "" : result.toString();
            return HttpResponse.of(response.getStatus(), reasonPhrase(response.getStatus()),
                    response.getContentType(), body.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unhandled error in route " + httpRequest.path(), e);
            return HttpResponse.internalError();
        }
    }

    private static String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };
    }
}
