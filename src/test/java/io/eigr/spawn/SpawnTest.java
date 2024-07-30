package io.eigr.spawn;

import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.java.test.domain.Actor;
import io.eigr.spawn.test.actors.JoeActor;
import io.eigr.spawn.test.actors.StatelessNamedActor;
import io.eigr.spawn.test.actors.UnNamedActor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class SpawnTest extends AbstractContainerBaseTest {

    @Test
    void testNamedInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef joeActor = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "JoeActor"));

        Class type = joeActor.getType();

        Assertions.assertEquals(type, JoeActor.class);
        Assertions.assertNotNull(joeActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                joeActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            Assertions.assertNotNull(reply);
            Assertions.assertEquals("Hi Erlang. Hello From Java", reply.getResponse());
        }
    }

    @Test
    void testUnNamedInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef unNamedJoeActor = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "UnNamedJoeActor", "UnNamedActor"));

        Class type = unNamedJoeActor.getType();

        Assertions.assertEquals(type, UnNamedActor.class);
        Assertions.assertNotNull(unNamedJoeActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                unNamedJoeActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            Assertions.assertNotNull(reply);
            Assertions.assertEquals("Hi Erlang. Hello From Java", reply.getResponse());
        }
    }

    @Test
    void testStatelessInvocation() throws ActorCreationException, ActorInvocationException {
        ActorRef statelessNamedActor = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "StatelessNamedActor"));

        Class type = statelessNamedActor.getType();

        Assertions.assertEquals(type, StatelessNamedActor.class);
        Assertions.assertNotNull(statelessNamedActor);

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("Elixir")
                .build();

        Optional<Actor.Reply> maybeReply =
                statelessNamedActor.invoke("SetLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            Assertions.assertNotNull(reply);
            Assertions.assertEquals("Hi Elixir. Hello From Java", reply.getResponse());
        }
    }
}
