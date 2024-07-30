package io.eigr.spawn.api.actors;

import java.lang.reflect.ParameterizedType;

public interface StatefulActor<S> extends BaseActor {
    default Class<S> getStateType() {
        return (Class<S>) ((ParameterizedType)
                getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }
    @Override
    default Boolean isStateful() {
        return true;
    }
}
