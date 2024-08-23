package io.eigr.spawn.api.actors;

import com.google.protobuf.Message;
import io.eigr.spawn.internal.ActionEmptyFunction;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@FunctionalInterface
public interface ActionBindings<A extends Message> extends ActionEmptyFunction {

    Value handle(ActorContext context, A argument);

    default Class<A> getArgumentType() {
        Type type = ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        if (type instanceof Class<?>) {
            return (Class<A>) type;
        } else {
            throw new IllegalStateException("Unable to determine generic type A.");
        }
    }

    static <A extends Message> ActionBindings<A> of(Class<A> type, ActionBindings<A> function) {
        return new ActionBindings<A>() {
            @Override
            public Value handle(ActorContext context, A argument) {
                return function.handle(context, argument);
            }

            @Override
            public Class<A> getArgumentType() {
                return type;
            }
        };
    }
}
