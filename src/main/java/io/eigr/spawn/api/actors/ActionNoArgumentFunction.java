package io.eigr.spawn.api.actors;

@FunctionalInterface
public interface ActionNoArgumentFunction extends ActionEmptyFunction {

    Value handle(ActorContext context);
}
