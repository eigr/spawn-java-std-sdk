package io.eigr.spawn;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.java.test.domain.Actor;
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
        spawnSystem = new Spawn.SpawnSystem()
                .create("spawn-system")
                .withPort(8091)
                .withProxyPort(9003)
                .addActor(JoeActor.class)
                .build();

        spawnSystem.start();
    }

    @Test
    public void testApp() throws Exception {
        ActorRef joeActor = spawnSystem.createActorRef("spawn-system", "test_joe");

        Actor.Request msg = Actor.Request.newBuilder()
                .setLanguage("erlang")
                .build();

        Actor.Reply reply =
                (Actor.Reply) joeActor.invoke("setLanguage", msg, Actor.Reply.class, Optional.empty());

        assertNotNull(reply);
        assertEquals("Hello From Java", reply.getResponse());
    }
}
