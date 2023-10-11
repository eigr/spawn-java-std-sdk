package io.eigr.spawn.test.extensions;

public class AImplementation implements A {
    @Override
    public String say(String name) {
        return String.format("Hello %s", name);
    }
}
