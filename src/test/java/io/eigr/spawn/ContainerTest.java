package io.eigr.spawn;

import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.java.test.domain.Actor;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class ContainerTest extends AbstractContainerBaseTest {

    @Test
    public void testApp() throws SpawnException {
        ActorRef joeActor = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "test_joe"));

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("erlang")
                .build();

        Optional<Actor.Reply> maybeReply =
                joeActor.invoke("setLanguage", msg, Actor.Reply.class);

        if (maybeReply.isPresent()) {
            Actor.Reply reply = maybeReply.get();
            assertNotNull(reply);
            assertEquals("Hello From Java", reply.getResponse());
        } else {
            throw new RuntimeException("Error");
        }

    }
}
