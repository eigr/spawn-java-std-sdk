package io.eigr.spawn.api.actors.behaviors;

import io.eigr.spawn.internal.ActorKind;

public final class UnNamedActorBehavior extends ActorBehavior {

    public UnNamedActorBehavior(ActorOption... options) {
        super(options);
    }

    @Override
    public ActorKind getActorType() {
        return ActorKind.UNNAMED;
    }
}
