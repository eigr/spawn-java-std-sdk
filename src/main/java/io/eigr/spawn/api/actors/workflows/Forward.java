package io.eigr.spawn.api.actors.workflows;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.actors.ActorRef;

import java.util.Objects;
import java.util.StringJoiner;

public final class Forward {

    private final ActorRef actor;

    private final String action;

    private Forward(ActorRef actor, String action) {
        this.actor = actor;
        this.action = action;
    }

    public static Forward to(ActorRef actor, String action) {
        return new Forward(actor, action);
    }

    public ActorRef getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public Protocol.Forward build() {
        return Protocol.Forward.newBuilder()
                .setActor(this.actor.getActorName())
                .setActionName(this.action)
                .build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Forward.class.getSimpleName() + "[", "]")
                .add("actor='" + actor + "'")
                .add("action='" + action + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Forward forward = (Forward) o;
        return Objects.equals(actor, forward.actor) && Objects.equals(action, forward.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, action);
    }
}
