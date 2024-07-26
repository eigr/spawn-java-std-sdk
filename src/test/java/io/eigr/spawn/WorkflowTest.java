package io.eigr.spawn;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.java.test.domain.Actor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorkflowTest extends AbstractContainerBaseTest {

    private ActorRef joeActorRef;

    @Before
    public void before() throws SpawnException {
        joeActorRef = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "JoeActor"));
    }

    @Test
    public void testBroadcastBuilder() {
        Broadcast broadcast = Broadcast.to("test.channel", "hi", Actor.Request.getDefaultInstance());
        final Protocol.Broadcast protocolBroadcast = broadcast.build();
        assertEquals("test.channel", protocolBroadcast.getChannelGroup());
        assertNotNull(protocolBroadcast.getValue());
    }

    @Test
    public void testForwardBuilder() throws Exception {
        Forward forward = Forward.to(joeActorRef, "hi");
        final Protocol.Forward protocolForward = forward.build();
        assertEquals("hi", protocolForward.getActionName());
        assertEquals("JoeActor", protocolForward.getActor());
    }

    @Test
    public void testPipeBuilder() throws Exception {
        Pipe pipe = Pipe.to(joeActorRef, "hi");
        final Protocol.Pipe protocolPipe = pipe.build();
        assertEquals("hi", protocolPipe.getActionName());
        assertEquals("JoeActor", protocolPipe.getActor());
    }

    @Test
    public void testSideEffectBuilder() throws Exception {
        SideEffect effect = SideEffect.to(joeActorRef, "hi", Actor.Request.getDefaultInstance());
        final Protocol.SideEffect protocolSideEffect = effect.build();
        Protocol.InvocationRequest request = protocolSideEffect.getRequest();
        assertNotNull(request);
        assertEquals("hi", request.getActionName());
        assertEquals("JoeActor", request.getActor().getId().getName());
    }
}
