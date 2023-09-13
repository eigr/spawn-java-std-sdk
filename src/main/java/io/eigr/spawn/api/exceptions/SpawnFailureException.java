package io.eigr.spawn.api.exceptions;

/**
 * Generic Spawn exception.
 *
 * @author Paulo H3nrique Alves
 */
public class SpawnFailureException extends RuntimeException  {

    protected SpawnFailureException() {
        super();
    }

    public SpawnFailureException(String s) {
        super(s);
    }

    public SpawnFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpawnFailureException(Throwable cause) {
        super(cause);
    }
}
