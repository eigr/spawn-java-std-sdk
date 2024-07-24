package io.eigr.spawn.internal.transport.server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass.ActorId;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.internal.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ActorServiceHandler<B extends ActorBehavior> implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(ActorServiceHandler.class);
    private static final int CACHE_MAXIMUM_SIZE = 10_000;
    private static final int CACHE_EXPIRE_AFTER_WRITE_SECONDS = 60;
    private static final String CONTENT_TYPE = "application/octet-stream";

    private final Spawn spawn;
    private final String system;
    private final List<Entity> entities;
    private final Cache<String, B> cache;

    public ActorServiceHandler(final Spawn spawn, final List<Entity> entities) {
        this.spawn = spawn;
        this.system = spawn.getSystem();
        this.entities = entities;
        this.cache = Caffeine.newBuilder()
                .maximumSize(CACHE_MAXIMUM_SIZE)
                .expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRE_AFTER_WRITE_SECONDS))
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("Received Actor Action Request.");
        if ("POST".equals(exchange.getRequestMethod())) {
            try (OutputStream os = exchange.getResponseBody()) {
                Protocol.ActorInvocationResponse response = handleRequest(exchange);
                byte[] bytes = response.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
                exchange.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }

    private Protocol.ActorInvocationResponse handleRequest(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            Protocol.ActorInvocation actorInvocationRequest = Protocol.ActorInvocation.parseFrom(in);
            Protocol.Context context = actorInvocationRequest.getCurrentContext();

            ActorId actorId = actorInvocationRequest.getActor();
            String actor = actorId.getName();
            String system = actorId.getSystem();
            String parent = actorId.getParent();
            String commandName = actorInvocationRequest.getActionName();

            Any value = actorInvocationRequest.getValue();

            Optional<Value> maybeValueResponse = callAction(system, actor, parent, commandName, value, context);
            log.debug("Actor {} return ActorInvocationResponse for command {}. Result value: {}",
                    actor, commandName, maybeValueResponse);

            if (maybeValueResponse.isPresent()) {
                return buildResponse(maybeValueResponse.get(), actor, system);
            }
        } catch (Exception e) {
            log.error("Error during handle request.", e);
            throw new IOException("Action result is null", e);
        }

        throw new IOException("Action result is null");
    }

    private Optional<Value> callAction(String system, String actor, String parent, String commandName, Any value, Protocol.Context context) throws ActorInvocationException {
        Optional<Entity> optionalEntity = getEntityByActor(actor, parent);
        if (optionalEntity.isPresent()) {
            Entity entity = optionalEntity.get();
            try {
                ActorBehavior actorRef = getOrCreateActor(system, actor, entity);
                Entity.EntityMethod entityMethod = getEntityMethod(commandName, entity);
                ActorContext actorContext = createActorContext(context, entity);

                return invokeAction(actorRef, commandName, value, entityMethod, actorContext);
            } catch (ReflectiveOperationException | InvalidProtocolBufferException e) {
                throw new ActorInvocationException(e);
            }
        }
        return Optional.empty();
    }

    private Optional<Value> invokeAction(ActorBehavior actorRef, String commandName, Any value, Entity.EntityMethod entityMethod, ActorContext actorContext) throws ReflectiveOperationException, InvalidProtocolBufferException, ActorInvocationException {
        if (entityMethod.getArity() == 0) {
            return Optional.of((Value) actorRef.call(commandName, actorContext));
        } else {
            Class inputType = entityMethod.getInputType();
            final var unpackedValue = value.unpack(inputType);
            return Optional.of((Value) actorRef.call(commandName, actorContext, unpackedValue));
        }
    }

    private ActorBehavior getOrCreateActor(String system, String actor, Entity entity) throws ReflectiveOperationException {
        String actorKey = String.format("%s:%s", system, actor);
        ActorBehavior actorRef = cache.getIfPresent(actorKey);
        if (actorRef == null) {
            actorRef = buildInstance(entity);
            cache.put(actorKey, (B) actorRef);
        }
        return actorRef;
    }

    private Entity.EntityMethod getEntityMethod(String commandName, Entity entity) throws ActorInvocationException {
        if (entity.getActions().containsKey(commandName)) {
            return (Entity.EntityMethod) entity.getActions().get(commandName);
        } else if (entity.getTimerActions().containsKey(commandName)) {
            return (Entity.EntityMethod) entity.getTimerActions().get(commandName);
        } else {
            throw new ActorInvocationException(
                    String.format("The Actor does not have the desired action: %s", commandName));
        }
    }

    private ActorContext createActorContext(Protocol.Context context, Entity entity) throws InvalidProtocolBufferException {
        if (context.hasState()) {
            Any anyCtxState = context.getState();
            log.debug("[{}] trying to get the state of the Actor {}. Parse Any type {} from State type {}",
                    system, entity.getActorName(), anyCtxState, entity.getStateType().getSimpleName());

            Object state = anyCtxState.unpack(entity.getStateType());
            return new ActorContext(spawn, state);
        } else {
            return new ActorContext(spawn);
        }
    }

    private <B extends ActorBehavior> B buildInstance(Entity entity) throws ReflectiveOperationException {
        Constructor<?> constructor = entity.getActor().getClass().getConstructor();
        StatefulActor stActor = (StatefulActor) constructor.newInstance();
        return (B) stActor.configure(entity.getCtx());
    }

    private Optional<Entity> getEntityByActor(String actor, String parent) {
        return entities.stream()
                .filter(e -> e.getActorName().equalsIgnoreCase(actor))
                .findFirst()
                .or(() -> entities.stream()
                        .filter(e -> e.getActorName().equalsIgnoreCase(parent))
                        .findFirst());
    }

    private Protocol.ActorInvocationResponse buildResponse(Value valueResponse, String actor, String system) {
        Protocol.Context.Builder updatedContextBuilder = Protocol.Context.newBuilder();

        Optional.<GeneratedMessage>of(valueResponse.getState())
                .ifPresent(state -> updatedContextBuilder.setState(Any.pack(state)));

        Any encodedValue = Optional.<GeneratedMessage>of(valueResponse.getResponse())
                .map(value -> Any.pack(value))
                .orElse(Any.pack(Protocol.Noop.getDefaultInstance()));

        return Protocol.ActorInvocationResponse.newBuilder()
                .setActorName(actor)
                .setActorSystem(system)
                .setValue(encodedValue)
                .setWorkflow(buildWorkflow(valueResponse))
                .setUpdatedContext(updatedContextBuilder.build())
                .setCheckpoint(valueResponse.getCheckpoint())
                .build();
    }

    private Protocol.Workflow buildWorkflow(Value valueResponse) {
        Protocol.Workflow.Builder workflowBuilder = Protocol.Workflow.newBuilder();

        valueResponse.getBroadcast().ifPresent(b -> workflowBuilder.setBroadcast(b.build()));
        valueResponse.getForward().ifPresent(f -> workflowBuilder.setForward(f.build()));
        valueResponse.getPipe().ifPresent(p -> workflowBuilder.setPipe(p.build()));
        valueResponse.getEffects().ifPresent(e -> workflowBuilder.addAllEffects(getProtocolEffects(e)));

        return workflowBuilder.build();
    }

    private List<Protocol.SideEffect> getProtocolEffects(List<SideEffect<?>> effects) {
        return effects.stream()
                .map(SideEffect::build)
                .collect(Collectors.toList());
    }
}
