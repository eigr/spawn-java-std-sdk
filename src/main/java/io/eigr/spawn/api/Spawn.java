package io.eigr.spawn.api;

import com.sun.net.httpserver.HttpServer;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.actors.ActorFactory;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.actors.annotations.NamedActor;
import io.eigr.spawn.api.actors.annotations.PooledActor;
import io.eigr.spawn.api.actors.annotations.UnNamedActor;
import io.eigr.spawn.internal.Entity;
import io.eigr.spawn.internal.client.OkHttpSpawnClient;
import io.eigr.spawn.internal.client.SpawnClient;
import io.eigr.spawn.internal.handlers.ActorServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spawn SDK Entrypoint
 */
public final class Spawn {
    private static final Logger log = LoggerFactory.getLogger(Spawn.class);
    private static final String HTTP_ACTORS_ACTIONS_URI = "/api/v1/actors/actions";

    private final SpawnClient client;

    private final int port;

    private String host;

    private final String proxyHost;
    private final int proxyPort;
    private final String system;
    private final List<Entity> entities;

    private Optional<Executor> optionalExecutor;


    private Spawn(SpawnSystem builder) {
        this.system = builder.system;
        this.entities = builder.entities;
        this.port = builder.port;
        this.host = builder.host;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.client = builder.client;
        this.optionalExecutor = builder.optionalExecutor;
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

    public ActorRef createActorRef(String system, String name) throws Exception {
        return ActorRef.of(this.client, system, name);
    }

    public ActorRef createActorRef(String system, String name, String parent) throws Exception {
        return ActorRef.of(this.client, system, name, parent);
    }

    public void start() throws Exception {
        startServer();
        registerActorSystem();
    }

    private void startServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.host, this.port), 0);
        httpServer.createContext(HTTP_ACTORS_ACTIONS_URI, new ActorServiceHandler(this, this.entities));
        if (this.optionalExecutor.isPresent()) {
            httpServer.setExecutor(this.optionalExecutor.get());
        } else {
            httpServer.setExecutor(Executors.newCachedThreadPool());
        }
        httpServer.start();
    }

    private void registerActorSystem() throws Exception {
        ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder()
                .putAllActors(getActors(this.entities))
                .build();

        ActorOuterClass.ActorSystem actorSystem = ActorOuterClass.ActorSystem.newBuilder()
                .setName(this.system)
                .setRegistry(registry)
                .build();

        Protocol.ServiceInfo si = Protocol.ServiceInfo.newBuilder()
                .setServiceName("jvm-std-sdk")
                .setServiceVersion("0.5.0")
                .setServiceRuntime(System.getProperty("java.version"))
                .setProtocolMajorVersion(1)
                .setProtocolMinorVersion(1)
                .build();

        Protocol.RegistrationRequest req = Protocol.RegistrationRequest.newBuilder()
                .setServiceInfo(si)
                .setActorSystem(actorSystem)
                .build();

        log.debug("Registering Actors on Proxy. Registry: {}", req);
        this.client.register(req);
    }

    private Map<String, ActorOuterClass.Actor> getActors(List<Entity> entities) {
        return entities.stream().map(actorEntity -> {
            ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                    ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                            .setTimeout(
                                    ActorOuterClass.TimeoutStrategy.newBuilder()
                                            .setTimeout(actorEntity.getSnapshotTimeout())
                                            .build()
                            )
                            .build();

            ActorOuterClass.ActorDeactivationStrategy deactivateStrategy =
                    ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                            .setTimeout(
                                    ActorOuterClass.TimeoutStrategy.newBuilder()
                                            .setTimeout(actorEntity.getDeactivateTimeout())
                                            .build()
                            )
                            .build();

            ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                    .setKind(actorEntity.getKind())
                    .setStateful(actorEntity.isStateful())
                    .setSnapshotStrategy(snapshotStrategy)
                    .setDeactivationStrategy(deactivateStrategy)
                    .setMinPoolSize(actorEntity.getMinPoolSize())
                    .setMaxPoolSize(actorEntity.getMaxPoolSize())
                    .build();

            Map<String, String> tags = new HashMap<>();
            ActorOuterClass.Metadata metadata = ActorOuterClass.Metadata.newBuilder()
                    .setChannelGroup(actorEntity.getChannel())
                    .putAllTags(tags)
                    .build();

            return ActorOuterClass.Actor.newBuilder()
                    .setId(
                            ActorOuterClass.ActorId.newBuilder()
                                    .setName(actorEntity.getActorName())
                                    .setSystem(this.system)
                                    .build()
                    )
                    .setMetadata(metadata)
                    .setSettings(settings)
                    .addAllActions(getActions(actorEntity))
                    .addAllTimerActions(getTimerActions(actorEntity))
                    .setState(ActorOuterClass.ActorState.newBuilder().build())
                    .build();

        }).collect(Collectors.toMap(actor -> actor.getId().getName(), Function.identity()));
    }

    private List<ActorOuterClass.Action> getActions(Entity actorEntity) {
        return actorEntity.getActions()
                .values()
                .stream()
                .filter(v -> Entity.EntityMethodType.DIRECT.equals(v.getType()))
                .map(action ->
                        ActorOuterClass.Action.newBuilder()
                                .setName(action.getName())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<ActorOuterClass.FixedTimerAction> getTimerActions(Entity actorEntity) {
        List<ActorOuterClass.FixedTimerAction> timerActions = actorEntity.getTimerActions()
                .values()
                .stream()
                .filter(v -> Entity.EntityMethodType.TIMER.equals(v.getType()))
                .map(action ->
                        ActorOuterClass.FixedTimerAction.newBuilder()
                                .setAction(
                                        ActorOuterClass.Action.newBuilder()
                                                .setName(action.getName())
                                                .build())
                                .setSeconds(action.getFixedPeriod())
                                .build()
                )
                .collect(Collectors.toList());

        log.debug("Actor have TimeActions: {}", timerActions);
        return timerActions;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Spawn.class.getSimpleName() + "[", "]")
                .add("system='" + system + "'")
                .add("port=" + port)
                .add("host='" + host + "'")
                .add("proxyHost='" + proxyHost + "'")
                .add("proxyPort=" + proxyPort)
                .toString();
    }

    public static final class SpawnSystem {

        private SpawnClient client;
        private final List<Entity> entities = new ArrayList<>();
        private int port = 8091;
        private String host = "127.0.0.1";
        private String proxyHost = "127.0.0.1";
        private int proxyPort = 9001;
        private String system = "spawn-system";

        private Optional<Executor> optionalExecutor = Optional.empty();

        public SpawnSystem create(String system) {
            this.system = system;
            return this;
        }

        public SpawnSystem withPort(int port) {
            this.port = port;
            return this;
        }

        public SpawnSystem withHost(String host) {
            this.host = host;
            return this;
        }

        public SpawnSystem withProxyHost(String host) {
            this.proxyHost = host;
            return this;
        }

        public SpawnSystem withProxyPort(int port) {
            this.proxyPort = port;
            return this;
        }

        public SpawnSystem withHttpHandlerExecutor(Executor executor) {
            this.optionalExecutor = Optional.of(executor);
            return this;
        }

        public SpawnSystem addActor(Class<?> actorKlass) {
            Optional<Entity> maybeEntity = getEntity(actorKlass);
            if (maybeEntity.isPresent()) {
                this.entities.add(maybeEntity.get());
            }
            return this;
        }

        public SpawnSystem addActorWithArgs(Class<?> actorKlass, Object arg, ActorFactory factory) {
            Optional<Entity> maybeEntity = getEntity(actorKlass, arg, factory);
            if (maybeEntity.isPresent()) {
                this.entities.add(maybeEntity.get());
            }
            return this;
        }

        public Spawn build() {
            this.client = new OkHttpSpawnClient(this.system, this.proxyHost, this.proxyPort);
            return new Spawn(this);
        }

        private Optional<Entity> getEntity(Class<?> actorKlass) {
            if (Objects.nonNull(actorKlass.getAnnotation(NamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(
                        actorKlass, actorKlass.getAnnotation(NamedActor.class), null, null));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(UnNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(
                        actorKlass, actorKlass.getAnnotation(UnNamedActor.class), null, null));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(PooledActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(
                        actorKlass, actorKlass.getAnnotation(PooledActor.class), null, null));
            }

            return Optional.empty();
        }

        private Optional<Entity> getEntity(Class<?> actorType, Object arg, ActorFactory factory) {
            if (Objects.nonNull(actorType.getAnnotation(NamedActor.class))) {
                NamedActor annotation = actorType.getAnnotation(NamedActor.class);
                return Optional.of(Entity.fromAnnotationToEntity(actorType, annotation, arg, factory));
            }

            if (Objects.nonNull(actorType.getAnnotation(UnNamedActor.class))) {
                UnNamedActor annotation = actorType.getAnnotation(UnNamedActor.class);
                return Optional.of(Entity.fromAnnotationToEntity(actorType, annotation, arg, factory));
            }

            if (Objects.nonNull(actorType.getAnnotation(PooledActor.class))) {
                PooledActor annotation = actorType.getAnnotation(PooledActor.class);
                return Optional.of(Entity.fromAnnotationToEntity(actorType, annotation, arg, factory));
            }

            return Optional.empty();
        }
    }
}
