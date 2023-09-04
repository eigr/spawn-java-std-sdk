package io.eigr.spawn.api.actors.annotations.stateless;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StatelessUnNamedActor {
    String value() default "";

    String name() default "";

    long deactivatedTimeout() default 10000;

    String channel() default "";

    int minPoolSize() default 1;

    int maxPoolSize() default 0;
}
