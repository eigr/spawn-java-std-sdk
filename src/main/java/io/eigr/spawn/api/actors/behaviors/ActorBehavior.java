package io.eigr.spawn.api.actors.behaviors;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.internal.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class ActorBehavior {
    Class<? extends GeneratedMessage> stateType;
    private String name;

    private String channel;
    private long deactivatedTimeout = 60000;
    private long snapshotTimeout = 50000;
    private final Map<String, ActionEnvelope> actions = new HashMap<>();

    public ActorBehavior(ActorOption... options) {
        Optional.ofNullable(options)
                .stream()
                .flatMap(Stream::of)
                .forEach(option -> option.accept(this));
    }

    public static ActorOption name(String actorName) {
        return instance -> instance.name = actorName;
    }

    public static ActorOption channel(String channel) {
        return instance -> instance.channel = channel;
    }

    public static ActorOption deactivated(long timeout) {
        return instance -> instance.deactivatedTimeout = timeout;
    }

    public static ActorOption snapshot(long timeout) {
        return instance -> instance.snapshotTimeout = timeout;
    }

    public static ActorOption init(ActionNoBindings action) {
        return instance -> instance.actions.put(
                "Init",
                new ActionEnvelope(
                        action,
                        new ActionConfiguration(ActionKind.NORMAL_DISPATCH, 0, null, null)));
    }

    public static ActorOption action(String name, ActionNoBindings action) {
        final Class<?> outputType = Value.class;
        return instance -> instance.actions.put(
                name,
                new ActionEnvelope(
                        action,
                        new ActionConfiguration(ActionKind.NORMAL_DISPATCH, 0, null, outputType)));
    }

    public static <T extends Message> ActorOption action(String name, ActionBindings<T> action) {
        final Class<?> inputType = action.getArgumentType();
        final Class<?> outputType = Value.class;

        return instance -> instance.actions.put(
                name,
                new ActionEnvelope(
                        action,
                        new ActionConfiguration(ActionKind.NORMAL_DISPATCH, 1, inputType, outputType)));
    }

    public static ActorOption timerAction(String name, int timer, ActionNoBindings action) {
        final Class<?> outputType = Value.class;

        return instance -> instance.actions.put(
                name, new ActionEnvelope(
                        action,
                        new ActionConfiguration(ActionKind.TIMER_DISPATCH, timer, 0, null, outputType)));
    }

    public static <T extends GeneratedMessage> ActorOption timerAction(String name, int timer, ActionBindings<T> action) {
        final Class<?> inputType = action.getArgumentType();
        final Class<?> outputType = Value.class;

        return instance -> instance.actions.put(
                name,
                new ActionEnvelope(
                        action,
                        new ActionConfiguration(ActionKind.TIMER_DISPATCH, timer, 1, inputType, outputType)));
    }

    public abstract ActorKind getActorType();

    public String getName() {
        return name;
    }

    public String getChannel() {
        return channel;
    }

    public long getDeactivatedTimeout() {
        return deactivatedTimeout;
    }

    public long getSnapshotTimeout() {
        return snapshotTimeout;
    }

    public Class<? extends GeneratedMessage> getStateType() {
        return this.stateType;
    }

    public void setStateType(Class<? extends GeneratedMessage> stateType) {
        this.stateType = stateType;
    }

    public Map<String, ActionEnvelope> getActions() {
        return actions;
    }

    public Value call(String action, ActorContext context) throws ActorInvocationException {
        if (this.actions.containsKey(action)) {
            return ((ActionNoBindings) this.actions.get(action).getFunction()).handle(context);
        }

        throw new ActorInvocationException(String.format("Action [%s] not found for Actor [%s]", action, name));
    }

    public <A extends Message> Value call(String action, ActorContext context, A argument) throws ActorInvocationException {
        if (this.actions.containsKey(action)) {
            return ((ActionBindings<A>) this.actions.get(action).getFunction()).handle(context, argument);
        }

        throw new ActorInvocationException(String.format("Action [%s] not found for Actor [%s]", action, name));
    }

    public interface ActorOption extends Consumer<ActorBehavior> {
    }
}
