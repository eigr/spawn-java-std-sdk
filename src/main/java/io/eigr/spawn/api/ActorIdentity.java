package io.eigr.spawn.api;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class ActorIdentity {
    private final String system;
    private final String name;
    private final Optional<String> maybeParent;

    private ActorIdentity(String system, String name, String parent){
        this.system = system;
        this.name = name;
        this.maybeParent = Optional.ofNullable(parent);
    }

    private ActorIdentity(String system, String name){
        this.system = system;
        this.name = name;
        this.maybeParent = Optional.empty();
    }

    public static ActorIdentity of(String system, String name) {
        return new ActorIdentity(system, name);
    }

    public static ActorIdentity of(String system, String name, String parent) {
        return new ActorIdentity(system, name, parent);
    }

    public String getSystem() {
        return system;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return maybeParent.get();
    }

    public Optional<String> getMaybeParent() {
        return maybeParent;
    }

    public boolean isParent() {
        return this.maybeParent.isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActorIdentity actorIdentity = (ActorIdentity) o;
        return system.equals(actorIdentity.system) && name.equals(actorIdentity.name) && maybeParent.equals(actorIdentity.maybeParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, name, maybeParent);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ActorIdentity.class.getSimpleName() + "[", "]")
                .add("system='" + system + "'")
                .add("name='" + name + "'")
                .add("maybeParent=" + maybeParent)
                .toString();
    }
}
