package io.eigr.spawn.api.exceptions;

public class ActorInvocationException extends SpawnException {

    public ActorInvocationException() {
        super();
    }

    public ActorInvocationException(String message) {
        super(message);
    }

    public ActorInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorInvocationException(Throwable cause) {
        super(cause);
    }
}
