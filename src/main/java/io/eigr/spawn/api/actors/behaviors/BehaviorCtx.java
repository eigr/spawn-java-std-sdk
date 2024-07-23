package io.eigr.spawn.api.actors.behaviors;

import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;

public final class BehaviorCtx {
    private DependencyInjector  injector;

    private BehaviorCtx() {
        this.injector = SimpleDependencyInjector.createInjector();
    }

    private BehaviorCtx(DependencyInjector injector) {
        this.injector = injector;
    }

    public static BehaviorCtx create() {
        return new BehaviorCtx();
    }

    public static BehaviorCtx create(DependencyInjector injector) {
        return new BehaviorCtx(injector);
    }

    public DependencyInjector getInjector() {
        return injector;
    }
}
