package io.eigr.spawn.internal;

import com.google.protobuf.MessageOrBuilder;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;

@FunctionalInterface
public interface ActionArgumentFunction<A extends MessageOrBuilder> extends ActionEmptyFunction {

    Value handle(ActorContext context, A argument);
}
