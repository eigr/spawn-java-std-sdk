package io.eigr.spawn.internal;

import java.util.StringJoiner;

public final class ActionEnvelope {

    private final ActionEmptyFunction function;
    private final ActionConfiguration config;

    public ActionEnvelope(ActionEmptyFunction function, ActionConfiguration config) {
        this.function = function;
        this.config = config;
    }

    public ActionEmptyFunction getFunction() {
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
