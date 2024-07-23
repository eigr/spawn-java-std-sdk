package io.eigr.spawn.internal;

import java.util.StringJoiner;

public final class ActionConfiguration {
    private final ActionKind kind;
    private int timer;

    public ActionConfiguration(ActionKind kind) {
        this.kind = kind;
    }

    public ActionConfiguration(ActionKind kind, int timer) {
        this.kind = kind;
        this.timer = timer;
    }

    public ActionKind getKind() {
        return kind;
    }

    public int getTimer() {
        return timer;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ActionConfiguration.class.getSimpleName() + "[", "]")
                .add("kind=" + kind)
                .add("timer=" + timer)
                .toString();
    }
}
