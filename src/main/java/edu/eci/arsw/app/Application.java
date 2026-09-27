package edu.eci.arsw.app;

import static edu.eci.arsw.webframework.WebFramework.*;

public class Application {

    public static void main(String[] args) throws Exception {

        staticfiles("/webroot");

        get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) {
                name = "world";
            }

            String greetingPrefix = System.getenv().getOrDefault("GREETING_PREFIX", "Hello");
            return greetingPrefix + " " + name;
        });

        get("/pi", (req, resp) -> {
            resp.setContentType("text/plain; charset=utf-8");
            return String.valueOf(Math.PI);
        });

        // Demonstrates concurrent request handling: this route deliberately
        // blocks for ~2 seconds. With the worker thread pool, several clients
        // hitting /slow at the same time all finish in roughly the same ~2s
        // window instead of queueing up behind each other.
        get("/slow", (req, resp) -> {
            resp.setContentType("text/plain; charset=utf-8");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Finished a 2-second task on thread " + Thread.currentThread().getName();
        });

        String environment = System.getenv().getOrDefault("APP_ENV", "development");
        if (environment.equals("development")) {
            get("/shutdown", (req, resp) -> {
                stop();
                return "Server will stop after this response.";
            });
        }

        start();
    }
}
