package io.eigr.spawn.api.actors;

@FunctionalInterface
public interface ActorFactory {
    <T extends Object> Object newInstance(T arg);
}
