package io.eigr.spawn.api.actors.workflows;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.InvocationOpts;
import io.eigr.spawn.api.ActorRef;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class SideEffect<T extends GeneratedMessageV3> {

    private final ActorRef actor;
    private final String command;
    private final T payload;
    private final Optional<InvocationOpts> opts;

    private SideEffect(ActorRef actor, String command, T payload) {
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.empty();
    }

    private SideEffect(ActorRef actor, String command, T payload, InvocationOpts opts) {
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.of(opts);
    }

    public static <T extends GeneratedMessageV3> SideEffect to(ActorRef actor, String command, T payload) {
        return new SideEffect(actor, command, payload);
    }

    public static <T extends GeneratedMessageV3> SideEffect to(ActorRef actor, String command, T payload, InvocationOpts opts) {
        return new SideEffect(actor, command, payload, opts);
    }

    public Protocol.SideEffect build() {
        Protocol.InvocationRequest.Builder requestBuilder = Protocol.InvocationRequest.newBuilder();

        if (this.opts.isPresent()) {
            InvocationOpts options = this.opts.get();
            if (options.getDelay().isPresent() && !options.getScheduledTo().isPresent()) {
                requestBuilder.setScheduledTo(options.getDelay().get());
            } else if (options.getScheduledTo().isPresent()) {
                requestBuilder.setScheduledTo(options.getScheduleTimeInLong());
            }
        }

        requestBuilder.setSystem(ActorOuterClass.ActorSystem.newBuilder()
                        .setName(this.actor.getActorSystem())
                        .build())
                .setActor(ActorOuterClass.Actor.newBuilder()
                        .setId(ActorOuterClass.ActorId.newBuilder()
                                .setSystem(this.actor.getActorSystem())
                                .setName(this.actor.getActorName())
                                .build())
                        .build())
                .setAsync(true)
                .setActionName(command)
                .setValue(Any.pack(payload));

        return Protocol.SideEffect.newBuilder()
                .setRequest(requestBuilder.build())
                .build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SideEffect.class.getSimpleName() + "[", "]")
                .add("actor='" + actor + "'")
                .add("command='" + command + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SideEffect<?> that = (SideEffect<?>) o;
        return Objects.equals(actor, that.actor) && Objects.equals(command, that.command) && Objects.equals(payload, that.payload) && Objects.equals(opts, that.opts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, command, payload, opts);
    }
}
