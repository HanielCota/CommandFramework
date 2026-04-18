package io.github.hanielcota.commandframework.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal service locator used to inject collaborators into command classes and executors.
 *
 * <p>Lookups first try an exact-type hit; on miss they fall back to a single assignable binding.
 * Ambiguous assignable matches (more than one binding whose concrete type is assignable to the
 * requested type) throw {@link IllegalStateException} rather than returning a best guess - this
 * guarantees command classes always receive the exact collaborator the operator bound, even as
 * bindings accumulate.
 *
 * <p><b>Thread-safety:</b> all state is held in a {@link ConcurrentHashMap}. Reads and writes are
 * safe for concurrent use.
 */
public final class DependencyContainer {

    private final Map<Class<?>, Object> bindings = new ConcurrentHashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        this.bindings.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(instance, "instance"));
    }

    public Object resolve(Class<?> type) {
        Objects.requireNonNull(type, "type");
        Object direct = this.bindings.get(type);
        if (direct != null) {
            return direct;
        }

        Object candidate = null;
        for (Map.Entry<Class<?>, Object> entry : this.bindings.entrySet()) {
            if (!type.isAssignableFrom(entry.getKey())) {
                continue;
            }
            if (candidate != null) {
                throw new IllegalStateException("Multiple bindings match " + type.getName());
            }
            candidate = entry.getValue();
        }
        return candidate;
    }
}
