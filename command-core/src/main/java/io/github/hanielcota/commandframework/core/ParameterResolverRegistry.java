package io.github.hanielcota.commandframework.core;

import io.github.hanielcota.commandframework.core.argument.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class ParameterResolverRegistry {

    private final Map<Class<?>, ParameterResolver<?>> resolvers = new ConcurrentHashMap<>();

    public static ParameterResolverRegistry withDefaults() {
        ParameterResolverRegistry registry = new ParameterResolverRegistry();

        registry.register(new ActorParameterResolver());
        registry.register(new RawArgumentsParameterResolver());
        registry.registerArgument(new StringArgumentResolver());
        registry.registerArgument(new IntegerArgumentResolver(Integer.class));
        registry.registerArgument(new IntegerArgumentResolver(Integer.TYPE));
        registry.registerArgument(new LongArgumentResolver(Long.class));
        registry.registerArgument(new LongArgumentResolver(Long.TYPE));
        registry.registerArgument(new DoubleArgumentResolver(Double.class));
        registry.registerArgument(new DoubleArgumentResolver(Double.TYPE));
        registry.registerArgument(new BooleanArgumentResolver(Boolean.class));
        registry.registerArgument(new BooleanArgumentResolver(Boolean.TYPE));
        return registry;
    }

    public <T> void register(ParameterResolver<T> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        Class<T> type = resolver.type();
        Objects.requireNonNull(type, "resolver.type");
        resolvers.put(type, resolver);
    }

    public <T> void registerArgument(ArgumentResolver<T> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        register(new SingleArgumentParameterResolver<>(resolver));
    }

    public <T> Optional<ParameterResolver<T>> find(Class<T> type) {
        Objects.requireNonNull(type, "type");
        ParameterResolver<?> resolver = resolvers.get(type);
        if (resolver == null && type.isEnum()) {
            return enumResolver(type);
        }
        return cast(resolver);
    }

    private <T> Optional<ParameterResolver<T>> enumResolver(Class<T> type) {
        ParameterResolver<?> resolver = resolvers.computeIfAbsent(
                type, ignored -> new SingleArgumentParameterResolver<>(new EnumArgumentResolver<>(type)));
        return cast(resolver);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<ParameterResolver<T>> cast(@Nullable ParameterResolver<?> resolver) {
        return Optional.ofNullable((ParameterResolver<T>) resolver);
    }
}
