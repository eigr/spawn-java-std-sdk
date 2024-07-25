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
import io.eigr.spawn.test.actors.StatelessNamedActor;
import io.eigr.spawn.test.actors.UnNamedActor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpawnTest {

    private static Spawn spawnSystem;

    @BeforeClass
    public static void setup() throws Exception {
        DependencyInjector injector = SimpleDependencyInjector.createInjector();
        injector.bind(String.class, "Hello with Constructor");

        spawnSystem = new Spawn.SpawnSystem()
                .create("spawn-system", injector)
                .withActor(JoeActor.class)
                .withActor(UnNamedActor.class)
                .withActor(ActorWithConstructor.class)
                .withActor(StatelessNamedActor.class)
                .withTransportOptions(
                        TransportOpts.builder()
                                .port(8091)
                                .proxyPort(9003)
                                .build()
                )
                .build();

        System.out.println("Starting ActorSystem...");
        spawnSystem.start();
    }

    @Test
    public void testNamedInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef joeActor = spawnSystem.createActorRef(
                ActorIdentity.of("spawn-system", "JoeActor"));

        Class type = joeActor.getType();

        assertEquals(type, JoeActor.class);
        assertNotNull(joeActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                joeActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            assertNotNull(reply);
            assertEquals("Hi Erlang. Hello From Java", reply.getResponse());
        }
    }

    @Test
    public void testUnNamedInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef unNamedJoeActor = spawnSystem.createActorRef(
                ActorIdentity.of("spawn-system", "UnNamedJoeActor", "UnNamedActor"));

        Class type = unNamedJoeActor.getType();

        assertEquals(type, UnNamedActor.class);
        assertNotNull(unNamedJoeActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                unNamedJoeActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            assertNotNull(reply);
            assertEquals("Hi Erlang. Hello From Java", reply.getResponse());
        }
    }

    @Test
    public void testStatelessInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef statelessNamedActor = spawnSystem.createActorRef(
                ActorIdentity.of("spawn-system", "StatelessNamedActor"));

        Class type = statelessNamedActor.getType();

        assertEquals(type, StatelessNamedActor.class);
        assertNotNull(statelessNamedActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Elixir")
                .build();

        Optional<Actor.Reply> maybeReply =
                statelessNamedActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            assertNotNull(reply);
            assertEquals("Hi Elixir. Hello From Java", reply.getResponse());
        }
    }
}
