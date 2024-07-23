package io.eigr.spawn.internal;

import com.google.protobuf.GeneratedMessageV3;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;

@FunctionalInterface
public interface ActionArgumentFunction<A extends GeneratedMessageV3> extends ActionEmptyFunction {

    Value  handle(ActorContext context, A argument);
}
