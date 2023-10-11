package io.eigr.spawn.api.extensions;

import java.util.HashMap;
import java.util.Map;

public final class SimpleDependencyInjector implements DependencyInjector {

    private final Map<Class<?>, Object> bucket;

    private SimpleDependencyInjector() {
        this.bucket = new HashMap<>();
    }

    private static class SimpleDependencyInjectorHelper{
        private static final SimpleDependencyInjector INSTANCE = new SimpleDependencyInjector();
    }

    public static DependencyInjector createInjector(){
        return SimpleDependencyInjectorHelper.INSTANCE;
    }

    @Override
    public <T> void bind(Class<T> type, Object instance) {
        if (this.bucket.containsKey(type)) {
            throw new IllegalArgumentException("There is already an instance associated with this type in the bucket");
        }

        this.bucket.put(type, instance);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return type.cast(this.bucket.get(type));
    }
}
