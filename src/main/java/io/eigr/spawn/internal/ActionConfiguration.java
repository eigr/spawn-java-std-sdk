package io.eigr.spawn.internal;

import java.util.StringJoiner;

public final class ActionConfiguration {
    private final ActionKind kind;
    private int timer;

    private int arity;

    private Class<?> inputType;

    private Class<?> outputType;

    public ActionConfiguration(ActionKind kind, int arity, Class<?> inputType, Class<?> outputType) {
        this.kind = kind;
        this.arity = arity;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public ActionConfiguration(ActionKind kind, int timer, int arity, Class<?> inputType, Class<?> outputType) {
        this.kind = kind;
        this.timer = timer;
        this.arity = arity;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public ActionKind getKind() {
        return kind;
    }

    public int getTimer() {
        return timer;
    }

    public Class<?> getInputType() {
        return inputType;
    }

    public Class<?> getOutputType() {
        return outputType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ActionConfiguration.class.getSimpleName() + "[", "]")
                .add("kind=" + kind)
                .add("timer=" + timer)
                .toString();
    }

    public int getArity() {
        return this.arity;
    }
}
