package io.eigr.spawn.api.actors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.InvocationOpts;
import io.eigr.spawn.api.exceptions.ActorInvokeException;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;
import io.eigr.spawn.internal.client.SpawnClient;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class ActorRef {
    private static final int CACHE_MAXIMUM_SIZE = 1_000;
    private static final int CACHE_EXPIRE_AFTER_WRITE_SECONDS = 60;
    private static final Cache<ActorOuterClass.ActorId, ActorRef> ACTOR_REF_CACHE = Caffeine.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRE_AFTER_WRITE_SECONDS))
            .build();

    private final ActorOuterClass.ActorId actorId;

    private final SpawnClient client;

    private ActorRef(ActorOuterClass.ActorId actorId, SpawnClient client) {
        this.client = client;
        this.actorId = actorId;
    }
    
    public static ActorRef of(SpawnClient client, String system, String name) throws Exception {
        ActorOuterClass.ActorId actorId = buildActorId(system, name);
        ActorRef ref = ACTOR_REF_CACHE.getIfPresent(actorId);
        if (Objects.nonNull(ref)){
            return ref;
        }

        ref = new ActorRef(actorId, client);
        ACTOR_REF_CACHE.put(actorId, ref);
        return ref;
    }

    public static ActorRef of(SpawnClient client, String system, String name, String parent) throws Exception {
        ActorOuterClass.ActorId actorId = buildActorId(system, name, parent);
        ActorRef ref = ACTOR_REF_CACHE.getIfPresent(actorId);
        if (Objects.nonNull(ref)){
            return ref;
        }

        spawnActor(actorId, client);
        ref = new ActorRef(actorId, client);
        ACTOR_REF_CACHE.put(actorId, ref);
        return ref;
    }

    public <T extends GeneratedMessageV3> Optional<Object>  invoke(String cmd, Class<T> outputType) throws Exception {
        Optional<Object> res = invokeActor(cmd, Empty.getDefaultInstance(), outputType, Optional.empty());
        if(res.isPresent() ){
            return Optional.of(outputType.cast(res.get()));
        }

        return res;
    }

    public <T extends GeneratedMessageV3> Optional<Object>  invoke(String cmd, Class<T> outputType, InvocationOpts opts) throws Exception {
        Optional<Object> res = invokeActor(cmd, Empty.getDefaultInstance(), outputType, Optional.ofNullable(opts));
        if(res.isPresent() ){
            return Optional.of(outputType.cast(res.get()));
        }

        return res;
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<Object> invoke(String cmd, S value, Class<T> outputType) throws Exception {
        Optional<Object> res = invokeActor(cmd, value, outputType, Optional.empty());
        if(res.isPresent() ){
            return Optional.of(outputType.cast(res.get()));
        }

        return res;
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<Object> invoke(String cmd, S value, Class<T> outputType, InvocationOpts opts) throws Exception {
        Optional<Object> res = invokeActor(cmd, value, outputType, Optional.ofNullable(opts));
        if(res.isPresent() ){
            return Optional.of(outputType.cast(res.get()));
        }

        return res;
    }

    public <T extends GeneratedMessageV3> void  invokeAsync(String cmd, Class<T> outputType) throws Exception {
        InvocationOpts opts = InvocationOpts.builder().async(true).build();
        invokeActor(cmd, Empty.getDefaultInstance(), outputType, Optional.of(opts));
    }

    public <T extends GeneratedMessageV3> void invokeAsync(String cmd, Class<T> outputType, InvocationOpts opts) throws Exception {
        InvocationOpts mergedOpts = InvocationOpts.builder()
                .async(true)
                .delay(opts.getDelay())
                .scheduledTo(opts.getScheduledTo())
                .build();

        invokeActor(cmd, Empty.getDefaultInstance(), outputType, Optional.ofNullable(mergedOpts));
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> void invokeAsync(String cmd, S value, Class<T> outputType) throws Exception {
        InvocationOpts opts = InvocationOpts.builder().async(true).build();
        invokeActor(cmd, value, outputType, Optional.of(opts));
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> void invokeAsync(String cmd, S value, Class<T> outputType, InvocationOpts opts) throws Exception {
        InvocationOpts mergedOpts = InvocationOpts.builder()
                .async(true)
                .delay(opts.getDelay())
                .scheduledTo(opts.getScheduledTo())
                .build();

        invokeActor(cmd, value, outputType, Optional.of(mergedOpts));
    }

    public String getActorSystem() {
        return this.actorId.getSystem();
    }

    public String getActorName() {
        return this.actorId.getName();
    }

    public Optional<String> maybeActorParentName() {
        return Optional.ofNullable(this.actorId.getParent());
    }

    public String getActorParentName() {
        return this.actorId.getParent();
    }

    public boolean isUnNamedActor() {
        if (Objects.nonNull(this.actorId.getParent())) {
            return true;
        }

        return false;
    }

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<Object> invokeActor(
            String cmd, S argument, Class<T> outputType, Optional<InvocationOpts> options) throws Exception {
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
                .setSystem(ActorOuterClass.ActorSystem.newBuilder().setName(this.actorId.getSystem()).build())
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
                        String.format("Unknown error when trying to invoke Actor %s", this.getActorName()));
            case ACTOR_NOT_FOUND:
                throw new ActorNotFoundException();
            case OK:
                if (resp.hasValue()) {
                    return Optional.of(resp.getValue().unpack(outputType));
                }
                return Optional.empty();
        }

        return Optional.empty();
    }

    private static ActorOuterClass.ActorId buildActorId(String system, String name) {
        ActorOuterClass.ActorId.Builder actorIdBuilder = ActorOuterClass.ActorId.newBuilder()
                .setSystem(system)
                .setName(name);

        return actorIdBuilder.build();
    }

    private static ActorOuterClass.ActorId buildActorId(String system, String name, String parent) {
        return ActorOuterClass.ActorId.newBuilder()
                .setSystem(system)
                .setName(name)
                .setParent(parent)
                .build();
    }

    private static void spawnActor(ActorOuterClass.ActorId actorId, SpawnClient client) throws Exception {
        Protocol.SpawnRequest req = Protocol.SpawnRequest.newBuilder()
                .addActors(actorId)
                .build();
        client.spawn(req);
    }
}
