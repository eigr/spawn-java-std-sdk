package io.eigr.spawn.api.actors;

import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;

public abstract class BaseActor {

    public abstract ActorBehavior configure(BehaviorCtx ctx);

    public abstract Boolean isStateful();


}
