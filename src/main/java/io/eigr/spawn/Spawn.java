package io.eigr.spawn;

import com.sun.net.httpserver.HttpServer;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.annotations.NamedActor;
import io.eigr.spawn.api.annotations.PooledActor;
import io.eigr.spawn.api.annotations.UnNamedActor;
import io.eigr.spawn.internal.Entity;
import io.eigr.spawn.internal.client.OkHttpSpawnClient;
import io.eigr.spawn.internal.client.SpawnClient;
import io.eigr.spawn.internal.handlers.ActorServiceHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spawn SDK Entrypoint
 */
public final class Spawn {

    private final SpawnClient client;

    private final int port;
    private final String proxyHost;
    private final int proxyPort;
    private final String system;
    private final List<Entity> entities;

    private String host;

    private Spawn(SpawnSystem builder) {
        this.system = builder.system;
        this.entities = builder.entities;
        this.port = builder.port;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.client = builder.client;
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

    public void start() throws Exception {
        startServer();
        registerActorSystem();
    }

    public ActorRef createActorRef(String system, String name) {
        return ActorRef.of(this.client, system, name);
    }

    public ActorRef createActorRef(String system, String name, String parent) {
        return ActorRef.of(this.client, system, name, parent);
    }

    private void startServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        httpServer.createContext("/api/v1/actors/actions", new ActorServiceHandler(this.system, this.entities));
        //httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.setExecutor(Executors.newCachedThreadPool());
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
                    .addAllActions(getCommands(actorEntity))
                    .addAllTimerActions(getTimerCommands(actorEntity))
                    .setState(ActorOuterClass.ActorState.newBuilder().build())
                    .build();

        }).collect(Collectors.toMap(actor -> actor.getId().getName(), Function.identity()));
    }

    private List<ActorOuterClass.Action> getCommands(Entity actorEntity) {
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

    private List<ActorOuterClass.FixedTimerAction> getTimerCommands(Entity actorEntity) {
        return actorEntity.getActions()
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
    }

    private static final class SpawnSystem {

        private SpawnClient client;
        private final List<Entity> entities = new ArrayList<>();
        private int port = 8091;
        private String proxyHost = "127.0.0.1";
        private int proxyPort = 9001;
        private String system = "spawn-system";

        public SpawnSystem create(String system) {
            this.system = system;
            return this;
        }

        public SpawnSystem withPort(int port) {
            this.port = port;
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

        public SpawnSystem withActor(Class<?> actorKlass) {
            Optional<Entity> maybeEntity = getEntity(actorKlass);
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
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(NamedActor.class)));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(UnNamedActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(UnNamedActor.class)));
            }

            if (Objects.nonNull(actorKlass.getAnnotation(PooledActor.class))) {
                return Optional.of(Entity.fromAnnotationToEntity(actorKlass, actorKlass.getAnnotation(PooledActor.class)));
            }

            return Optional.empty();
        }
    }
}
