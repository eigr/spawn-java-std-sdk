package io.eigr.spawn.api.exceptions;

/**
 * Actor Registration Exception.
 *
 * @author Paulo H3nrique Alves
 */
public class ActorRegistrationException extends SpawnException {

    public ActorRegistrationException() {
        super();
    }
    public ActorRegistrationException(String s) {
        super(s);
    }

    public ActorRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorRegistrationException(Throwable cause) {
        super(cause);
    }
}
