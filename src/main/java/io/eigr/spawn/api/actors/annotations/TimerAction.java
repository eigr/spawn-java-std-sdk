package io.eigr.spawn.api.actors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SpawnAnnotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimerAction {

    /**
     * The name of the action to handle.
     *
     * <p>If not specified, the name of the method will be used as the command name.
     *
     * @return The action name.
     */
    String name() default "";

    int period() default 0;

    /**
     * * The input type.
     *
     * The type class of input method parameter. Generally, this will be determined by looking at the parameter of the input type
     * handler method, however if the event doesn't need to be passed to the method (for example,
     * perhaps it contains no data), then this can be used to indicate which event this handler
     * handles.
     */
    Class<?> inputType() default Default.class;

    Class<?> outputType() default Default.class;

    class Default {}
}