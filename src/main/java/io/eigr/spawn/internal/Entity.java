package io.eigr.spawn.internal;

import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.ActorFactory;
import io.eigr.spawn.api.actors.annotations.*;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulPooledActor;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulUnNamedActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessNamedActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessPooledActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessUnNamedActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class Entity {
    private static final Logger log = LoggerFactory.getLogger(Entity.class);
    private String actorName;
    private Class<?> actorType;

    private Optional<Object> actorArg;

    private Optional<ActorFactory> actorFactory;

    private ActorOuterClass.Kind kind;

    private Class stateType;
    private String actorBeanName;
    private boolean stateful;

    private long deactivateTimeout;

    private long snapshotTimeout;
    private Map<String, EntityMethod> actions;

    private Map<String, EntityMethod> timerActions;

    private int minPoolSize;
    private int maxPoolSize;

    private String channel;

    public Entity(
            String actorName,
            Class<?> actorType,
            ActorOuterClass.Kind kind,
            Class stateType,
            String actorBeanName,
            boolean stateful,
            long deactivateTimeout,
            long snapshotTimeout,
            Map<String, EntityMethod> actions,
            Map<String, EntityMethod> timerActions,
            int minPoolSize,
            int maxPoolSize,
            String channel,
            Optional<Object> actorArg,
            Optional<ActorFactory> actorFactory) {
        this.actorName = actorName;
        this.actorType = actorType;
        this.kind = kind;
        this.stateType = stateType;
        this.actorBeanName = actorBeanName;
        this.stateful = stateful;
        this.deactivateTimeout = deactivateTimeout;
        this.snapshotTimeout = snapshotTimeout;
        this.actions = actions;
        this.timerActions = timerActions;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.channel = channel;
        this.actorArg = actorArg;
        this.actorFactory = actorFactory;
    }



    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Class<?> getActorType() {
        return actorType;
    }

    public ActorOuterClass.Kind getKind() {
        return kind;
    }

    public Class getStateType() {
        return stateType;
    }

    public String getActorBeanName() {
        return actorBeanName;
    }

    public boolean isStateful() {
        return stateful;
    }

    public long getDeactivateTimeout() {
        return deactivateTimeout;
    }

    public long getSnapshotTimeout() {
        return snapshotTimeout;
    }

    public Map<String, EntityMethod> getActions() {
        return actions;
    }

    public Map<String, EntityMethod> getTimerActions() {
        return timerActions;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public String getChannel(){ return channel; }

    public enum EntityMethodType {
        DIRECT, TIMER
    }

    public Optional<Object> getActorArg() {
        return actorArg;
    }

    public Optional<ActorFactory> getActorFactory() {
        return actorFactory;
    }

    public static final class EntityMethod {
        private String name;

        private EntityMethodType type;

        private int fixedPeriod;

        private Method method;
        private Class<?> inputType;
        private Class<?> outputType;

        public EntityMethod(
                String name, EntityMethodType type, int fixedPeriod, Method method, Class<?> inputType, Class<?> outputType) {
            this.name = name;
            this.type = type;
            this.fixedPeriod = fixedPeriod;
            this.method = method;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public String getName() {
            return name;
        }

        public EntityMethodType getType() {
            return type;
        }

        public int getFixedPeriod() {
            return fixedPeriod;
        }

        public Method getMethod() {
            return method;
        }

        public Class getInputType() {
            int arity = method.getParameterTypes().length;

            if (arity == 2 && Objects.isNull(inputType)) {
                for (Class<?> parameterType : method.getParameterTypes()) {
                    if (!inputType.isAssignableFrom(ActorContext.class)) {
                        return parameterType;
                    }
                }
            }
            return inputType;
        }

        public Class<?> getOutputType() {
            return outputType;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("EntityMethod{");
            sb.append("name='").append(name).append('\'');
            sb.append(", type=").append(type);
            sb.append(", fixedPeriod=").append(fixedPeriod);
            sb.append(", method=").append(method);
            sb.append(", inputType=").append(inputType);
            sb.append(", outputType=").append(outputType);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("actorName='").append(actorName).append('\'');
        sb.append(", actorType=").append(actorType);
        sb.append(", kind=").append(kind);
        sb.append(", stateType=").append(stateType);
        sb.append(", actorBeanName='").append(actorBeanName).append('\'');
        sb.append(", stateful=").append(stateful);
        sb.append(", deactivateTimeout=").append(deactivateTimeout);
        sb.append(", snapshotTimeout=").append(snapshotTimeout);
        sb.append(", actions=").append(actions);
        sb.append(", timerActions=").append(timerActions);
        sb.append(", minPoolSize=").append(minPoolSize);
        sb.append(", maxPoolSize=").append(maxPoolSize);
        sb.append(", channel=").append(channel);
        sb.append('}');
        return sb.toString();
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatefulNamedActor actor, Object arg, ActorFactory factory) {
        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.NAMED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final long snapshotTimeout = actor.snapshotTimeout();
        final boolean isStateful = true;
        final Class stateType = actor.stateType();
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                stateType,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering NamedActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatefulUnNamedActor actor, Object arg, ActorFactory factory) {
        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.UNNAMED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final long snapshotTimeout = actor.snapshotTimeout();
        final boolean isStateful = true;
        final Class stateType = actor.stateType();
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                stateType,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering UnNamedActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatefulPooledActor actor, Object arg, ActorFactory factory) {

        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.POOLED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final long snapshotTimeout = actor.snapshotTimeout();
        final boolean isStateful = true;
        final Class stateType = actor.stateType();
        final int minPoolSize = actor.minPoolSize();
        final int maxPoolSize = actor.maxPoolSize();
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                stateType,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                minPoolSize,
                maxPoolSize,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering PooledActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatelessNamedActor actor, Object arg, ActorFactory factory) {
        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.NAMED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final boolean isStateful = false;
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                null,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                0,
                actions,
                timerActions,
                0,
                0,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering NamedActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatelessUnNamedActor actor, Object arg, ActorFactory factory) {
        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.UNNAMED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final boolean isStateful = false;
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                null,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                0,
                actions,
                timerActions,
                0,
                0,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering UnNamedActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    public static Entity fromAnnotationToEntity(Class<?> entity, StatelessPooledActor actor, Object arg, ActorFactory factory) {

        String actorBeanName = entity.getSimpleName();
        String actorName;
        if ((Objects.isNull(actor.name()) || actor.name().isEmpty())) {
            actorName = actorBeanName;
        } else {
            actorName = actor.name();
        }

        final ActorKind kind = ActorKind.POOLED;
        final long deactivateTimeout = actor.deactivatedTimeout();
        final boolean isStateful = false;
        final int minPoolSize = actor.minPoolSize();
        final int maxPoolSize = actor.maxPoolSize();
        final String channel = actor.channel();

        final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
        final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

        Entity entityType = new Entity(
                actorName,
                entity,
                getKind(kind),
                null,
                actorBeanName,
                isStateful,
                deactivateTimeout,
                0,
                actions,
                timerActions,
                minPoolSize,
                maxPoolSize,
                channel,
                Optional.ofNullable(arg),
                Optional.ofNullable(factory));

        log.info("Registering PooledActor: {}", actorName);
        log.debug("Registering Entity -> {}", entityType);
        return entityType;
    }

    private static Map<String, Entity.EntityMethod> buildActions(Class<?> entity, Class<? extends Annotation> annotationType) {
        final Map<String, Entity.EntityMethod> actions = new HashMap<>();

        List<Method> methods = Arrays.stream(entity.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .collect(Collectors.toList());

        for (Method method : methods) {
            try {
                method.setAccessible(true);
                String commandName = getActionName(method, annotationType);
                Class<?> inputType = getInputType(method, annotationType);
                Class<?> outputType = getOutputType(method, annotationType);

                Entity.EntityMethod action = new Entity.EntityMethod(
                        commandName,
                        getEntityMethodType(method, annotationType),
                        getPeriod(method, annotationType),
                        method,
                        inputType,
                        outputType);

                actions.put(commandName, action);
            } catch (SecurityException e) {
                log.error("Failure on load Actor Action", e);
            }
        }
        return actions;
    }

    private static int getPeriod(Method method, Class<? extends Annotation> type) {
        int period = 0;

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            period = act.period();
        }

        return period;
    }

    private static Entity.EntityMethodType getEntityMethodType(Method method, Class<? extends Annotation> type) {
        Entity.EntityMethodType entityMethodType = null;

        if (type.isAssignableFrom(Action.class)) {
            entityMethodType = Entity.EntityMethodType.DIRECT;
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            entityMethodType = Entity.EntityMethodType.TIMER;
        }

        return entityMethodType;
    }

    private static String getActionName(Method method, Class<? extends Annotation> type) {
        String commandName = "";

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            commandName = ((!act.name().equalsIgnoreCase("")) ? act.name() : method.getName());
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            commandName = ((!act.name().equalsIgnoreCase("")) ? act.name() : method.getName());
        }

        return commandName;
    }

    private static Class<?> getInputType(Method method, Class<? extends Annotation> type) {
        Class<?> inputType = null;

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            inputType = (!act.inputType().isAssignableFrom(Action.Default.class) ? act.inputType() : method.getParameterTypes()[0]);
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            inputType = (!act.inputType().isAssignableFrom(TimerAction.Default.class) ? act.inputType() : method.getParameterTypes()[0]);
        }

        return inputType;
    }

    private static Class<?> getOutputType(Method method, Class<? extends Annotation> type) {
        Class<?> outputType = null;

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            outputType = (!act.outputType().isAssignableFrom(Action.Default.class) ? act.outputType() : method.getReturnType());
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            outputType = (!act.outputType().isAssignableFrom(TimerAction.Default.class) ? act.outputType() : method.getReturnType());
        }

        return outputType;
    }

    private static ActorOuterClass.Kind getKind(ActorKind kind) {
        switch (kind) {
            case UNNAMED:
                return ActorOuterClass.Kind.UNAMED;
            case POOLED:
                return ActorOuterClass.Kind.POOLED;
            case PROXY:
                return ActorOuterClass.Kind.PROXY;
            default:
                return ActorOuterClass.Kind.NAMED;
        }
    }
}