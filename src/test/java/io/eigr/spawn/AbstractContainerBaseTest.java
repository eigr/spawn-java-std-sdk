package io.eigr.spawn;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import io.eigr.spawn.test.actors.ActorWithConstructor;
import io.eigr.spawn.test.actors.JoeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

abstract class AbstractContainerBaseTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractContainerBaseTest.class);
    private static final GenericContainer<?> SPAWN_CONTAINER;
    private static final String spawnProxyImage = "eigr/spawn-proxy:1.4.1-rc.1";
    private static final String userFunctionPort = "8091";
    private static final String spawnProxyPort = "9004";
    protected static final Spawn spawnSystem;
    protected static final String spawnSystemName = "spawn-system-test";

    static {
        Testcontainers.exposeHostPorts(8091);

        SPAWN_CONTAINER = new GenericContainer<>(DockerImageName.parse(spawnProxyImage))
                .withCreateContainerCmdModifier(e -> e.withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(PortBinding.parse("9004:9004"))))
               // .withEnv("TZ", "America/Fortaleza")
                .withEnv("SPAWN_PROXY_LOGGER_LEVEL", "DEBUG")
                .withEnv("SPAWN_STATESTORE_KEY", "3Jnb0hZiHIzHTOih7t2cTEPEpY98Tu1wvQkPfq/XwqE=")
                .withEnv("PROXY_ACTOR_SYSTEM_NAME", spawnSystemName)
                .withEnv("PROXY_DATABASE_TYPE", "native")
                .withEnv("PROXY_DATABASE_DATA_DIR", "mnesia_data")
                .withEnv("NODE_COOKIE", "cookie-9ce3712b0c3ee21b582c30f942c0d4da-HLuZyQzy+nt0p0r/PVVFTp2tqfLom5igrdmwkYSuO+Q=")
                .withEnv("POD_NAMESPACE", spawnSystemName)
                .withEnv("POD_IP", spawnSystemName)
                .withEnv("PROXY_HTTP_PORT", spawnProxyPort)
                .withEnv("USER_FUNCTION_PORT", userFunctionPort)
                .withEnv("USER_FUNCTION_HOST", "host.docker.internal") // Docker
                .withExtraHost("host.docker.internal", "host-gateway") // Docker
//                .withEnv("USER_FUNCTION_HOST", "host.containers.internal") // Podman
//                .withExtraHost("host.containers.internal", "host-gateway") // Podman
                .withExposedPorts(9004);
        SPAWN_CONTAINER.start();

        DependencyInjector injector = SimpleDependencyInjector.createInjector();
        injector.bind(String.class, "Hello with Constructor");

        spawnSystem = new Spawn.SpawnSystem()
                .create(spawnSystemName)
                .withActor(JoeActor.class)
                .withActor(ActorWithConstructor.class, injector, arg -> new ActorWithConstructor((DependencyInjector) arg))
                .withTerminationGracePeriodSeconds(5)
                .withTransportOptions(TransportOpts.builder()
                        .port(8091)
                        .proxyPort(9004)
                        .build())
                .build();

        try {
            spawnSystem.start();
        } catch (SpawnException e) {
            throw new RuntimeException(e);
        }
        log.info(String.format("%s started", spawnSystemName));
    }
}

