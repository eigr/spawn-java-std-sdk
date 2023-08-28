package io.eigr.spawn.api.actors;

import io.eigr.spawn.internal.client.SpawnClient;

import java.util.Optional;

public final class ActorRef {

    private final String name;

    private final String system;

    private final Optional<String> parent;
    private final SpawnClient client;

    private ActorRef(SpawnClient client, String system, String name) {
        this.client = client;
        this.system = system;
        this.name = name;
        this.parent = Optional.empty();
    }
    private ActorRef(SpawnClient client, String system, String name, String parent) {
        this.client = client;
        this.system = system;
        this.name = name;
        this.parent = Optional.of(parent);
    }

    public static ActorRef of(SpawnClient client, String system, String name) {
        return new ActorRef(client, system, name);
    }

    public static ActorRef of(SpawnClient client, String system, String name, String parent) {
        return new ActorRef(client, system, name, parent);
    }

    public String getActorSystem() {
        return this.system;
    }

    public String getActorName() {
        return this.name;
    }

    public Optional<String> maybeActorParentName() {
        return this.parent;
    }

    public String getActorParentName() {
        return this.parent.get();
    }

    public boolean isUnnamedActor() {
        return Optional.empty().isPresent();
    }
}
