package io.eigr.spawn.test.extensions;

import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleDependencyInjectorTest {

    private DependencyInjector injector;

    @Before
    public void before() {
        injector = SimpleDependencyInjector.createInjector();
    }

    @Test
    public void testInjection() {
        injector.bind(A.class, new AImplementation());

        A implementation = injector.getInstance(A.class);
        String actual = implementation.say("Spawn");
        assertEquals("Hello Spawn", actual);
    }
}
