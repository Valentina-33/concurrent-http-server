package edu.eci.arsw.networking.http;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestParserTest {

    private BufferedReader readerFor(String raw) {
        return new BufferedReader(new StringReader(raw));
    }

    @Test
    void parsesMethodPathAndVersion() throws Exception {
        HttpRequest request = HttpRequestParser.parse(readerFor("GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n"));

        assertEquals(HttpMethod.GET, request.method());
        assertEquals("GET", request.rawMethod());
        assertEquals("/index.html", request.path());
        assertEquals("HTTP/1.1", request.version());
    }

    @Test
    void parsesQueryParameters() throws Exception {
        HttpRequest request = HttpRequestParser.parse(readerFor("GET /api/greeting?name=Ada HTTP/1.1\r\n\r\n"));

        assertEquals("/api/greeting", request.path());
        assertEquals("Ada", request.queryParams().get("name"));
    }

    @Test
    void decodesUrlEncodedQueryValues() throws Exception {
        HttpRequest request = HttpRequestParser.parse(readerFor("GET /api/greeting?name=Ada%20Lovelace HTTP/1.1\r\n\r\n"));

        assertEquals("Ada Lovelace", request.queryParams().get("name"));
    }

    @Test
    void parsesMultipleQueryParameters() throws Exception {
        HttpRequest request = HttpRequestParser.parse(readerFor("GET /search?q=a&limit=10 HTTP/1.1\r\n\r\n"));

        assertEquals("a", request.queryParams().get("q"));
        assertEquals("10", request.queryParams().get("limit"));
    }

    @Test
    void recognizesNonGetMethodsAsOther() throws Exception {
        HttpRequest request = HttpRequestParser.parse(readerFor("POST /api/square HTTP/1.1\r\n\r\n"));

        assertEquals(HttpMethod.OTHER, request.method());
        assertEquals("POST", request.rawMethod());
    }

    @Test
    void returnsNullWhenConnectionClosedImmediately() throws Exception {
        assertNull(HttpRequestParser.parse(readerFor("")));
    }

    @Test
    void throwsOnMalformedRequestLine() {
        BufferedReader reader = readerFor("NOT A REQUEST LINE AT ALL\r\n\r\n");
        assertThrows(MalformedRequestException.class, () -> HttpRequestParser.parse(reader));
    }

    @Test
    void throwsOnBlankRequestLine() {
        BufferedReader reader = readerFor("\r\n");
        assertThrows(MalformedRequestException.class, () -> HttpRequestParser.parse(reader));
    }

    @Test
    void consumesHeadersSoTheyDoNotLeakIntoTheNextRead() throws Exception {
        BufferedReader reader = readerFor("GET / HTTP/1.1\r\nHost: localhost\r\nAccept: */*\r\n\r\nBODY_MARKER");
        HttpRequestParser.parse(reader);

        String remaining = reader.readLine();
        assertTrue(remaining == null || remaining.equals("BODY_MARKER"));
    }

    @Test
    void ioErrorPropagatesFromUnderlyingReader() {
        BufferedReader broken = new BufferedReader(new StringReader("x")) {
            @Override
            public String readLine() throws IOException {
                throw new IOException("boom");
            }
        };
        assertThrows(IOException.class, () -> HttpRequestParser.parse(broken));
    }
}
