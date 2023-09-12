package io.eigr.spawn.api.exceptions;

/**
 * Generic Spawn exception.
 *
 * @author Paulo H3nrique Alves
 */
public class SpawnException extends RuntimeException {

    public SpawnException() {
        super();
    }

    public SpawnException(String s) {
        super(s);
    }

    public SpawnException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpawnException(Throwable cause) {
        super(cause);
    }
}
