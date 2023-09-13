package io.eigr.spawn;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.java.test.domain.Actor;
import io.eigr.spawn.test.actors.JoeActor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorkflowTest {

    private ActorRef joeActorRef;
    private Spawn spawnSystem;
    @Before
    public void before() throws SpawnException {
        spawnSystem = new Spawn.SpawnSystem()
                .create("spawn-system")
                .withActor(JoeActor.class)
                .withTransportOptions(
                        TransportOpts.builder()
                                .port(8091)
                                .proxyPort(9003)
                                .build()
                )
                .build();

        joeActorRef = spawnSystem.createActorRef(
                ActorIdentity.of("spawn-system", "joe"));
    }

    @Test
    public void testBroadcastBuilder() {
        Broadcast broadcast = Broadcast.to("test.channel", "hi", Actor.Request.getDefaultInstance());
        final Protocol.Broadcast protocolBroadcast = broadcast.build();
        assertEquals("hi", protocolBroadcast.getActionName());
        assertEquals("test.channel", protocolBroadcast.getChannelGroup());
        assertNotNull(protocolBroadcast.getValue());
    }

    @Test
    public void testForwardBuilder() throws Exception {
        Forward forward = Forward.to(joeActorRef, "hi");
        final Protocol.Forward protocolForward = forward.build();
        assertEquals("hi", protocolForward.getActionName());
        assertEquals("joe", protocolForward.getActor());
    }

    @Test
    public void testPipeBuilder() throws Exception {
        Pipe pipe = Pipe.to(joeActorRef, "hi");
        final Protocol.Pipe protocolPipe = pipe.build();
        assertEquals("hi", protocolPipe.getActionName());
        assertEquals("joe", protocolPipe.getActor());
    }

    @Test
    public void testSideEffectBuilder() throws Exception {
        SideEffect effect = SideEffect.to(joeActorRef, "hi", Actor.Request.getDefaultInstance());
        final Protocol.SideEffect protocolSideEffect = effect.build();
        Protocol.InvocationRequest request = protocolSideEffect.getRequest();
        assertNotNull(request);
        assertEquals("hi", request.getActionName());
        assertEquals("joe", request.getActor().getId().getName());
    }
}
