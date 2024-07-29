package io.eigr.spawn.internal;

import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.actors.BaseActor;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.StatelessActor;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.api.actors.behaviors.UnNamedActorBehavior;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Entity<A extends BaseActor, B extends ActorBehavior> {
    private static final Logger log = LoggerFactory.getLogger(Entity.class);

    private A actor;

    private B behavior;

    private BehaviorCtx ctx;
    private String actorName;
    private Class<?> actorType;

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
            BehaviorCtx ctx,
            A actor,
            B behavior,
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
            String channel) {
        this.ctx = ctx;
        this.actor = actor;
        this.behavior = behavior;
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
    }

    public static Entity fromStatelessActorToEntity(BehaviorCtx ctx, Class<?> actor) throws ActorCreationException {
        try {
            Constructor<?> constructor = actor.getConstructor();
            StatelessActor stActor = (StatelessActor) constructor.newInstance();
            ActorBehavior behavior = stActor.configure(ctx);

            if (behavior.getClass().isAssignableFrom(NamedActorBehavior.class)) {
                Entity entity = buildNamedActor(null, stActor, (NamedActorBehavior) behavior, ctx);
                return entity;
            }

            if (behavior.getClass().isAssignableFrom(UnNamedActorBehavior.class)) {
                return buildUnNamedActor(null, stActor, (UnNamedActorBehavior) behavior, ctx);
            }

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new ActorCreationException();
        }

        throw new ActorCreationException();
    }

    public static Entity fromStatefulActorToEntity(BehaviorCtx ctx, Class<?> actor) throws ActorCreationException {
        try {
            Constructor<?> constructor = actor.getConstructor();
            StatefulActor stActor = (StatefulActor) constructor.newInstance();
            Class<?> stateType = stActor.getStateType();
            ActorBehavior behavior = stActor.configure(ctx);

            if (behavior.getClass().isAssignableFrom(NamedActorBehavior.class)) {
                Entity entity = buildNamedActor(stateType, stActor, (NamedActorBehavior) behavior, ctx);
                return entity;
            }

            if (behavior.getClass().isAssignableFrom(UnNamedActorBehavior.class)) {
                return buildUnNamedActor(stateType, stActor, (UnNamedActorBehavior) behavior, ctx);
            }

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new ActorCreationException();
        }

        throw new ActorCreationException();
    }

    private static Map<String, Entity.EntityMethod> getActions(Map<String, ActionEnvelope> actions) {
        return actions
                .entrySet()
                .stream().filter(entry -> entry.getValue().getConfig().getKind().equals(ActionKind.NORMAL_DISPATCH))
                .map(entry -> {
                    final String actionName = entry.getKey();
                    final ActionEnvelope envelope = entry.getValue();
                    final ActionConfiguration config = envelope.getConfig();

                    return new AbstractMap.SimpleEntry<>(
                            actionName,
                            new Entity.EntityMethod(
                                    actionName,
                                    EntityMethodType.DIRECT,
                                    config.getArity(),
                                    0,
                                    config.getInputType(),
                                    config.getOutputType()));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, Entity.EntityMethod> getTimerActions(Map<String, ActionEnvelope> actions) {
        return actions
                .entrySet()
                .stream().filter(entry -> entry.getValue().getConfig().getKind().equals(ActionKind.TIMER_DISPATCH))
                .map(entry -> {
                    final String actionName = entry.getKey();
                    final ActionEnvelope envelope = entry.getValue();
                    final ActionConfiguration config = envelope.getConfig();
                    final int arity = config.getArity();
                    final int timer = config.getTimer();

                    return new AbstractMap.SimpleEntry<>(
                            actionName,
                            new Entity.EntityMethod(
                                    actionName,
                                    EntityMethodType.DIRECT,
                                    arity,
                                    timer,
                                    config.getInputType(),
                                    config.getOutputType()));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Entity buildNamedActor(Class<?> stateType, StatefulActor actor, NamedActorBehavior behavior, BehaviorCtx ctx) {
        String actorName = behavior.getName() ;
        if (Objects.isNull(actorName) || behavior.getName().isBlank()) {
            actorName = actor.getClass().getSimpleName();
        }

        final ActorKind kind = behavior.getActorType();
        final String channel = behavior.getChannel();
        final long deactivateTimeout = behavior.getDeactivatedTimeout();
        final long snapshotTimeout = behavior.getSnapshotTimeout();
        final Map<String, ActionEnvelope> envelopeActions = behavior.getActions();
        final Map<String, Entity.EntityMethod> actions = getActions(envelopeActions);
        final Map<String, Entity.EntityMethod> timerActions = getTimerActions(envelopeActions);

        Entity entityType = new Entity(
                ctx,
                actor,
                behavior,
                actorName,
                actor.getClass(),
                getKind(kind),
                stateType,
                actorName,
                actor.isStateful(),
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel);

        return entityType;
    }

    private static Entity buildNamedActor(Class<?> stateType, StatelessActor actor, NamedActorBehavior behavior, BehaviorCtx ctx) {
        String actorName = behavior.getName() ;
        if (Objects.isNull(actorName) || behavior.getName().isBlank()) {
            actorName = actor.getClass().getSimpleName();
        }

        final ActorKind kind = behavior.getActorType();
        final String channel = behavior.getChannel();
        final long deactivateTimeout = behavior.getDeactivatedTimeout();
        final long snapshotTimeout = behavior.getSnapshotTimeout();
        final Map<String, ActionEnvelope> envelopeActions = behavior.getActions();
        final Map<String, Entity.EntityMethod> actions = getActions(envelopeActions);
        final Map<String, Entity.EntityMethod> timerActions = getTimerActions(envelopeActions);

        Entity entityType = new Entity(
                ctx, actor, behavior, actorName,
                actor.getClass(),
                getKind(kind),
                stateType,
                actorName,
                actor.isStateful(),
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel);

        return entityType;
    }

    private static Entity buildUnNamedActor(Class<?> stateType, StatefulActor actor, UnNamedActorBehavior behavior, BehaviorCtx ctx) {
        String actorName = behavior.getName() ;
        if (Objects.isNull(actorName) || behavior.getName().isBlank()) {
            actorName = actor.getClass().getSimpleName();
        }

        final ActorKind kind = behavior.getActorType();
        final String channel = behavior.getChannel();
        long deactivateTimeout = behavior.getDeactivatedTimeout();
        long snapshotTimeout = behavior.getSnapshotTimeout();
        final Map<String, ActionEnvelope> envelopeActions = behavior.getActions();
        final Map<String, Entity.EntityMethod> actions = getActions(envelopeActions);
        final Map<String, Entity.EntityMethod> timerActions = getTimerActions(envelopeActions);

        Entity entityType = new Entity(
                ctx,
                actor,
                behavior,
                actorName,
                actor.getClass(),
                getKind(kind),
                stateType,
                actorName,
                actor.isStateful(),
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel);

        return entityType;
    }

    private static Entity buildUnNamedActor(Class<?> stateType, StatelessActor actor, UnNamedActorBehavior behavior, BehaviorCtx ctx) {
        String actorName = behavior.getName() ;
        if (Objects.isNull(actorName) || behavior.getName().isBlank()) {
            actorName = actor.getClass().getSimpleName();
        }

        final ActorKind kind = behavior.getActorType();
        final String channel = behavior.getChannel();
        long deactivateTimeout = behavior.getDeactivatedTimeout();
        long snapshotTimeout = behavior.getSnapshotTimeout();
        final Map<String, ActionEnvelope> envelopeActions = behavior.getActions();
        final Map<String, Entity.EntityMethod> actions = getActions(envelopeActions);
        final Map<String, Entity.EntityMethod> timerActions = getTimerActions(envelopeActions);

        Entity entityType = new Entity(
                ctx,
                actor,
                behavior,
                actorName,
                actor.getClass(),
                getKind(kind),
                stateType,
                actorName,
                actor.isStateful(),
                deactivateTimeout,
                snapshotTimeout,
                actions,
                timerActions,
                0,
                0,
                channel);

        return entityType;
    }

    private static ActorOuterClass.Kind getKind(ActorKind kind) {
        switch (kind) {
            case UNNAMED:
                return ActorOuterClass.Kind.UNNAMED;
            case POOLED:
                return ActorOuterClass.Kind.POOLED;
            case PROXY:
                return ActorOuterClass.Kind.PROXY;
            default:
                return ActorOuterClass.Kind.NAMED;
        }
    }

    public <A extends BaseActor> A getActor() {
        return (A) this.actor;
    }

    public <B extends ActorBehavior> B getBehavior() {
        return (B) this.behavior;
    }

    public BehaviorCtx getCtx() {
        return this.ctx;
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

    public String getChannel() {
        return channel;
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

    public enum EntityMethodType {
        DIRECT, TIMER
    }

    public static final class EntityMethod {
        private String name;

        private EntityMethodType type;

        private int arity;

        private int fixedPeriod;

        private Class<?> inputType;
        private Class<?> outputType;

        public EntityMethod(
                String name, EntityMethodType type, int arity, int fixedPeriod, Class<?> inputType, Class<?> outputType) {
            this.name = name;
            this.type = type;
            this.arity = arity;
            this.fixedPeriod = fixedPeriod;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public String getName() {
            return name;
        }

        public EntityMethodType getType() {
            return type;
        }

        public int getArity() {
            return arity;
        }

        public int getFixedPeriod() {
            return fixedPeriod;
        }

        public Class<?> getInputType() {
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
            sb.append(", arity=").append(arity);
            sb.append(", fixedPeriod=").append(fixedPeriod);
            sb.append(", inputType=").append(inputType);
            sb.append(", outputType=").append(outputType);
            sb.append('}');
            return sb.toString();
        }
    }
}