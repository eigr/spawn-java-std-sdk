package io.eigr.spawn.api.actors;

import io.eigr.spawn.api.Spawn;

import java.util.Optional;

public final class ActorContext<S extends Object> {

    private Spawn spawn;

    private Optional<S> state;

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
        final StringBuilder sb = new StringBuilder("ActorContext{");
        sb.append("state=").append(state);
        sb.append('}');
        return sb.toString();
    }
}
