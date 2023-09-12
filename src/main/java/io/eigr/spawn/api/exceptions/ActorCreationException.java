package io.eigr.spawn.api.exceptions;

/**
 * Actor Creation Exception.
 *
 * @author Paulo H3nrique Alves
 */
public class ActorCreationException extends SpawnException {

    public ActorCreationException() {
        super();
    }

    public ActorCreationException(String s) {
        super(s);
    }

    public ActorCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorCreationException(Throwable cause) {
        super(cause);
    }
}
