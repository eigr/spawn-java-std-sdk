package io.eigr.spawn;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.java.test.domain.Actor;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorkflowTest {

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
        Forward forward = Forward.to(ActorRef.of(null, "spawn-system", "joe"), "hi");
        final Protocol.Forward protocolForward = forward.build();
        assertEquals("hi", protocolForward.getActionName());
        assertEquals("joe", protocolForward.getActor());
    }

    @Test
    public void testPipeBuilder() throws Exception {
        Pipe pipe = Pipe.to(ActorRef.of(null, "spawn-system", "joe"), "hi");
        final Protocol.Pipe protocolPipe = pipe.build();
        assertEquals("hi", protocolPipe.getActionName());
        assertEquals("joe", protocolPipe.getActor());
    }

    @Test
    public void testSideEffectBuilder() throws Exception {
        SideEffect effect = SideEffect.to(ActorRef.of(null, "spawn-system", "joe"), "hi", Actor.Request.getDefaultInstance());
        final Protocol.SideEffect protocolSideEffect = effect.build();
        Protocol.InvocationRequest request = protocolSideEffect.getRequest();
        assertNotNull(request);
        assertEquals("hi", request.getActionName());
        assertEquals("joe", request.getActor().getId().getName());
    }
}
