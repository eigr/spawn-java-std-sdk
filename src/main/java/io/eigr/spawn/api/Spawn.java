package io.eigr.spawn.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.net.httpserver.HttpServer;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.actors.ActorFactory;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulPooledActor;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulUnNamedActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessNamedActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessPooledActor;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessUnNamedActor;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorRegistrationException;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.api.exceptions.SpawnFailureException;
import io.eigr.spawn.internal.Entity;
import io.eigr.spawn.internal.transport.client.OkHttpSpawnClient;
import io.eigr.spawn.internal.transport.client.SpawnClient;
import io.eigr.spawn.internal.transport.server.ActorServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spawn SDK Entrypoint
 */
public final class Spawn {
    private static final Logger log = LoggerFactory.getLogger(Spawn.class);
    private static final String HTTP_ACTORS_ACTIONS_URI = "/api/v1/actors/actions";
    private static final int CACHE_MAXIMUM_SIZE = 1_000;
    private static final int CACHE_EXPIRE_AFTER_WRITE_SECONDS = 60;

    private final Cache<ActorOuterClass.ActorId, ActorRef> actorIdCache;
    private final SpawnClient client;

    private final int port;
    private final String proxyHost;
    private final int proxyPort;
    private final String system;
    private final List<Entity> entities;
    private final String host;
    private final Executor executor;
    private final int terminationGracePeriodSeconds;

    private Spawn(SpawnSystem builder) {
        this.system = builder.system;
        this.entities = builder.entities;
        this.port = builder.transportOpts.getPort();
        this.host = builder.transportOpts.getHost();
        this.proxyHost = builder.transportOpts.getProxyHost();
        this.proxyPort = builder.transportOpts.getProxyPort();
        this.actorIdCache = builder.actorIdCache;
        this.client = builder.client;
        this.terminationGracePeriodSeconds = builder.terminationGracePeriodSeconds;
        this.executor = builder.transportOpts.getExecutor();
    }

    public int getPort() {
        return port;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getSystem() {
        return system;
    }

    public int getTerminationGracePeriodSeconds() {
        return terminationGracePeriodSeconds;
    }

    /**
     * <p>This method is responsible for creating instances of the ActorRef class when Actor is a UnNamed actor.
     * See more about ActorRef in {@link io.eigr.spawn.api.InvocationOpts} class
     * </p>
     *
     * @param identity the name of the actor that this ActorRef instance should represent
     * @return the ActorRef instance
     * @throws {@link io.eigr.spawn.api.exceptions.ActorCreationException}
     * @since 0.0.1
     */
    public ActorRef createActorRef(ActorIdentity identity) throws ActorCreationException {
        return ActorRef.of(this.client, this.actorIdCache, identity);
    }

    /**
     * <p>This method is responsible for creating instances of the ActorRef in batch.
     * See more about ActorRef in {@link io.eigr.spawn.api.InvocationOpts} class
     * </p>
     *
     * @param identities the name of the actor that this ActorRef instance should represent
     * @return stream of the ActorRef instances
     * @throws {@link io.eigr.spawn.api.exceptions.ActorCreationException}
     * @since 0.8.0
     */
    public Stream<ActorRef> createMultiActorRefs(List<ActorIdentity> identities) throws ActorCreationException {
        List<ActorOuterClass.ActorId> ids = identities.stream().map(identity -> {
            if (identity.isParent()) {
                return ActorRef.buildActorId(identity.getSystem(), identity.getName(), identity.getParent());
            }

            return ActorRef.buildActorId(identity.getSystem(), identity.getName());
        }).collect(Collectors.toList());

        ActorRef.spawnAllActors(ids, this.client);

        return identities.stream().map(identity -> {
            try {
                return identity.isParent() ? ActorRef.of(this.client, this.actorIdCache, identity, false) : ActorRef.of(this.client, this.actorIdCache, identity);
            } catch (ActorCreationException e) {
                throw new SpawnFailureException(e);
            }
        });
    }

    /**
     * <p>This method Starts communication with the Spawn proxy.
     * </p>
     *
     * @since 0.0.1
     */
    public void start() throws SpawnException {
        startServer();
        registerActorSystem();
    }

    private void startServer() throws SpawnException {
        try {
            final HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.host, this.port), 0);
            httpServer.createContext(HTTP_ACTORS_ACTIONS_URI, new ActorServiceHandler(this, this.entities));
            httpServer.setExecutor(this.executor);
            httpServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Stopping Spawn HTTP Server with termination grace period {} ...", this.terminationGracePeriodSeconds);
                httpServer.stop(this.terminationGracePeriodSeconds);
            }));
        } catch (IOException ex) {
            throw new SpawnException(ex);
        }
    }

    private void registerActorSystem() throws ActorRegistrationException {
        ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder().putAllActors(getActors(this.entities)).build();

        ActorOuterClass.ActorSystem actorSystem = ActorOuterClass.ActorSystem.newBuilder().setName(this.system).setRegistry(registry).build();

        Protocol.ServiceInfo si = Protocol.ServiceInfo.newBuilder().setServiceName("jvm-std-sdk").setServiceVersion("0.5.0").setServiceRuntime(System.getProperty("java.version")).setProtocolMajorVersion(1).setProtocolMinorVersion(1).build();

        Protocol.RegistrationRequest req = Protocol.RegistrationRequest.newBuilder().setServiceInfo(si).setActorSystem(actorSystem).build();

        log.debug("Registering Actors on Proxy. Registry: {}", req);
        this.client.register(req);
    }

    private Map<String, ActorOuterClass.Actor> getActors(List<Entity> entities) {
        return entities.stream().map(actorEntity -> {
            ActorOuterClass.ActorSnapshotStrategy snapshotStrategy;
            if (actorEntity.isStateful()) {
                snapshotStrategy = ActorOuterClass.ActorSnapshotStrategy.newBuilder().setTimeout(ActorOuterClass.TimeoutStrategy.newBuilder().setTimeout(actorEntity.getSnapshotTimeout()).build()).build();
            } else {
                snapshotStrategy = ActorOuterClass.ActorSnapshotStrategy.newBuilder().build();
            }


            ActorOuterClass.ActorDeactivationStrategy deactivateStrategy = ActorOuterClass.ActorDeactivationStrategy.newBuilder().setTimeout(ActorOuterClass.TimeoutStrategy.newBuilder().setTimeout(actorEntity.getDeactivateTimeout()).build()).build();

            ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder().setKind(actorEntity.getKind()).setStateful(actorEntity.isStateful()).setSnapshotStrategy(snapshotStrategy).setDeactivationStrategy(deactivateStrategy).setMinPoolSize(actorEntity.getMinPoolSize()).setMaxPoolSize(actorEntity.getMaxPoolSize()).build();

            Map<String, String> tags = new HashMap<>();
            ActorOuterClass.Metadata metadata = ActorOuterClass.Metadata.newBuilder().setChannelGroup(actorEntity.getChannel()).putAllTags(tags).build();

            return ActorOuterClass.Actor.newBuilder().setId(ActorOuterClass.ActorId.newBuilder().setName(actorEntity.getActorName()).setSystem(this.system).build()).setMetadata(metadata).setSettings(settings).addAllActions(getActions(actorEntity)).addAllTimerActions(getTimerActions(actorEntity)).setState(ActorOuterClass.ActorState.newBuilder().build()).build();

        }).collect(Collectors.toMap(actor -> actor.getId().getName(), Function.identity()));
    }

    private List<ActorOuterClass.Action> getActions(Entity actorEntity) {
        return actorEntity.getActions().values().stream().filter(v -> Entity.EntityMethodType.DIRECT.equals(v.getType())).map(action -> ActorOuterClass.Action.newBuilder().setName(action.getName()).build()).collect(Collectors.toList());
    }

    private List<ActorOuterClass.FixedTimerAction> getTimerActions(Entity actorEntity) {
        List<ActorOuterClass.FixedTimerAction> timerActions = actorEntity.getTimerActions().values().stream().filter(v -> Entity.EntityMethodType.TIMER.equals(v.getType())).map(action -> ActorOuterClass.FixedTimerAction.newBuilder().setAction(ActorOuterClass.Action.newBuilder().setName(action.getName()).build()).setSeconds(action.getFixedPeriod()).build()).collect(Collectors.toList());

        log.debug("Actor have TimeActions: {}", timerActions);
        return timerActions;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Spawn.class.getSimpleName() + "[", "]").add("system='" + system + "'").add("port=" + port).add("host='" + host + "'").add("proxyHost='" + proxyHost + "'").add("proxyPort=" + proxyPort).toString();
    }

    public static final class SpawnSystem {
        private final List<Entity> entities = new ArrayList<>();

        private Cache<ActorOuterClass.ActorId, ActorRef> actorIdCache;
        private SpawnClient client;
        private String system = "spawn-system";

        private int terminationGracePeriodSeconds = 30;

        private TransportOpts transportOpts = TransportOpts.builder().build();

        /**
         * <p>Builder method that establishes the ActorSystem to which the application will be part.
         * </p>
         *
         * @param system ActorSystem name of the actor that this ActorRef instance should represent
         * @return the SpawnSystem instance
         * @since 0.0.1
         */
        public SpawnSystem create(String system) {
            this.system = system;
            return this;
        }

        /**
         * <p>Builder method that establishes the ActorSystem to which the application will be part.
         * The name of the ActorSystem will be captured at runtime via an environment variable called PROXY_ACTOR_SYSTEM_NAME
         * </p>
         *
         * @return the SpawnSystem instance
         * @since 0.0.1
         */
        public SpawnSystem createFromEnv() {
            String system = System.getenv("PROXY_ACTOR_SYSTEM_NAME");
            Objects.requireNonNull(system, "To use createFromEnv method it is necessary to have defined the environment variable PROXY_ACTOR_SYSTEM_NAME");
            this.system = system;
            return this;
        }

        /**
         * <p>Constructor method that adds a new Actor to the Spawn proxy.
         * </p>
         *
         * @param actorKlass the actor definition class
         * @return the SpawnSystem instance
         * @since 0.0.1
         */
        public SpawnSystem withActor(Class<?> actorKlass) {
            Optional<Entity> maybeEntity = getEntity(actorKlass);
            if (maybeEntity.isPresent()) {
                this.entities.add(maybeEntity.get());
            }
            return this;
        }

        /**
         * <p>Constructor method that adds a new Actor to the Spawn proxy.
         * Allows options to be passed to the class constructor. The constructor must consist of only one argument
         * </p>
         *
         * @param actorKlass the actor definition class
         * @param arg        the object that will be passed as an argument to the constructor via the lambda fabric
         * @param factory    a lambda that constructs the instance of the Actor object
         * @return the SpawnSystem instance
         * @since 0.0.1
         */
        public SpawnSystem withActor(Class<?> actorKlass, Object arg, ActorFactory factory) {
            Optional<Entity> maybeEntity = getEntity(actorKlass, arg, factory);
            if (maybeEntity.isPresent()) {
                this.entities.add(maybeEntity.get());
            }
            return this;
        }

        public SpawnSystem withTerminationGracePeriodSeconds(int seconds) {
            this.terminationGracePeriodSeconds = seconds;
            return this;
        }

        /**
         * @param opts TransportOpts instance with options for communicating with the proxy as well as other transport
         *             settings such as the type of Executor to be used to handle incoming requests.
         * @return the SpawnSystem instance
         * @since 0.0.1
         */
        public SpawnSystem withTransportOptions(TransportOpts opts) {
            this.transportOpts = opts;
            return this;
        }

        public Spawn build() {
            this.actorIdCache = Caffeine.newBuilder().maximumSize(CACHE_MAXIMUM_SIZE).expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRE_AFTER_WRITE_SECONDS)).build();

            this.client = new OkHttpSpawnClient(this.system, this.transportOpts.getProxyHost(), this.transportOpts.getProxyPort());

            return new Spawn(this);
        }

        private Optional<Entity> getEntity(Class<?> actorKlass) {
            Optional<Entity> maybeEntity = getStatefulEntity(actorKlass, null, null);

            if (maybeEntity.isPresent()) {
                return maybeEntity;
            }

            maybeEntity = getStatelessEntity(actorKlass, null, null);
            if (maybeEntity.isPresent()) {
                return maybeEntity;
            }

            return Optional.empty();
        }

        private Optional<Entity> getEntity(Class<?> actorKlass, Object arg, ActorFactory factory) {
            Optional<Entity> maybeEntity = getStatefulEntity(actorKlass, arg, factory);

            if (maybeEntity.isPresent()) {
                return maybeEntity;
            }

            maybeEntity = getStatelessEntity(actorKlass, arg, factory);
            if (maybeEntity.isPresent()) {
                return maybeEntity;
            }

            return Optional.empty();
        }

        private Optional<Entity> getStatefulEntity(Class<?> actorKlass, Object arg, ActorFactory factory) {
            if (Objects.nonNull(actorKlass.getAnnotation(StatefulNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatefulNamedActor.class), arg, factory));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(StatefulUnNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatefulUnNamedActor.class), arg, factory));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(StatefulPooledActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatefulPooledActor.class), arg, factory));
            }

            return Optional.empty();
        }

        private Optional<Entity> getStatelessEntity(Class<?> actorKlass, Object arg, ActorFactory factory) {
            if (Objects.nonNull(actorKlass.getAnnotation(StatelessNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatelessNamedActor.class), arg, factory));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(StatelessUnNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatelessUnNamedActor.class), arg, factory));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(StatelessPooledActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(StatelessPooledActor.class), arg, factory));
            }

            return Optional.empty();
        }
    }
}
