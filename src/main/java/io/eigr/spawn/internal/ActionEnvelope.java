package io.eigr.spawn.internal;

import java.util.StringJoiner;

public final class ActionEnvelope<F extends ActionEmptyFunction> {

    private final F function;
    private final ActionConfiguration config;

    public ActionEnvelope(F function, ActionConfiguration config) {
        this.function = function;
        this.config = config;
    }

    public F getFunction() {
        return function;
    }

    public ActionConfiguration getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ActionEnvelope.class.getSimpleName() + "[", "]")
                .add("function=" + function)
                .add("config=" + config)
                .toString();
    }
}
