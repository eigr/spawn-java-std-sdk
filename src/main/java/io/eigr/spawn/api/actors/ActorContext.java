package io.eigr.spawn.api.actors;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.extensions.DependencyInjector;

import java.util.Optional;
import java.util.StringJoiner;

public final class ActorContext<S extends Object> {
    private Spawn spawn;
    private Optional<S> state;

    private DependencyInjector injector;

    public ActorContext(Spawn spawn){
        this.spawn = spawn;
        this.state = Optional.empty();
    }

    public ActorContext(Spawn spawn, S state) {
        this.spawn = spawn;
        this.state = Optional.of(state);
    }

    public Spawn getSpawnSystem()  {
        return spawn;
    }

    public Optional<S> getState()  {
        return state;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", ActorContext.class.getSimpleName() + "[", "]")
                .add("spawn=" + spawn)
                .add("state=" + state)
                .toString();
    }
}
