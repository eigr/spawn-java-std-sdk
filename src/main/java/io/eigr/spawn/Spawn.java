package io.eigr.spawn;

import com.sun.net.httpserver.HttpServer;
import io.eigr.spawn.internal.handlers.ActorServiceHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Spawn SDK Entrypoint
 */
public final class Spawn {

    private String host;
    private int port;
    private String proxyHost;
    private int proxyPort;

    private String system;

    private List<Actor> actors;

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

    public List<Actor> getActors() {
        return actors;
    }

    public void start() throws IOException {
        startServer();
        registerActorSystem();
    }
    private void startServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        httpServer.createContext("/api/v1/actors", new ActorServiceHandler(this.system, this.actors));
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        httpServer.start();
    }

    private void registerActorSystem(){
        // TODO register actors on proxy
    }

    private Spawn(SpawnSystem builder) {
        this.port = builder.port;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.system = builder.system;
        this.actors = builder.actors;
    }

    private static final class SpawnSystem {
        private int port = 8091;
        private String proxyHost = "127.0.0.1";
        private int proxyPort = 9001;

        private String system = "spawn-system";
        private List<Actor> actors = new ArrayList<>();

        public SpawnSystem port(int port) {
            this.port = port;
            return this;
        }

        public SpawnSystem proxyHost(String host) {
            this.proxyHost = host;
            return this;
        }

        public SpawnSystem proxyPort(int port) {
            this.proxyPort = port;
            return this;
        }

        public SpawnSystem of(String system) {
            this.system = system;
            return this;
        }

        public SpawnSystem registerActor(Actor actor) {
            this.actors.add(actor);
            return this;
        }

        public SpawnSystem registerAllActors(Collection<Actor> actors) {
            this.actors.addAll(actors);
            return this;
        }

        public Spawn build() {
            return new Spawn(this);
        }

    }
}
