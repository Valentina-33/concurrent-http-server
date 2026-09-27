package edu.eci.arsw.webframework;

import java.io.IOException;

public class WebFramework {

    private static final Router ROUTER = new Router();
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_POOL_SIZE = 20;

    private static String staticFilesLocation = "/webroot";
    private static HttpServer server;

    public static void staticfiles(String root) {
        staticFilesLocation = root;
    }

    public static void get(String path, RouteHandler handler) {
        ROUTER.addGetRoute(path, handler);
    }

    public static void start() throws IOException {
        start(resolvePort());
    }

    public static void start(int port) throws IOException {
        server = new HttpServer(ROUTER, new StaticFileService(staticFilesLocation), resolvePoolSize());
        server.start(port);
    }

    public static void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private static int resolvePort() {
        String portValue = System.getenv("PORT");
        return (portValue == null || portValue.isBlank()) ? DEFAULT_PORT : Integer.parseInt(portValue);
    }

    /**
     * Size of the worker thread pool that handles connections at the same
     * time. It can be set from outside, so a small EC2 instance and a
     * developer laptop can each use a different size without rebuilding.
     */
    private static int resolvePoolSize() {
        String poolSizeValue = System.getenv("THREAD_POOL_SIZE");
        return (poolSizeValue == null || poolSizeValue.isBlank()) ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSizeValue);
    }
}
