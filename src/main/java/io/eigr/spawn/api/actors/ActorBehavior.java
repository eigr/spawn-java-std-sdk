package io.eigr.spawn.api.actors;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.MessageOrBuilder;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;
import io.eigr.spawn.internal.ActionArgumentFunction;
import io.eigr.spawn.internal.ActionEmptyFunction;
import io.eigr.spawn.internal.ActionNoArgumentFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ActorBehavior {
    public interface ActorOption extends Consumer<ActorBehavior> {}

    private String name;

    private String channel;

    Class<? extends GeneratedMessageV3> stateType;

    private long deactivatedTimeout = 60000;

    private long snapshotTimeout = 50000;

    private Map<String, ActionEmptyFunction> actions = new HashMap<>();

    public ActorBehavior(ActorOption... options){
        Optional.ofNullable(options)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .forEach(option -> option.accept(this));
    }

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

    public Class<? extends GeneratedMessageV3> getStateType() {
        return this.stateType;
    }

    public void setStateType(Class<? extends GeneratedMessageV3> stateType) {
        this.stateType = stateType;
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

    public static ActorOption action(String name, ActionNoArgumentFunction action) {
        return instance -> instance.actions.put(name, action);
    }

    public static <T extends MessageOrBuilder> ActorOption action(String name, ActionArgumentFunction<T> action) {
        return instance -> instance.actions.put(name, action);
    }

    public static ActorOption init(ActionNoArgumentFunction action) {
        return instance -> instance.actions.put("Init", action);
    }

    public <A extends MessageOrBuilder> Value call(String action, ActorContext context) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionNoArgumentFunction) this.actions.get(action)).handle(context);
        }

        throw new ActorNotFoundException(
                String.format("Action [%s] not found for Actor [%s]", action, name));
    }

    public <A extends MessageOrBuilder> Value call(String action, ActorContext context, A argument) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionArgumentFunction<A>) this.actions.get(action)).handle(context, argument);
        }

        throw new ActorNotFoundException(
                String.format("Action [%s] not found for Actor [%s]", action, name));
    }

}
