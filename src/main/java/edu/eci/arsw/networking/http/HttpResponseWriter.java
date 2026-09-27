package edu.eci.arsw.networking.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serializes an {@link HttpResponse} onto a socket output stream: status
 * line, headers computed from the actual byte length, a blank line, and the
 * raw body bytes.
 */
public final class HttpResponseWriter {

    private HttpResponseWriter() {
    }

    public static void write(OutputStream out, HttpResponse response) throws IOException {
        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 ").append(response.statusCode()).append(' ').append(response.reasonPhrase()).append("\r\n");
        headers.append("Content-Type: ").append(response.contentType()).append("\r\n");
        headers.append("Content-Length: ").append(response.body().length).append("\r\n");
        response.headers().forEach((name, value) -> headers.append(name).append(": ").append(value).append("\r\n"));
        headers.append("Connection: close\r\n");
        headers.append("\r\n");

        out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(response.body());
        out.flush();
    }
}
