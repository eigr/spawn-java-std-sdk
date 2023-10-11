package io.eigr.spawn.api;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class ActorIdentity {
    private final String system;
    private final String name;
    private final Optional<String> maybeParent;

    private final boolean lookup;

    private ActorIdentity(String system, String name, String parent, boolean lookup){
        this.system = system;
        this.name = name;
        this.maybeParent = Optional.of(parent);
        this.lookup = lookup;
    }

    private ActorIdentity(String system, String name, boolean lookup){
        this.system = system;
        this.name = name;
        this.maybeParent = Optional.empty();
        this.lookup = lookup;
    }

    public static ActorIdentity of(String system, String name) {
        return new ActorIdentity(system, name, false);
    }

    public static ActorIdentity of(String system, String name, String parent) {
        return new ActorIdentity(system, name, parent, true);
    }

    public static ActorIdentity of(String system, String name, boolean lookup) {
        return new ActorIdentity(system, name, lookup);
    }

    public static ActorIdentity of(String system, String name, String parent, boolean lookup) {
        return new ActorIdentity(system, name, parent, lookup);
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

    public boolean hasLookup() {
        return lookup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActorIdentity that = (ActorIdentity) o;
        return lookup == that.lookup && system.equals(that.system) && name.equals(that.name) && maybeParent.equals(that.maybeParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, name, maybeParent, lookup);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ActorIdentity.class.getSimpleName() + "[", "]")
                .add("system='" + system + "'")
                .add("name='" + name + "'")
                .add("maybeParent=" + maybeParent)
                .add("lookup=" + lookup)
                .toString();
    }
}
