package io.eigr.spawn;

import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import io.eigr.spawn.java.test.domain.Actor;
import io.eigr.spawn.test.actors.ActorWithConstructor;
import io.eigr.spawn.test.actors.JoeActor;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpawnTest {

    private Spawn spawnSystem;

    @Before
    public void before() throws Exception {
        DependencyInjector injector = SimpleDependencyInjector.createInjector();
        injector.bind(String.class, "Hello with Constructor");

        spawnSystem = new Spawn.SpawnSystem()
                .create("spawn-system")
                .withActor(JoeActor.class)
                .withActor(ActorWithConstructor.class, injector, arg -> new ActorWithConstructor((DependencyInjector) arg))
                .withTransportOptions(
                        TransportOpts.builder()
                                .port(8091)
                                .proxyPort(9003)
                                .build()
                )
                .build();

        spawnSystem.start();
    }

    @Test
    public void testApp() throws ActorCreationException, ActorInvocationException {
        ActorRef joeActor = spawnSystem.createActorRef(
                ActorIdentity.of("spawn-system", "test_joe"));

        assertNotNull(joeActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                joeActor.invoke("setLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            assertNotNull(reply);
            assertEquals("Hello From Java", reply.getResponse());
        }
    }
}
