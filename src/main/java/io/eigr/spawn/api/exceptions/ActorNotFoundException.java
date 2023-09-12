package io.eigr.spawn.api.exceptions;

public final class ActorNotFoundException extends SpawnException {

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
