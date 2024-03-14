package io.eigr.spawn.internal;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;

@FunctionalInterface
public interface ActionNoArgumentFunction extends ActionEmptyFunction {

    Value handle(ActorContext context);
}
