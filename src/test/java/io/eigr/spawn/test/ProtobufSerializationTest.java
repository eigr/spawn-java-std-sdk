package io.eigr.spawn.test;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import domain.actors.State;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProtobufSerializationTest {
    private static final Logger log = LoggerFactory.getLogger(ProtobufSerializationTest.class);

    @Test
    public void testAnyUnpack() throws InvalidProtocolBufferException, ClassNotFoundException {
        State stateActor = State.newBuilder().buildPartial();

        Any anyCtxState = Any.pack(stateActor);

        String typeUrl = anyCtxState.getTypeUrl();
        String typeName = typeUrl.substring(typeUrl.lastIndexOf('/') + 1);

        Class protoClass = Class.forName(typeName);
        stateActor = (State) anyCtxState.unpack(protoClass);
        assertNotNull(stateActor);
    }
}
