package io.eigr.spawn;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SpawnTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SpawnTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(SpawnTest.class);
    }

    public void testApp() {
        new Spawn.SpawnSystem().build();
        assertTrue(true);
    }
}
