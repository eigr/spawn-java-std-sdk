package io.eigr.spawn.api.actors;

import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;

public interface BaseActor {

    ActorBehavior configure(BehaviorCtx ctx);

    Boolean isStateful();

}
