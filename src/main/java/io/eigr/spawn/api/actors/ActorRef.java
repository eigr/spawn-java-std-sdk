package io.eigr.spawn.api.actors;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.InvocationOpts;
import io.eigr.spawn.api.exceptions.ActorInvokeException;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;
import io.eigr.spawn.internal.client.SpawnClient;

import java.util.Optional;

public final class ActorRef {

    private final ActorOuterClass.ActorId actorId;

    private final String name;

    private final String system;

    private final Optional<String> parent;

    private final SpawnClient client;

    private ActorRef(SpawnClient client, String system, String name) throws Exception {
        this.client = client;
        this.system = system;
        this.name = name;
        this.parent = Optional.empty();
        this.actorId = buildActorId();
        if (this.parent.isPresent()){
            spawnActor();
        }
    }

    private ActorRef(SpawnClient client, String system, String name, String parent) throws Exception {
        this.client = client;
        this.system = system;
        this.name = name;
        this.parent = Optional.of(parent);
        this.actorId = buildActorId();
        if (this.parent.isPresent()){
            spawnActor();
        }
    }


    public static ActorRef of(SpawnClient client, String system, String name) throws Exception {
        return new ActorRef(client, system, name);
    }

    public static ActorRef of(SpawnClient client, String system, String name, String parent) throws Exception {
        return new ActorRef(client, system, name, parent);
    }

    public <T extends GeneratedMessageV3> Object invoke(String cmd, Class<T> outputType) throws Exception {
        Object res = invokeActor(cmd, Empty.getDefaultInstance(), outputType, Optional.empty());
        return outputType.cast(res);
    }

    public <T extends GeneratedMessageV3> Object invoke(String cmd, Class<T> outputType, Optional<InvocationOpts> opts) throws Exception {
        Object res = invokeActor(cmd, Empty.getDefaultInstance(), outputType, opts);
        return outputType.cast(res);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String cmd, S value, Class<T> outputType) throws Exception {
        Object res = invokeActor(cmd, value, outputType, Optional.empty());
        return outputType.cast(res);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String cmd, S value, Class<T> outputType, Optional<InvocationOpts> opts) throws Exception {
        Object res = invokeActor(cmd, value, outputType, opts);
        return outputType.cast(res);
    }

    public String getActorSystem() {
        return this.system;
    }

    public String getActorName() {
        return this.name;
    }

    public Optional<String> maybeActorParentName() {
        return this.parent;
    }

    public String getActorParentName() {
        return this.parent.get();
    }

    public boolean isUnnamedActor() {
        return Optional.empty().isPresent();
    }

    private ActorOuterClass.ActorId buildActorId() {
        ActorOuterClass.ActorId.Builder actorIdBuilder = ActorOuterClass.ActorId.newBuilder()
                .setSystem(this.system)
                .setName(this.name);

        if (this.parent.isPresent()) {
            actorIdBuilder.setParent(this.parent.get());
        }

        return actorIdBuilder.build();
    }

    private void spawnActor() throws Exception {
        Protocol.SpawnRequest req = Protocol.SpawnRequest.newBuilder()
                .addActors(this.actorId)
                .build();
        this.client.spawn(req);
    }

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invokeActor(String cmd, S argument, Class<T> outputType, Optional<InvocationOpts> options) throws Exception {
        Protocol.InvocationRequest.Builder invocationRequestBuilder = Protocol.InvocationRequest.newBuilder();

        if (options.isPresent()) {
            InvocationOpts opts = options.get();
            invocationRequestBuilder.setAsync(opts.isAsync());

            if (opts.getDelay().isPresent() && !opts.getScheduledTo().isPresent()) {
                invocationRequestBuilder.setScheduledTo(opts.getDelay().get());
            } else if (opts.getScheduledTo().isPresent()) {
                invocationRequestBuilder.setScheduledTo(opts.getScheduleTimeInLong());
            }
        }

        final ActorOuterClass.Actor actorRef = ActorOuterClass.Actor.newBuilder()
                .setId(this.actorId)
                .build();

        Any commandArg = Any.pack(argument);

        invocationRequestBuilder
                .setSystem(ActorOuterClass.ActorSystem.newBuilder().setName(this.system).build())
                .setActor(actorRef)
                .setActionName(cmd)
                .setValue(commandArg)
                .build();

        Protocol.InvocationResponse resp = this.client.invoke(invocationRequestBuilder.build());
        final Protocol.RequestStatus status = resp.getStatus();
        switch (status.getStatus()) {
            case UNKNOWN:
            case ERROR:
            case UNRECOGNIZED:
                throw new ActorInvokeException(
                        String.format("Unknown error when trying to invoke Actor %s", this.name));
            case ACTOR_NOT_FOUND:
                throw new ActorNotFoundException();
            case OK:
                if (resp.hasValue()) {
                    return resp.getValue().unpack(outputType);
                }
                return null;
        }

        throw new ActorNotFoundException();
    }
}
