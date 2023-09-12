package io.eigr.spawn.api.exceptions;

/**
 * Actor Registration Exception.
 *
 * @author Paulo H3nrique Alves
 */
public class ActorNotFoundException extends ActorInvocationException {

    public ActorNotFoundException() {
        super();
    }
    public ActorNotFoundException(String s) {
        super(s);
    }

    public ActorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorNotFoundException(Throwable cause) {
        super(cause);
    }
}
