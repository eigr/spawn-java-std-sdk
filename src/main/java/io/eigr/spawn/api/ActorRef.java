package io.eigr.spawn.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessage;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;
import io.eigr.spawn.internal.transport.client.SpawnClient;

import java.util.*;

/**
 * ActorRef is responsible for representing an instance of an Actor
 *
 * @author Adriano Santos
 */
public final class ActorRef {

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
     * @param cache is actor ids cache
     * @param identity ActorIdentity name of the actor that this ActorRef instance should represent
     * @return the ActorRef instance
     * @since 0.0.1
     */
    protected static ActorRef of(SpawnClient client, Cache<ActorOuterClass.ActorId, ActorRef> cache, ActorIdentity identity) throws ActorCreationException {
        ActorOuterClass.ActorId actorId;

        if (identity.isParent()) {
            actorId = buildActorId(identity.getSystem(), identity.getName(), identity.getParent());

            //spawnActor(actorId, client);
        } else {
            actorId = buildActorId(identity.getSystem(), identity.getName());
        }

        ActorRef ref = cache.getIfPresent(actorId);
        if (Objects.nonNull(ref)) {
            return ref;
        }

        if (identity.hasLookup()) {
            spawnActor(actorId, client);
        }

        ref = new ActorRef(actorId, client);
        cache.put(actorId, ref);
        return ref;
    }

    protected static ActorOuterClass.ActorId buildActorId(String system, String name) {
        ActorOuterClass.ActorId.Builder actorIdBuilder = ActorOuterClass.ActorId.newBuilder()
                .setSystem(system)
                .setName(name);

        return actorIdBuilder.build();
    }

    protected static ActorOuterClass.ActorId buildActorId(String system, String name, String parent) {
        return ActorOuterClass.ActorId.newBuilder()
                .setSystem(system)
                .setName(name)
                .setParent(parent)
                .build();
    }

    protected static void spawnActor(ActorOuterClass.ActorId actorId, SpawnClient client) throws ActorCreationException {
        Protocol.SpawnRequest req = Protocol.SpawnRequest.newBuilder()
                .addActors(actorId)
                .build();
        client.spawn(req);
    }

    protected static void spawnAllActors(List<ActorOuterClass.ActorId> actorIds, SpawnClient client) throws ActorCreationException {
        Protocol.SpawnRequest req = Protocol.SpawnRequest.newBuilder()
                .addAllActors(actorIds)
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
    public <T extends GeneratedMessage> Optional<T> invoke(String action, Class<T> outputType) throws ActorInvocationException {
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
    public <T extends GeneratedMessage> Optional<T> invoke(String action, Class<T> outputType, InvocationOpts opts) throws ActorInvocationException {
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
    public <T extends GeneratedMessage, S extends GeneratedMessage> Optional<T> invoke(String action, S value, Class<T> outputType) throws ActorInvocationException {
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
    public <T extends GeneratedMessage, S extends GeneratedMessage> Optional<T> invoke(String action, S value, Class<T> outputType, InvocationOpts opts) throws ActorInvocationException {
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
    public <T extends GeneratedMessage> void invokeAsync(String action) throws ActorInvocationException {
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
    public <T extends GeneratedMessage> void invokeAsync(String action, InvocationOpts opts) throws ActorInvocationException {
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
    public <T extends GeneratedMessage> void invokeAsync(String action, T value) throws ActorInvocationException {
        InvocationOpts opts = InvocationOpts.builder().async(true).build();
        invokeActorAsync(action, value, Optional.of(opts));
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
    public <T extends GeneratedMessage> void invokeAsync(String action, T value, InvocationOpts opts) throws ActorInvocationException {
        InvocationOpts mergedOpts = InvocationOpts.builder()
                .async(true)
                .delaySeconds(opts.getDelaySeconds())
                .scheduledTo(opts.getScheduledTo())
                .timeoutSeconds(opts.getTimeoutSeconds())
                .build();

        invokeActorAsync(action, value, Optional.of(mergedOpts));
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

    private <T extends GeneratedMessage, S extends GeneratedMessage> Optional<T> invokeActor(
            String cmd, S argument, Class<T> outputType, Optional<InvocationOpts> options) throws ActorInvocationException {
        Objects.requireNonNull(this.actorId, "ActorId cannot be null");

        Protocol.InvocationRequest.Builder invocationRequestBuilder = Protocol.InvocationRequest.newBuilder();

        if (Objects.nonNull(this.actorId.getParent()) && !this.actorId.getParent().isEmpty()) {
            invocationRequestBuilder.setRegisterRef(this.actorId.getParent());
        }

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
                throw new ActorNotFoundException("Actor not found.");
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

    private <T extends GeneratedMessage, S extends GeneratedMessage> void invokeActorAsync(
            String cmd, S argument, Optional<InvocationOpts> options) {
        Objects.requireNonNull(this.actorId, "ActorId cannot be null");

        Protocol.InvocationRequest.Builder invocationRequestBuilder = Protocol.InvocationRequest.newBuilder();

        if (Objects.nonNull(this.actorId.getParent()) && !this.actorId.getParent().isEmpty()) {
            invocationRequestBuilder.setRegisterRef(this.actorId.getParent());
        }

        Map<String, String> metadata = new HashMap<>();
        options.ifPresent(opts -> {
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
                .setAsync(true)
                .setSystem(ActorOuterClass.ActorSystem.newBuilder().setName(this.actorId.getSystem()).build())
                .setActor(actorRef)
                .setActionName(cmd)
                .setValue(commandArg)
                .putAllMetadata(metadata)
                .build();

        this.client.invokeAsync(invocationRequestBuilder.build());
    }
}
