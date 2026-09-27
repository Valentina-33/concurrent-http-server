package edu.eci.arsw.networking.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Resolves a browser-supplied request path into a safe, relative path inside
 * the public resources area, or {@link Optional#empty()} when the request
 * tries to leave that area (a "../" traversal attempt, however encoded the
 * segments arrive after URL-decoding).
 *
 * <p>The normalizer never asks the filesystem or the classloader whether a
 * ".." segment escapes the root: it rejects the request as soon as the
 * segment stack would go negative, so no traversal payload ever reaches the
 * resource lookup.</p>
 */
public final class PathNormalizer {

    private PathNormalizer() {
    }

    public static Optional<String> normalize(String requestPath) {
        if (requestPath == null) {
            return Optional.empty();
        }
        // Reject raw NUL bytes and backslashes some clients use to try to
        // smuggle a Windows-style traversal past a naive "/" only check.
        if (requestPath.indexOf('\0') >= 0 || requestPath.indexOf('\\') >= 0) {
            return Optional.empty();
        }

        String[] segments = requestPath.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (stack.isEmpty()) {
                    return Optional.empty();
                }
                stack.removeLast();
            } else {
                stack.addLast(segment);
            }
        }
        return Optional.of(String.join("/", stack));
    }
}
