package edu.eci.arsw.webframework;

import edu.eci.arsw.networking.http.HttpResponse;
import edu.eci.arsw.networking.util.ContentTypes;
import edu.eci.arsw.networking.util.PathNormalizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Serves the files registered through {@code staticfiles(...)}. Looks them up
 * as classpath resources, so they can live inside the packaged jar.
 */
public final class StaticFileService {

    private static final String INDEX_RESOURCE = "index.html";

    private final String root;

    public StaticFileService(String location) {
        String safeLocation = location == null ? "" : location;
        this.root = safeLocation.startsWith("/") ? safeLocation.substring(1) : safeLocation;
    }

    public HttpResponse serve(String requestPath) {
        Optional<String> normalized = PathNormalizer.normalize(requestPath);
        if (normalized.isEmpty()) {
            return HttpResponse.badRequestPlain("400 Bad Request: unsafe path " + requestPath);
        }

        String relative = normalized.get().isEmpty() ? INDEX_RESOURCE : normalized.get();
        byte[] bytes = readResource(root + "/" + relative);
        if (bytes == null) {
            return HttpResponse.notFound(requestPath);
        }
        return HttpResponse.ok(bytes, ContentTypes.resolve(relative));
    }

    private byte[] readResource(String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            in.transferTo(buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
