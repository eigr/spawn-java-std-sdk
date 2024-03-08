package io.eigr.spawn.api.actors;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.MessageOrBuilder;
import io.eigr.spawn.api.exceptions.ActorNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ActorBehavior {
    public interface ActorOption extends Consumer<ActorBehavior> {}

    private String name;
    private String system;

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

    public Class<? extends GeneratedMessageV3> getStateType() {
        return this.stateType;
    }

    public void setStateType(Class<? extends GeneratedMessageV3> stateType) {
        this.stateType = stateType;
    }

    public static ActorOption name(String actorName) {
        return model -> model.name = actorName;
    }

    public static ActorOption system(String actorSystem) {
        return model -> model.system = actorSystem;
    }

    public static ActorOption deactivated(long timeout) {
        return model -> model.deactivatedTimeout = timeout;
    }

    public static ActorOption snapshot(long timeout) {
        return model -> model.snapshotTimeout = timeout;
    }

    public static ActorOption action(String name, ActionNoArgumentFunction action) {
        return model -> model.actions.put(name, action);
    }

    public static <T extends MessageOrBuilder> ActorOption action(String name, ActionArgumentFunction<T> action) {
        return model -> model.actions.put(name, action);
    }

    public <A extends MessageOrBuilder> Value call(String action, ActorContext context) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionNoArgumentFunction) this.actions.get(action)).handle(context);
        }

        throw new ActorNotFoundException(
                String.format("Action [%s] not found for Actor [%s::%s]", action, system, name));
    }

    public <A extends MessageOrBuilder> Value call(String action, ActorContext context, A argument) throws ActorNotFoundException {
        if (this.actions.containsKey(action)) {
            ((ActionArgumentFunction<A>) this.actions.get(action)).handle(context, argument);
        }

        throw new ActorNotFoundException(
                String.format("Action [%s] not found for Actor [%s::%s]", action, system, name));
    }

}
