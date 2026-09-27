package edu.eci.arsw.networking.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpResponseWriterTest {

    @Test
    void writesStatusLineHeadersBlankLineAndBody() throws Exception {
        HttpResponse response = HttpResponse.ok("hello".getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpResponseWriter.write(out, response);

        String written = out.toString(StandardCharsets.UTF_8);
        assertTrue(written.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(written.contains("Content-Type: text/plain; charset=utf-8\r\n"));
        assertTrue(written.contains("Content-Length: 5\r\n"));
        assertTrue(written.contains("\r\n\r\nhello"));
    }

    @Test
    void contentLengthMatchesByteLengthNotCharacterCount() throws Exception {
        // "café" is 4 characters but 5 bytes in UTF-8 -- the length must be
        // computed from the encoded bytes, not String#length().
        byte[] body = "café".getBytes(StandardCharsets.UTF_8);
        HttpResponse response = HttpResponse.ok(body, "text/plain; charset=utf-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpResponseWriter.write(out, response);

        String written = out.toString(StandardCharsets.UTF_8);
        assertTrue(written.contains("Content-Length: " + body.length + "\r\n"));
    }

    @Test
    void extraHeadersFromTheResponseAreWritten() throws Exception {
        HttpResponse response = HttpResponse.methodNotAllowed("POST");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpResponseWriter.write(out, response);

        String written = out.toString(StandardCharsets.UTF_8);
        assertTrue(written.startsWith("HTTP/1.1 405 Method Not Allowed\r\n"));
        assertTrue(written.contains("Allow: GET\r\n"));
    }

    @Test
    void binaryBodyIsWrittenByteForByte() throws Exception {
        byte[] body = new byte[]{0, 1, 2, (byte) 0xFF, 127, (byte) 0x80};
        HttpResponse response = HttpResponse.ok(body, "application/octet-stream");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpResponseWriter.write(out, response);

        byte[] written = out.toByteArray();
        byte[] tail = new byte[body.length];
        System.arraycopy(written, written.length - body.length, tail, 0, body.length);
        assertTrue(java.util.Arrays.equals(body, tail));
    }
}
