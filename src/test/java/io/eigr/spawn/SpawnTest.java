package io.eigr.spawn;

import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.java.test.domain.Actor;
import io.eigr.spawn.test.actors.JoeActor;
import io.eigr.spawn.test.actors.StatelessNamedActor;
import io.eigr.spawn.test.actors.UnNamedActor;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpawnTest extends AbstractContainerBaseTest {

    @Test
    public void testNamedInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef joeActor = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "JoeActor"));

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
                ActorIdentity.of(spawnSystemName, "UnNamedJoeActor", "UnNamedActor"));

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
                ActorIdentity.of(spawnSystemName, "StatelessNamedActor"));

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
