package io.eigr.spawn.api.actors;

public interface StatelessActor extends BaseActor {
    @Override
    default Boolean isStateful() {
        return false;
    }
}
