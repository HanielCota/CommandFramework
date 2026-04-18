package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolveException;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.PlatformBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Turns a selected executor plus raw argument tokens into a {@link PreparedInvocation} and binds
 * the final reflective call-site arguments, including sender placeholders.
 *
 * <p>Owns the resolver lookup table and the enum-resolver {@link ClassValue} cache so both the
 * dispatcher and the suggestion engine share a single resolver contract.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use. Mutable state is confined to the
 * {@link ClassValue} cache which is inherently thread-safe.
 */
final class ArgumentPreparer {

    private static final Object SENDER_ARGUMENT = new Object();

    private final Map<Class<?>, ArgumentResolver<?>> resolvers;
    private final PlatformBridge<?> bridge;
    // ClassValue ties the cached resolver to the enum Class's own lifetime: when a plugin is
    // reloaded and its ClassLoader is collected, the Class and its resolver entry are freed
    // together. Using a ConcurrentHashMap here would pin both in memory forever.
    @SuppressWarnings("unchecked")
    private final ClassValue<ArgumentResolver<Object>> enumResolverCache = new ClassValue<>() {
        @Override
        protected ArgumentResolver<Object> computeValue(Class<?> type) {
            return (ArgumentResolver<Object>) (ArgumentResolver<?>) DefaultArgumentResolvers.enumResolver(type);
        }
    };

    ArgumentPreparer(Map<Class<?>, ArgumentResolver<?>> resolvers, PlatformBridge<?> bridge) {
        this.resolvers = Objects.requireNonNull(resolvers, "resolvers");
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    /**
     * Resolves raw argument tokens into the prepared call-site values for the selected executor.
     *
     * @throws MissingArgumentException   if a required positional token is absent
     * @throws InvalidInputException      if an argument resolver rejects a token
     * @throws TooManyArgumentsException  if trailing tokens remain after all parameters are bound
     */
    PreparedInvocation prepare(CommandActor actor, CommandDefinition command, String label, Selection selection) {
        int parameterCount = selection.executor().parameters().size();
        List<Object> resolvedValues = new ArrayList<>(parameterCount);
        List<Object> previousArguments = new ArrayList<>(parameterCount);
        int tokenIndex = 0;

        for (ParameterDefinition parameter : selection.executor().parameters()) {
            if (parameter.sender()) {
                resolvedValues.add(SENDER_ARGUMENT);
                continue;
            }

            List<String> argumentTokens = selection.argumentTokens();
            int tokenCount = argumentTokens.size();
            String input = null;
            // Greedy consumes everything remaining and pins tokenIndex to the end; the follow-up
            // check below therefore always sees tokenIndex == tokenCount and skips. For non-greedy
            // parameters we fall through to the same check and take one token.
            if (parameter.greedy()) {
                if (tokenIndex < tokenCount) {
                    input = String.join(" ", argumentTokens.subList(tokenIndex, tokenCount));
                }
                tokenIndex = tokenCount;
            }
            if (tokenIndex < tokenCount) {
                input = argumentTokens.get(tokenIndex++);
            }

            if (input == null) {
                if (!parameter.optional()) {
                    throw new MissingArgumentException(parameter.name());
                }
                Object defaultValue = this.defaultValue(actor, label, selection.commandPath(), parameter, previousArguments);
                resolvedValues.add(defaultValue);
                previousArguments.add(defaultValue);
                continue;
            }

            if (input.length() > parameter.maxLength()) {
                throw new InvalidInputException(parameter.name(), input);
            }

            Object resolved = this.resolveArgument(actor, label, selection.commandPath(), parameter, input, previousArguments);
            resolvedValues.add(resolved);
            previousArguments.add(resolved);
        }
        if (tokenIndex < selection.argumentTokens().size()) {
            throw new TooManyArgumentsException(selection.argumentTokens().get(tokenIndex));
        }

        return new PreparedInvocation(command, selection.executor(), label, selection.commandPath(), resolvedValues);
    }

    private Object defaultValue(
            CommandActor actor,
            String label,
            String commandPath,
            ParameterDefinition parameter,
            List<Object> previousArguments
    ) {
        if (parameter.javaOptional()
                && io.github.hanielcota.commandframework.annotation.Optional.UNSET.equals(parameter.optionalValue())) {
            return Optional.empty();
        }

        if (io.github.hanielcota.commandframework.annotation.Optional.UNSET.equals(parameter.optionalValue())) {
            throw new MissingArgumentException(parameter.name());
        }

        // resolveArgument already wraps in Optional.of(...) when parameter.javaOptional() is true.
        // Do NOT re-wrap here - doing so yields Optional<Optional<X>> and breaks reflective invoke.
        return this.resolveArgument(actor, label, commandPath, parameter, parameter.optionalValue(), previousArguments);
    }

    private Object resolveArgument(
            CommandActor actor,
            String label,
            String commandPath,
            ParameterDefinition parameter,
            String input,
            List<Object> previousArguments
    ) {
        try {
            ArgumentResolutionContext context = new ArgumentResolutionContext(actor, label, commandPath, previousArguments);
            Object value = this.resolver(parameter.resolvedType()).resolve(context, input);
            if (value == null) {
                // Use getSimpleName() - keeps the diagnostic human-readable without leaking the
                // consumer's package layout if this message ever ends up in a user-facing surface.
                throw new ArgumentResolveException(
                        parameter.name(), input, "Resolver returned null for " + parameter.resolvedType().getSimpleName());
            }
            return parameter.javaOptional() ? Optional.of(value) : value;
        } catch (ArgumentResolveException exception) {
            throw new InvalidInputException(parameter.name(), input, exception);
        }
    }

    /**
     * Produces the final reflective argument array, substituting sender placeholders for the actor.
     *
     * @throws PlayerOnlySignal if the executor declares a player-only sender and the actor is not a player
     */
    Object[] bindArguments(CommandActor actor, PreparedInvocation invocation) {
        ExecutorDefinition executor = invocation.executor();
        List<Object> preparedArguments = invocation.preparedArguments();
        List<ParameterDefinition> parameters = executor.parameters();
        Object[] arguments = new Object[parameters.size()];
        for (int index = 0; index < parameters.size(); index++) {
            ParameterDefinition parameter = parameters.get(index);
            Object value = preparedArguments.get(index);
            if (value != SENDER_ARGUMENT) {
                arguments[index] = value;
                continue;
            }

            if (parameter.rawType().isInstance(actor.platformSender())) {
                arguments[index] = actor.platformSender();
                continue;
            }
            if (parameter.rawType().isAssignableFrom(actor.getClass())) {
                arguments[index] = actor;
                continue;
            }
            if (parameter.rawType() == CommandActor.class) {
                arguments[index] = actor;
                continue;
            }
            if (this.bridge.isPlayerSenderType(parameter.rawType())) {
                throw new PlayerOnlySignal();
            }
            throw new IllegalStateException("Unsupported sender type at runtime: " + parameter.rawType().getName());
        }
        return arguments;
    }

    @SuppressWarnings("unchecked")
    ArgumentResolver<Object> resolver(Class<?> type) {
        if (type.isEnum()) {
            return this.enumResolverCache.get(type);
        }
        ArgumentResolver<?> resolver = this.resolvers.get(type);
        if (resolver == null) {
            throw new IllegalStateException("No argument resolver registered for " + type.getName());
        }
        return (ArgumentResolver<Object>) resolver;
    }

    static final class MissingArgumentException extends RuntimeException {
        final String argumentName;

        MissingArgumentException(String argumentName) {
            this.argumentName = argumentName;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    static final class InvalidInputException extends RuntimeException {
        final String argumentName;
        final String input;

        InvalidInputException(String argumentName, String input) {
            this(argumentName, input, null);
        }

        InvalidInputException(String argumentName, String input, Throwable cause) {
            super(cause);
            this.argumentName = argumentName;
            this.input = input;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    static final class TooManyArgumentsException extends RuntimeException {
        final String input;

        TooManyArgumentsException(String input) {
            this.input = input;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    static final class PlayerOnlySignal extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
