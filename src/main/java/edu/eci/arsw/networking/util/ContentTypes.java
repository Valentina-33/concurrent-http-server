package edu.eci.arsw.networking.util;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Maps a resource's file extension to the HTTP content type the browser
 * needs in order to interpret the response correctly (build the DOM, run a
 * script, or decode an image).
 */
public final class ContentTypes {

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            entry("html", "text/html; charset=utf-8"),
            entry("htm", "text/html; charset=utf-8"),
            entry("js", "text/javascript; charset=utf-8"),
            entry("css", "text/css; charset=utf-8"),
            entry("png", "image/png"),
            entry("jpg", "image/jpeg"),
            entry("jpeg", "image/jpeg"),
            entry("gif", "image/gif"),
            entry("svg", "image/svg+xml"),
            entry("ico", "image/x-icon"),
            entry("txt", "text/plain; charset=utf-8"),
            entry("json", "application/json; charset=utf-8")
    );

    private static final String DEFAULT_TYPE = "application/octet-stream";

    private ContentTypes() {
    }

    public static String resolve(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return DEFAULT_TYPE;
        }
        String extension = fileName.substring(dot + 1).toLowerCase();
        return BY_EXTENSION.getOrDefault(extension, DEFAULT_TYPE);
    }

    public static boolean isSupportedExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return BY_EXTENSION.containsKey(fileName.substring(dot + 1).toLowerCase());
    }
}
