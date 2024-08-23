package io.eigr.spawn.test;

import domain.actors.Reply;
import domain.actors.Request;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
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

        Request msg = Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Reply> maybeReply =
                joeActor.invoke("SetLanguage", msg, Reply.class);

        if (maybeReply.isPresent()) {
            Reply reply = maybeReply.get();
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

        Request msg = Request.newBuilder()
                .setLanguage("Erlang")
                .build();

        Optional<Reply> maybeReply =
                unNamedJoeActor.invoke("SetLanguage", msg, Reply.class);

        if (maybeReply.isPresent()) {
            Reply reply = maybeReply.get();
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

        Request msg = Request.newBuilder()
                .setLanguage("Elixir")
                .build();

        Optional<Reply> maybeReply =
                statelessNamedActor.invoke("SetLanguage", msg, Reply.class);

        if (maybeReply.isPresent()) {
            Reply reply = maybeReply.get();
            Assertions.assertNotNull(reply);
            Assertions.assertEquals("Hi Elixir. Hello From Java", reply.getResponse());
        }
    }
}
