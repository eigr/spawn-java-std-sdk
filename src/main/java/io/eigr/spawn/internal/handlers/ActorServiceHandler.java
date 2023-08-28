package io.eigr.spawn.internal.handlers;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass.ActorId;
import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.exceptions.ActorInvokeException;
import io.eigr.spawn.api.workflows.Broadcast;
import io.eigr.spawn.api.workflows.Forward;
import io.eigr.spawn.api.workflows.Pipe;
import io.eigr.spawn.api.workflows.SideEffect;
import io.eigr.spawn.internal.Entity;

public final class ActorServiceHandler implements HttpHandler {
    private String system;

    private List<Entity> entities;

    public ActorServiceHandler(String system, List<Entity> actors) {
        this.system = system;
        this.entities = actors;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Protocol.ActorInvocationResponse response = handleRequest(exchange);
        }

        String response = "Hi there!";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Protocol.ActorInvocationResponse handleRequest(HttpExchange exchange) throws IOException {
        try(InputStream in = exchange.getRequestBody()) {
            Protocol.ActorInvocation actorInvocationRequest = Protocol.ActorInvocation.parseFrom(in);
            Protocol.Context context = actorInvocationRequest.getCurrentContext();

            ActorId actorId = actorInvocationRequest.getActor();
            String actor = actorId.getName();
            String system = actorId.getSystem();
            String commandName = actorInvocationRequest.getActionName();

            Any value = actorInvocationRequest.getValue();

            Optional<Value> maybeValueResponse = callAction(system, actor, commandName, value, context);
            //log.info("Actor {} return ActorInvocationResponse for command {}. Result value: {}",
            //        actor, commandName, maybeValueResponse);

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
                        .build();
            }

            throw new ActorInvokeException("Action result is null");
        }
    }

    private Optional<Value> callAction(String system, String actor, String commandName, Any value, Protocol.Context context) {
        Optional<Entity> optionalEntity = getEntityByActor(actor);

        if (optionalEntity.isPresent()) {
            try {
                Entity entity = optionalEntity.get();
                final Object actorRef = null; //this.context.getBean(entity.getActorType());

                final Entity.EntityMethod entityMethod = entity.getActions().get(commandName);
                final Method actorMethod = entityMethod.getMethod();
                Class inputType = entityMethod.getInputType();

                ActorContext actorContext;
                if (context.hasState()) {
                    Object state = context.getState().unpack(entity.getStateType());
                    actorContext = new ActorContext(state);
                } else {
                    actorContext = new ActorContext();
                }

                if (inputType.isAssignableFrom(ActorContext.class)) {
                    return Optional.of((Value) actorMethod.invoke(actorRef, actorContext));
                } else {
                    final Object unpack = value.unpack(entityMethod.getInputType());
                    return Optional.of((Value) actorMethod.invoke(actorRef, unpack, actorContext));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
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
