package io.eigr.spawn.api.actors;

@FunctionalInterface
public interface ActorFactory {

    Object newInstance(Object arg);
}
