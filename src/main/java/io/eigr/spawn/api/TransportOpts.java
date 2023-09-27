package io.eigr.spawn.api;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransportOpts {

    private String host;
    private int port;
    private String proxyHost;
    private int proxyPort;
    private Executor executor;

    private TransportOpts(TransportOptsBuilder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.executor = builder.executor;
    }

    public static TransportOptsBuilder builder() {
        return new TransportOptsBuilder();
    }

    public String getHost() {
        return host;
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

    public Executor getExecutor() {
        return executor;
    }

    public static final class TransportOptsBuilder {

        private String host = "127.0.0.1";
        private int port = 8091;
        private String proxyHost = "127.0.0.1";
        private int proxyPort = 9001;
        private Executor executor = Executors.newCachedThreadPool();

        public TransportOpts build() {
            return new TransportOpts(this);
        }

        public TransportOptsBuilder host(String host) {
            this.host = host;
            return this;
        }

        public TransportOptsBuilder port(int port) {
            this.port = port;
            return this;
        }

        public TransportOptsBuilder proxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public TransportOptsBuilder proxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public TransportOptsBuilder executor(Executor executor) {
            this.executor = executor;
            return this;
        }
    }
}
