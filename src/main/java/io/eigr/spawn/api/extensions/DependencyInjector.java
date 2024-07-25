package io.eigr.spawn.api.extensions;

public interface DependencyInjector {

    <T extends Object> void bind(Class<T> type, Object instance);

    <T extends Object> void bind(Class<T> type, String alias, Object instance);

    <T extends Object> T getInstance(Class<T> type);

    <T extends Object> T getInstanceByAlias(String alias);
}
