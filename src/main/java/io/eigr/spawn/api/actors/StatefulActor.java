package io.eigr.spawn.api.actors;

import java.lang.reflect.ParameterizedType;

public interface StatefulActor<S> extends BaseActor {
    default Class<S> getStateType(Class<S> entityClass) {
        return entityClass;
    }
    @Override
    default Boolean isStateful() {
        return true;
    }
}
