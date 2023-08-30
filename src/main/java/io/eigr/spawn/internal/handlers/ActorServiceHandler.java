package io.eigr.spawn.internal.handlers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass.ActorId;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.ActorFactory;
import io.eigr.spawn.api.exceptions.ActorInvokeException;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.internal.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ActorServiceHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(ActorServiceHandler.class);

    private static final int CACHE_MAXIMUM_SIZE = 10_000;
    private static final int CACHE_EXPIRE_AFTER_WRITE_SECONDS = 60;

    private final Spawn spawn;
    private final String system;

    private final List<Entity> entities;

    private final Cache<String, Object> cache;


    public ActorServiceHandler(final Spawn spawn, final List<Entity> actors) {
        this.spawn = spawn;
        this.system = spawn.getSystem();
        this.entities = actors;
        this.cache = Caffeine.newBuilder()
                .maximumSize(CACHE_MAXIMUM_SIZE)
                .expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRE_AFTER_WRITE_SECONDS))
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("Received Actor Action Request.");

        if ("POST".equals(exchange.getRequestMethod())) {
            Protocol.ActorInvocationResponse response = handleRequest(exchange);
            try(OutputStream os = exchange.getResponseBody()) {
                byte[] bytes = response.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.  sendResponseHeaders(200, bytes.length);
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
            String commandName = actorInvocationRequest.getActionName();

            Any value = actorInvocationRequest.getValue();

            Optional<Value> maybeValueResponse = callAction(system, actor, commandName, value, context);
            log.info("Actor {} return ActorInvocationResponse for command {}. Result value: {}",
                    actor, commandName, maybeValueResponse);

            if (maybeValueResponse.isPresent()) {
                Value valueResponse = maybeValueResponse.get();
                Any encodedState = Any.pack(valueResponse.getState());
                Any encodedValue = Any.pack(valueResponse.getResponse());

                Protocol.Context updatedContext = Protocol.Context.newBuilder()
                        .setState(encodedState)
                        .build();

                return Protocol.ActorInvocationResponse.newBuilder()
                        .setActorName(actor)
                        .setActorSystem(system)
                        .setValue(encodedValue)
                        .setWorkflow(buildWorkflow(valueResponse))
                        .setUpdatedContext(updatedContext)
                        .setCheckpoint(valueResponse.getCheckpoint())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error during handle request. Error: {}", e);
        }

        throw new ActorInvokeException("Action result is null");
    }

    private Optional<Value> callAction(String system, String actor, String commandName, Any value, Protocol.Context context) {
        Optional<Entity> optionalEntity = getEntityByActor(actor);
        if (optionalEntity.isPresent()) {
            Entity entity = optionalEntity.get();

            try {
                String actorKey = String.format("%s:%s", system, actor);
                Object actorRef = this.cache.getIfPresent(actorKey);
                if (Objects.isNull(actorRef)) {
                    actorRef = buildInstance(entity);
                    this.cache.put(actorKey, actorRef);
                }

                Entity.EntityMethod entityMethod;

                if (entity.getActions().containsKey(commandName)) {
                    entityMethod = entity.getActions().get(commandName);
                } else if (entity.getTimerActions().containsKey(commandName)) {
                    entityMethod = entity.getTimerActions().get(commandName);
                } else {
                    throw new ActorInvokeException(String.format("The Actor does not have the desired action: %s", commandName));
                }

                final Method actorMethod = entityMethod.getMethod();
                Class inputType = entityMethod.getInputType();
                log.debug("Action input type is: {}. Deserialize with value {}", inputType, value.getTypeUrl());

                ActorContext actorContext;
                if (context.hasState()) {
                    Object state = context.getState().unpack(entity.getStateType());
                    actorContext = new ActorContext(this.spawn, state);
                } else {
                    actorContext = new ActorContext(this.spawn);
                }

                if (inputType.isAssignableFrom(ActorContext.class)) {
                    return Optional.of((Value) actorMethod.invoke(actorRef, actorContext));
                } else {
                    final Object unpack = value.unpack(inputType);
                    return Optional.of((Value) actorMethod.invoke(actorRef, unpack, actorContext));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        return Optional.empty();
    }

    private Object buildInstance(Entity entity) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return buildInstanceByArg(entity);
    }

    private Object buildInstanceByArg(Entity entity) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (entity.getActorFactory().isPresent() && entity.getActorArg().isPresent()) {
            ActorFactory factory = entity.getActorFactory().get();
            Object arg = entity.getActorArg().get();
            return factory.newInstance(arg);
        }

        Class<?> klass = entity.getActorType();
        Constructor<?> constructor = klass.getConstructor();
        return constructor.newInstance();
    }

    private Optional<Entity> getEntityByActor(String actor) {
        return this.entities.stream()
                .filter(e -> e.getActorName().equalsIgnoreCase(actor))
                .findFirst();
    }

    private Protocol.Workflow buildWorkflow(Value valueResponse) {
        Protocol.Workflow.Builder workflowBuilder = Protocol.Workflow.newBuilder();

        if (valueResponse.getBroadcast().isPresent()) {
            Protocol.Broadcast b = ((Broadcast) valueResponse.getBroadcast().get()).build();
            workflowBuilder.setBroadcast(b);
        }

        if (valueResponse.getForward().isPresent()) {
            Protocol.Forward f = ((Forward) valueResponse.getForward().get()).build();
            workflowBuilder.setForward(f);
        }

        if (valueResponse.getPipe().isPresent()) {
            Protocol.Pipe p = ((Pipe) valueResponse.getPipe().get()).build();
            workflowBuilder.setPipe(p);
        }

        if (valueResponse.getEffects().isPresent()) {
            List<SideEffect> efs = ((List<SideEffect>) valueResponse.getEffects().get());
            workflowBuilder.addAllEffects(getProtocolEffects(efs));
        }

        return workflowBuilder.build();
    }

    private List<Protocol.SideEffect> getProtocolEffects(List<SideEffect> effects) {
        return effects.stream()
                .map(SideEffect::build)
                .collect(Collectors.toList());
    }

}
