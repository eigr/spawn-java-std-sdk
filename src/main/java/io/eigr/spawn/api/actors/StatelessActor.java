package io.eigr.spawn.api.actors;

public abstract class StatelessActor extends BaseActor {
    @Override
    public Boolean isStateful() {
        return false;
    }
}
