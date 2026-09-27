package edu.eci.arsw.networking.http;

/**
 * Signals that the bytes read from a client socket could not be parsed as a
 * well-formed HTTP request line. The connection handler catches this and
 * replies with a 400 instead of letting the exception kill the server loop.
 */
public class MalformedRequestException extends Exception {

    public MalformedRequestException(String message) {
        super(message);
    }
}
