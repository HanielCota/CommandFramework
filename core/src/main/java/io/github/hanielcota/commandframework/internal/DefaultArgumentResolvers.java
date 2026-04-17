package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolveException;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Built-in {@link ArgumentResolver} factory for the primitive types and {@link UUID}.
 *
 * <p>Resolvers are inserted in a well-defined order into a {@link LinkedHashMap} so that framework
 * consumers can still override any type by rebinding it after {@link #create()} returns. Decimal
 * parsers additionally reject non-finite or implausibly large values to protect downstream code
 * from overflow-style inputs.
 *
 * <p><b>Thread-safety:</b> the factory and returned resolvers are stateless and safe for
 * concurrent use.
 */
public final class DefaultArgumentResolvers {

    private static final double MAX_DECIMAL_ABS = 1_000_000_000_000D;

    private DefaultArgumentResolvers() {
    }

    public static Map<Class<?>, ArgumentResolver<?>> create() {
        Map<Class<?>, ArgumentResolver<?>> resolvers = new LinkedHashMap<>();
        ArgumentResolver<String> stringResolver = simple(String.class, input -> input);
        ArgumentResolver<Boolean> booleanResolver = booleanResolver();
        ArgumentResolver<Integer> integerResolver = simple(Integer.class, DefaultArgumentResolvers::parseInteger);
        ArgumentResolver<Long> longResolver = simple(Long.class, DefaultArgumentResolvers::parseLong);
        ArgumentResolver<Double> doubleResolver = simple(Double.class, DefaultArgumentResolvers::parseDouble);
        ArgumentResolver<Float> floatResolver = simple(Float.class, DefaultArgumentResolvers::parseFloat);
        ArgumentResolver<UUID> uuidResolver = simple(UUID.class, UUID::fromString);

        register(resolvers, String.class, stringResolver);
        register(resolvers, Boolean.class, booleanResolver);
        register(resolvers, boolean.class, booleanResolver);
        register(resolvers, Integer.class, integerResolver);
        register(resolvers, int.class, integerResolver);
        register(resolvers, Long.class, longResolver);
        register(resolvers, long.class, longResolver);
        register(resolvers, Double.class, doubleResolver);
        register(resolvers, double.class, doubleResolver);
        register(resolvers, Float.class, floatResolver);
        register(resolvers, float.class, floatResolver);
        register(resolvers, UUID.class, uuidResolver);
        return Map.copyOf(resolvers);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> ArgumentResolver<E> enumResolver(Class<?> enumType) {
        Class<E> typedClass = (Class<E>) enumType;
        return new ArgumentResolver<>() {
            @Override
            public Class<E> type() {
                return typedClass;
            }

            @Override
            public E resolve(ArgumentResolutionContext context, String input) throws ArgumentResolveException {
                try {
                    return Enum.valueOf(typedClass, input.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw new ArgumentResolveException("enum", input, "Invalid enum constant", exception);
                }
            }

            @Override
            public List<String> suggest(CommandActor actor, String currentInput) {
                String lowered = currentInput.toLowerCase(Locale.ROOT);
                return Arrays.stream(typedClass.getEnumConstants())
                        .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                        .filter(name -> name.startsWith(lowered))
                        .toList();
            }
        };
    }

    private static <T> void register(Map<Class<?>, ArgumentResolver<?>> resolvers, Class<?> key, ArgumentResolver<T> resolver) {
        resolvers.put(key, resolver);
    }

    private static ArgumentResolver<Boolean> booleanResolver() {
        return new ArgumentResolver<>() {
            @Override
            public Class<Boolean> type() {
                return Boolean.class;
            }

            @Override
            public Boolean resolve(ArgumentResolutionContext context, String input) throws ArgumentResolveException {
                if (input.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                if (input.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                }
                throw new ArgumentResolveException("boolean", input, "Expected true or false");
            }

            @Override
            public List<String> suggest(CommandActor actor, String currentInput) {
                String lowered = currentInput.toLowerCase(Locale.ROOT);
                return Stream.of("true", "false").filter(option -> option.startsWith(lowered)).toList();
            }
        };
    }

    private static <T> ArgumentResolver<T> simple(Class<T> type, ThrowingFunction<String, T> mapper) {
        return new ArgumentResolver<>() {
            @Override
            public Class<T> type() {
                return type;
            }

            @Override
            public T resolve(ArgumentResolutionContext context, String input) throws ArgumentResolveException {
                try {
                    return mapper.apply(input);
                } catch (ArgumentResolveException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw new ArgumentResolveException(type.getSimpleName(), input, "Unable to resolve argument", exception);
                }
            }
        };
    }

    private static Integer parseInteger(String input) throws ArgumentResolveException {
        try {
            return Integer.valueOf(input);
        } catch (NumberFormatException exception) {
            throw new ArgumentResolveException("integer", input, "Invalid integer", exception);
        }
    }

    private static Long parseLong(String input) throws ArgumentResolveException {
        try {
            return Long.valueOf(input);
        } catch (NumberFormatException exception) {
            throw new ArgumentResolveException("long", input, "Invalid long", exception);
        }
    }

    private static Double parseDouble(String input) throws ArgumentResolveException {
        try {
            double value = Double.parseDouble(input);
            if (!Double.isFinite(value) || Math.abs(value) > MAX_DECIMAL_ABS) {
                throw new ArgumentResolveException("double", input, "Invalid decimal");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new ArgumentResolveException("double", input, "Invalid decimal", exception);
        }
    }

    private static Float parseFloat(String input) throws ArgumentResolveException {
        try {
            float value = Float.parseFloat(input);
            if (!Float.isFinite(value) || Math.abs(value) > MAX_DECIMAL_ABS) {
                throw new ArgumentResolveException("float", input, "Invalid decimal");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new ArgumentResolveException("float", input, "Invalid decimal", exception);
        }
    }

    @FunctionalInterface
    private interface ThrowingFunction<I, O> {
        O apply(I input) throws Exception;
    }
}
