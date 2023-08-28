package io.eigr.spawn.api.annotations;

import com.google.protobuf.GeneratedMessageV3;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedActor {
    String value() default "";

    //@AliasFor("value")
    String name() default "";

    boolean stateful() default true;

    Class<? extends GeneratedMessageV3> stateType();

    long deactivatedTimeout() default 60000;

    long snapshotTimeout() default 50000;

    String channel() default "";

    int minPoolSize() default 1;

    int maxPoolSize() default 0;
}
