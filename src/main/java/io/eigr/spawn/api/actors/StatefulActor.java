package io.eigr.spawn.api.actors;

import java.lang.reflect.ParameterizedType;

public abstract class StatefulActor<S> {
    public Class<S> getStateType() {
        return (Class<S>) ((ParameterizedType)
                getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    public abstract ActorBehavior configure();
}
