package io.eigr.spawn.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.internal.transport.client.SpawnClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ActorRef is responsible for representing an instance of an Actor
 *
 * @author Adriano Santos
 */
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

    /**
     * <p>This method is responsible for creating instances of the ActorRef class
     * </p>
     *
     * @param client is the client part of the Spawn protocol and is responsible for communicating with the Proxy.
     * @param system ActorSystem name of the actor that this ActorRef instance should represent
     * @param name   the name of the actor that this ActorRef instance should represent
     * @return the ActorRef instance
     * @since 0.0.1
     */
    protected static ActorRef of(SpawnClient client, String system, String name) throws ActorCreationException {
        ActorOuterClass.ActorId actorId = buildActorId(system, name);
        ActorRef ref = ACTOR_REF_CACHE.getIfPresent(actorId);
        if (Objects.nonNull(ref)) {
            return ref;
        }

        ref = new ActorRef(actorId, client);
        ACTOR_REF_CACHE.put(actorId, ref);
        return ref;
    }

    /**
     * <p>This method is responsible for creating instances of the ActorRef class when Actor is a UnNamed actor.
     * </p>
     *
     * @param client is the client part of the Spawn protocol and is responsible for communicating with the Proxy.
     * @param system ActorSystem name of the actor that this ActorRef instance should represent
     * @param name   the name of the actor that this ActorRef instance should represent
     * @param parent the name of the unnamed parent actor
     * @return the ActorRef instance
     * @since 0.0.1
     */
    protected static ActorRef of(SpawnClient client, String system, String name, String parent) throws ActorCreationException {
        ActorOuterClass.ActorId actorId = buildActorId(system, name, parent);
        ActorRef ref = ACTOR_REF_CACHE.getIfPresent(actorId);
        if (Objects.nonNull(ref)) {
            return ref;
        }

        spawnActor(actorId, client);
        ref = new ActorRef(actorId, client);
        ACTOR_REF_CACHE.put(actorId, ref);
        return ref;
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

    private static void spawnActor(ActorOuterClass.ActorId actorId, SpawnClient client) throws ActorCreationException {
        Protocol.SpawnRequest req = Protocol.SpawnRequest.newBuilder()
                .addActors(actorId)
                .build();
        client.spawn(req);
    }

    /**
     * <p>This method synchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action     name of the action to be called.
     * @param outputType the class that corresponds to the expected return type
     * @return an Optional containing, or not, the response object to the Action call
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3> Optional<T> invoke(String action, Class<T> outputType) throws SpawnException {
        Optional<T> res = invokeActor(action, Empty.getDefaultInstance(), outputType, Optional.empty());
        return res.map(outputType::cast);

    }

    /**
     * <p>This method synchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action     name of the action to be called.
     * @param outputType the class that corresponds to the expected return type
     * @param opts       options that can be passed during the invocation of the Action.
     *                   Please see the {@link io.eigr.spawn.api.InvocationOpts} class for more information
     * @return an Optional containing, or not, the response object to the Action call
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3> Optional<T> invoke(String action, Class<T> outputType, InvocationOpts opts) throws SpawnException {
        Optional<T> res = invokeActor(action, Empty.getDefaultInstance(), outputType, Optional.ofNullable(opts));
        return res.map(outputType::cast);

    }

    /**
     * <p>This method synchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action     name of the action to be called.
     * @param value      the action argument object.
     * @param outputType the class that corresponds to the expected return type
     * @return an Optional containing, or not, the response object to the Action call
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<T> invoke(String action, S value, Class<T> outputType) throws SpawnException {
        Optional<T> res = invokeActor(action, value, outputType, Optional.empty());
        return res.map(outputType::cast);

    }

    /**
     * <p>This method synchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action     name of the action to be called.
     * @param value      the action argument object.
     * @param outputType the class that corresponds to the expected return type
     * @param opts       options that can be passed during the invocation of the Action.
     *                   Please see the {@link io.eigr.spawn.api.InvocationOpts} class for more information
     * @return an Optional containing, or not, the response object to the Action call
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<T> invoke(String action, S value, Class<T> outputType, InvocationOpts opts) throws SpawnException {
        Optional<T> res = invokeActor(action, value, outputType, Optional.ofNullable(opts));
        return res.map(outputType::cast);

    }

    /**
     * <p>This method asynchronously invokes an action on the actor that this ActorRef instance represents via the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action name of the action to be called.
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3> void invokeAsync(String action) throws SpawnException {
        InvocationOpts opts = InvocationOpts.builder().async(true).build();
        invokeActor(action, Empty.getDefaultInstance(), null, Optional.of(opts));
    }

    /**
     * <p>This method asynchronously invokes an action on the actor that this ActorRef instance represents via the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action name of the action to be called.
     * @param opts   options that can be passed during the invocation of the Action.
     *               Please see the {@link io.eigr.spawn.api.InvocationOpts} class for more information
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3> void invokeAsync(String action, InvocationOpts opts) throws ActorInvocationException {
        InvocationOpts mergedOpts = InvocationOpts.builder()
                .async(true)
                .delaySeconds(opts.getDelaySeconds())
                .scheduledTo(opts.getScheduledTo())
                .timeoutSeconds(opts.getTimeoutSeconds())
                .build();

        invokeActor(action, Empty.getDefaultInstance(), null, Optional.ofNullable(mergedOpts));
    }

    /**
     * <p>This method asynchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action name of the action to be called.
     * @param value  the action argument object.
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> void invokeAsync(String action, S value) throws ActorInvocationException {
        InvocationOpts opts = InvocationOpts.builder().async(true).build();
        invokeActor(action, value, null, Optional.of(opts));
    }

    /**
     * <p>This method asynchronously invokes an action on the actor that this ActorRef instance represents through the Spawn Proxy.
     * Used when it is not necessary to send parameters to the Action.
     * </p>
     *
     * @param action name of the action to be called.
     * @param value  the action argument object.
     * @param opts   options that can be passed during the invocation of the Action.
     *               Please see the {@link io.eigr.spawn.api.InvocationOpts} class for more information
     * @since 0.0.1
     */
    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> void invokeAsync(String action, S value, InvocationOpts opts) throws ActorInvocationException {
        InvocationOpts mergedOpts = InvocationOpts.builder()
                .async(true)
                .delaySeconds(opts.getDelaySeconds())
                .scheduledTo(opts.getScheduledTo())
                .timeoutSeconds(opts.getTimeoutSeconds())
                .build();

        invokeActor(action, value, null, Optional.of(mergedOpts));
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

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Optional<T> invokeActor(
            String cmd, S argument, Class<T> outputType, Optional<InvocationOpts> options) throws ActorInvocationException {
        Objects.requireNonNull(this.actorId, "ActorId cannot be null");

        Protocol.InvocationRequest.Builder invocationRequestBuilder = Protocol.InvocationRequest.newBuilder();

        Map<String, String> metadata = new HashMap<>();
        options.ifPresent(opts -> {
            invocationRequestBuilder.setAsync(opts.isAsync());
            metadata.put("request-timeout", String.valueOf(opts.getTimeout()));
            opts.getDelaySeconds().ifPresent(invocationRequestBuilder::setScheduledTo);
            // 'scheduledTo' override 'delay' if both is set.
            opts.getScheduledTo()
                    .ifPresent(scheduleTo -> invocationRequestBuilder.setScheduledTo(opts.getScheduleTimeInLong()));
        });

        final ActorOuterClass.Actor actorRef = ActorOuterClass.Actor.newBuilder()
                .setId(this.actorId)
                .build();

        Any commandArg = Any.pack(argument);

        invocationRequestBuilder
                .setSystem(ActorOuterClass.ActorSystem.newBuilder().setName(this.actorId.getSystem()).build())
                .setActor(actorRef)
                .setActionName(cmd)
                .setValue(commandArg)
                .putAllMetadata(metadata)
                .build();

        Protocol.InvocationResponse resp = this.client.invoke(invocationRequestBuilder.build());
        final Protocol.RequestStatus status = resp.getStatus();
        switch (status.getStatus()) {
            case UNKNOWN:
            case ERROR:
            case UNRECOGNIZED:
                String msg = String.format("Error when trying to invoke Actor %s. Details: %s",
                        this.getActorName(), status.getMessage());
                throw new ActorInvocationException(msg);
            case ACTOR_NOT_FOUND:
                throw new ActorInvocationException("Actor not found.");
            case OK:
                if (resp.hasValue() && Objects.nonNull(outputType)) {
                    try {
                        return Optional.of(resp.getValue().unpack(outputType));
                    } catch (Exception e) {
                        throw new ActorInvocationException("Error handling response.", e);
                    }
                }
                return Optional.empty();
        }

        return Optional.empty();
    }
}
