package io.eigr.spawn.api.actors.workflows;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.ActorRef;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.StringJoiner;

public final class Pipe {

    private final ActorRef actor;

    private final String action;

    private Pipe(ActorRef actor, String action) {
        this.actor = actor;
        this.action = action;
    }

    @NotNull
    public static Pipe to(ActorRef actor, String action) {
        return new Pipe(actor, action);
    }

    public ActorRef getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public Protocol.Pipe build() {
        return Protocol.Pipe.newBuilder()
                .setActor(this.actor.getActorName())
                .setActionName(this.action)
                .build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Pipe.class.getSimpleName() + "[", "]")
                .add("actor='" + actor + "'")
                .add("action='" + action + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pipe pipe = (Pipe) o;
        return Objects.equals(actor, pipe.actor) && Objects.equals(action, pipe.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, action);
    }
}
