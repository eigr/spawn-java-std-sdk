package io.eigr.spawn.api.actors.behaviors;

import io.eigr.spawn.internal.ActorKind;

public final class NamedActorBehavior extends ActorBehavior {

    public NamedActorBehavior(ActorOption... options) {
        super(options);
    }

    @Override
    public ActorKind getActorType() {
        return ActorKind.NAMED;
    }
}
