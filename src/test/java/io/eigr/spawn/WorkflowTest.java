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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowTest extends AbstractContainerBaseTest {

    private ActorRef joeActorRef;

    @BeforeEach
    public void before() throws SpawnException {
        joeActorRef = spawnSystem.createActorRef(
                ActorIdentity.of(spawnSystemName, "JoeActor"));
    }

    @Test
    void testBroadcastBuilder() {
        Broadcast broadcast = Broadcast.to("test.channel", "hi", Actor.Request.getDefaultInstance());
        final Protocol.Broadcast protocolBroadcast = broadcast.build();
        Assertions.assertEquals("test.channel", protocolBroadcast.getChannelGroup());
        Assertions.assertNotNull(protocolBroadcast.getValue());
    }

    @Test
    void testForwardBuilder() throws Exception {
        Forward forward = Forward.to(joeActorRef, "hi");
        final Protocol.Forward protocolForward = forward.build();
        Assertions.assertEquals("hi", protocolForward.getActionName());
        Assertions.assertEquals("JoeActor", protocolForward.getActor());
    }

    @Test
    void testPipeBuilder() throws Exception {
        Pipe pipe = Pipe.to(joeActorRef, "hi");
        final Protocol.Pipe protocolPipe = pipe.build();
        Assertions.assertEquals("hi", protocolPipe.getActionName());
        Assertions.assertEquals("JoeActor", protocolPipe.getActor());
    }

    @Test
    void testSideEffectBuilder() throws Exception {
        SideEffect effect = SideEffect.to(joeActorRef, "hi", Actor.Request.getDefaultInstance());
        final Protocol.SideEffect protocolSideEffect = effect.build();
        Protocol.InvocationRequest request = protocolSideEffect.getRequest();
        Assertions.assertNotNull(request);
        Assertions.assertEquals("hi", request.getActionName());
        Assertions.assertEquals("JoeActor", request.getActor().getId().getName());
    }
}
