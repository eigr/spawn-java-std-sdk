package io.eigr.spawn.api.actors;

import com.google.protobuf.MessageOrBuilder;

@FunctionalInterface
public interface ActionArgumentFunction<A extends MessageOrBuilder> extends ActionEmptyFunction {

    Value handle(ActorContext context, A argument);
}
