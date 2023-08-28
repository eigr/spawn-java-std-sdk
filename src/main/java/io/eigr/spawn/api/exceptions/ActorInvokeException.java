package io.eigr.spawn.api.exceptions;

public final class ActorInvokeException extends IllegalStateException {

    public ActorInvokeException() {}
    public ActorInvokeException(String message) {
        super(message);
    }
}
