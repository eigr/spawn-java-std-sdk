package io.eigr.spawn.api.actors.workflows;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.InvocationOpts;

import java.util.Optional;

public final class SideEffect<T extends GeneratedMessageV3> {

    private final String system;
    private final String actor;
    private final String command;
    private final T payload;


    private final Optional<InvocationOpts> opts;

    private SideEffect(String system, String actor, String command, T payload) {
        this.system = system;
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.empty();
    }

    private SideEffect(String system, String actor, String command, T payload, InvocationOpts opts) {
        this.system = system;
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.of(opts);
    }

    public static <T extends GeneratedMessageV3> SideEffect to(String system, String actor, String command, T payload) {
        return new SideEffect(system, actor, command, payload);
    }

    public static <T extends GeneratedMessageV3> SideEffect to(String system, String actor, String command, T payload, InvocationOpts opts) {
        return new SideEffect(system, actor, command, payload, opts);
    }

    public Protocol.SideEffect build() {
        Protocol.InvocationRequest.Builder requestBuilder = Protocol.InvocationRequest.newBuilder();

        if (this.opts.isPresent()) {
            InvocationOpts options = this.opts.get();
            if (options.getDelay().isPresent() && !options.getScheduledTo().isPresent()) {
                requestBuilder.setScheduledTo(options.getDelay().get());
            } else if (options.getScheduledTo().isPresent()){
                requestBuilder.setScheduledTo(options.getScheduleTimeInLong());
            }
        }

        requestBuilder.setSystem(ActorOuterClass.ActorSystem.newBuilder()
                        .setName(this.system)
                        .build())
                .setActor(ActorOuterClass.Actor.newBuilder()
                        .setId(ActorOuterClass.ActorId.newBuilder()
                                .setSystem(this.system)
                                .setName(this.actor)
                                .build())
                        .build())
                .setAsync(true)
                .setActionName(command)
                .setValue(Any.pack(payload));

        return Protocol.SideEffect.newBuilder()
                .setRequest(requestBuilder.build())
                .build();
    }
}
