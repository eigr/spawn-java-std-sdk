package io.eigr.spawn;

import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.internal.Entity;
import io.eigr.spawn.test.actors.JoeActor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityTest {

    @Test
    public void testEntityBuilder() {
        StatefulNamedActor annotation = JoeActor.class.getAnnotation(StatefulNamedActor.class);
        final Entity entity = Entity.fromAnnotationToEntity(JoeActor.class, annotation, null, null);
        assertEquals(1, entity.getActions().values().size());
        assertEquals(0, entity.getTimerActions().values().size());
        assertEquals("test.channel", entity.getChannel());
    }
}
