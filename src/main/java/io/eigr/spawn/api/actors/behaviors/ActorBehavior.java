package io.eigr.spawn.api.actors.behaviors;

import com.google.protobuf.GeneratedMessage;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;
import io.eigr.spawn.internal.*;

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
        Optional.ofNullable(options).map(Stream::of).orElseGet(Stream::empty).forEach(option -> option.accept(this));
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

    public static ActorOption init(ActionNoArgumentFunction action) {
        return instance -> instance.actions.put("Init", new ActionEnvelope(action, new ActionConfiguration(ActionKind.NORMAL_DISPATCH)));
    }

    public static ActorOption action(String name, ActionNoArgumentFunction action) {
        return instance -> instance.actions.put(name, new ActionEnvelope(action, new ActionConfiguration(ActionKind.NORMAL_DISPATCH)));
    }

    public static <T extends GeneratedMessage> ActorOption action(String name, ActionArgumentFunction<T> action) {
        return instance -> instance.actions.put(name, new ActionEnvelope(action, new ActionConfiguration(ActionKind.NORMAL_DISPATCH)));
    }

    public static ActorOption timerAction(String name, int timer, ActionNoArgumentFunction action) {
        return instance -> instance.actions.put(name, new ActionEnvelope(action, new ActionConfiguration(ActionKind.TIMER_DISPATCH, timer)));
    }

    public static <T extends GeneratedMessage> ActorOption timerAction(String name, int timer, ActionArgumentFunction<T> action) {
        return instance -> instance.actions.put(name, new ActionEnvelope(action, new ActionConfiguration(ActionKind.TIMER_DISPATCH, timer)));
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

    public <A extends GeneratedMessage> Value call(String action, ActorContext context) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionNoArgumentFunction) this.actions.get(action).getFunction()).handle(context);
        }

        throw new ActorNotFoundException(String.format("Action [%s] not found for Actor [%s]", action, name));
    }

    public <A extends GeneratedMessage> Value call(String action, ActorContext context, A argument) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionArgumentFunction<A>) this.actions.get(action).getFunction()).handle(context, argument);
        }

        throw new ActorNotFoundException(String.format("Action [%s] not found for Actor [%s]", action, name));
    }

    public interface ActorOption extends Consumer<ActorBehavior> {
    }

}
