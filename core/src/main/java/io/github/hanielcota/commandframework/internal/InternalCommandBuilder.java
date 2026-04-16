package io.github.hanielcota.commandframework.internal;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.hanielcota.commandframework.*;
import io.github.hanielcota.commandframework.annotation.*;

import java.lang.reflect.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Reflects annotated command classes into the immutable {@link CommandDefinition} / executor
 * metadata consumed by {@link CommandDispatcher}.
 *
 * <p>Responsibilities: scans the configured package (via ClassGraph) for {@link Command}-annotated
 * classes, instantiates them through {@link DependencyContainer}, validates method signatures and
 * argument names, and materialises cooldown, permission, confirmation and async metadata.
 *
 * <p>Validation is eager: unusable constructs (ambiguous sender parameters, missing
 * {@link Arg @Arg} names, malformed {@link Cooldown @Cooldown} durations) surface as
 * {@link IllegalStateException} at build time rather than at dispatch time.
 *
 * <p><b>Thread-safety:</b> a builder instance is intended for single-threaded use during framework
 * construction. The {@link RegisteredCommand}s it produces are immutable and safe to share.
 */
public final class InternalCommandBuilder<S> {

    private static final int DEFAULT_MAX_ARG_LENGTH = 256;
    private static final Pattern VALID_LABEL = Pattern.compile("[a-zA-Z0-9_-]+");

    private final PlatformBridge<S> bridge;
    private final DependencyContainer dependencies;
    private final MessageProvider messageProvider;
    private final Map<MessageKey, String> messageOverrides;
    private final List<ArgumentResolver<?>> customResolvers;
    private final List<CommandMiddleware> middlewares;
    private final List<String> scanPackages;
    private final List<Object> commandInstances;
    private final int rateLimitCommands;
    private final Duration rateLimitWindow;

    public InternalCommandBuilder(
            PlatformBridge<S> bridge,
            DependencyContainer dependencies,
            MessageProvider messageProvider,
            Map<MessageKey, String> messageOverrides,
            List<ArgumentResolver<?>> customResolvers,
            List<CommandMiddleware> middlewares,
            List<String> scanPackages,
            List<Object> commandInstances,
            int rateLimitCommands,
            Duration rateLimitWindow
    ) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        this.messageProvider = Objects.requireNonNull(messageProvider, "messageProvider");
        this.messageOverrides = Map.copyOf(Objects.requireNonNull(messageOverrides, "messageOverrides"));
        this.customResolvers = List.copyOf(Objects.requireNonNull(customResolvers, "customResolvers"));
        this.middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
        this.scanPackages = List.copyOf(Objects.requireNonNull(scanPackages, "scanPackages"));
        this.commandInstances = List.copyOf(Objects.requireNonNull(commandInstances, "commandInstances"));
        this.rateLimitCommands = rateLimitCommands;
        this.rateLimitWindow = Objects.requireNonNull(rateLimitWindow, "rateLimitWindow");
    }

    public CommandFramework<S> build() {
        Map<Class<?>, ArgumentResolver<?>> resolvers = new LinkedHashMap<>(DefaultArgumentResolvers.create());
        List<ArgumentResolver<?>> platformResolvers = this.bridge.platformResolvers();
        if (platformResolvers != null) {
            platformResolvers.forEach(resolver -> {
                Objects.requireNonNull(resolver.type(), () -> "ArgumentResolver.type() must not return null in " + resolver.getClass().getName());
                resolvers.put(resolver.type(), resolver);
            });
        }
        this.customResolvers.forEach(resolver -> {
            Objects.requireNonNull(resolver.type(), () -> "ArgumentResolver.type() must not return null in " + resolver.getClass().getName());
            resolvers.put(resolver.type(), resolver);
        });

        MessageProvider mergedProvider = key -> this.messageOverrides.getOrDefault(key, this.messageProvider.message(key));
        MessageService messages = new MessageService(mergedProvider);

        List<Object> instances = new ArrayList<>(this.commandInstances);
        Set<Class<?>> knownTypes = new LinkedHashSet<>();
        for (Object instance : instances) {
            knownTypes.add(instance.getClass());
        }
        for (Object scannedInstance : this.scanCommands()) {
            if (knownTypes.add(scannedInstance.getClass())) {
                instances.add(scannedInstance);
            }
        }
        if (instances.isEmpty()) {
            throw new IllegalStateException("No command classes found");
        }

        instances.forEach(this::injectFields);

        Map<String, CommandDefinition> labels = new LinkedHashMap<>();
        Set<String> confirmationCommands = new LinkedHashSet<>();
        List<RegisteredCommand> registeredCommands = new ArrayList<>(instances.size());
        for (Object instance : instances) {
            CommandDefinition definition = this.buildDefinition(instance, resolvers);
            registeredCommands.add(new RegisteredCommand(definition.name(), definition.aliases(), definition.description()));
            confirmationCommands.addAll(definition.confirmationCommandNames());
            for (String label : definition.labels()) {
                String normalized = label.toLowerCase(Locale.ROOT);
                CommandDefinition previous = labels.putIfAbsent(normalized, definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate command label '" + normalized + "'");
                }
            }
        }

        for (String confirmLabel : confirmationCommands) {
            if (labels.containsKey(confirmLabel)) {
                throw new IllegalStateException("Confirmation command '" + confirmLabel
                        + "' collides with a registered command label");
            }
        }

        CommandDispatcher dispatcher = new CommandDispatcher(
                this.bridge,
                labels,
                confirmationCommands,
                resolvers,
                this.middlewares,
                messages,
                new CommandTokenizer(),
                new CooldownManager(),
                new ConfirmationManager(),
                this.bridge.logger()
        );

        return new CommandFramework<>(
                this.bridge,
                registeredCommands,
                dispatcher,
                messages,
                new RateLimiter(this.rateLimitCommands, this.rateLimitWindow, this.bridge.logger())
        );
    }

    private List<Object> scanCommands() {
        List<String> packages = this.scanPackages.isEmpty()
                ? List.of(this.bridge.defaultScanPackage())
                : this.scanPackages;

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .overrideClassLoaders(this.bridge.classLoader())
                .acceptPackages(packages.toArray(String[]::new))
                .scan()) {
            List<Object> instances = new ArrayList<>();
            scanResult.getClassesWithAnnotation(Command.class.getName()).forEach(classInfo -> {
                Class<?> type = classInfo.loadClass();
                instances.add(this.instantiate(type));
            });
            return instances;
        }
    }

    private Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("No accessible no-arg constructor in " + type.getName(), exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to instantiate " + type.getName(), exception);
        }
    }

    private void injectFields(Object instance) {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalStateException("@Inject fields cannot be final: " + field);
                }

                Object dependency = this.dependencies.resolve(field.getType());
                if (dependency == null) {
                    throw new IllegalStateException("No binding for " + field.getType().getName()
                            + " required by " + instance.getClass().getName());
                }

                try {
                    field.setAccessible(true);
                    field.set(instance, dependency);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Unable to inject field " + field, exception);
                }
            }
            current = current.getSuperclass();
        }
    }

    private CommandDefinition buildDefinition(Object instance, Map<Class<?>, ArgumentResolver<?>> resolvers) {
        Class<?> type = instance.getClass();
        Command command = type.getAnnotation(Command.class);
        if (command == null) {
            throw new IllegalStateException("Missing @Command on " + type.getName());
        }

        String classPermission = this.classPermission(type, command);
        boolean classRequirePlayer = type.isAnnotationPresent(RequirePlayer.class);

        ExecutorDefinition root = null;
        Map<String, ExecutorDefinition> executors = new LinkedHashMap<>();
        Set<String> confirmationCommands = new LinkedHashSet<>();

        for (Method method : type.getDeclaredMethods()) {
            Execute execute = method.getAnnotation(Execute.class);
            if (execute == null) {
                continue;
            }
            method.setAccessible(true);

            if (!(method.getReturnType() == Void.TYPE || CommandResult.class.isAssignableFrom(method.getReturnType()))) {
                throw new IllegalStateException("Invalid return type " + method.getReturnType().getName()
                        + " in " + method);
            }

            String subcommand = execute.sub().trim().toLowerCase(Locale.ROOT);
            if (subcommand.contains(" ")) {
                throw new IllegalStateException("Sub-command paths must be a single token in " + method);
            }

            ExecutorDefinition definition = new ExecutorDefinition(
                    method,
                    subcommand,
                    this.description(method, command),
                    this.methodPermission(method, classPermission),
                    this.requirePlayer(method, classRequirePlayer),
                    method.isAnnotationPresent(Async.class),
                    this.cooldown(method),
                    this.confirm(method),
                    this.parameters(method, resolvers)
            );

            if (definition.confirm() != null) {
                confirmationCommands.add(definition.confirm().commandName().toLowerCase(Locale.ROOT));
            }

            if (subcommand.isEmpty()) {
                if (root != null) {
                    throw new IllegalStateException("Duplicate sub '' in " + type.getName());
                }
                root = definition;
                continue;
            }

            ExecutorDefinition previous = executors.putIfAbsent(subcommand, definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate sub '" + subcommand + "' in " + type.getName());
            }
        }

        if (root == null && executors.isEmpty()) {
            throw new IllegalStateException("No executors found in " + type.getName());
        }

        String commandName = command.name().trim();
        this.validateLabel(commandName, "command name", type);
        List<String> aliases = new ArrayList<>(command.aliases().length);
        for (String alias : command.aliases()) {
            String trimmed = alias.trim();
            this.validateLabel(trimmed, "alias '" + alias + "'", type);
            aliases.add(trimmed);
        }

        return new CommandDefinition(
                instance,
                commandName,
                aliases,
                command.description(),
                root,
                Map.copyOf(executors),
                confirmationCommands
        );
    }

    private List<ParameterDefinition> parameters(Method method, Map<Class<?>, ArgumentResolver<?>> resolvers) {
        Parameter[] parameters = method.getParameters();
        List<ParameterDefinition> definitions = new ArrayList<>(parameters.length);
        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            if (parameter.isAnnotationPresent(Sender.class)) {
                definitions.add(this.senderDefinition(method, parameter));
            } else {
                definitions.add(this.argumentDefinition(method, parameter, resolvers, index, parameters.length));
            }
        }
        return List.copyOf(definitions);
    }

    private ParameterDefinition senderDefinition(Method method, Parameter parameter) {
        if (!this.bridge.supportsSenderType(parameter.getType())) {
            throw new IllegalStateException("Unsupported sender type " + parameter.getType().getName()
                    + " in " + method);
        }
        if (method.isAnnotationPresent(Async.class) && parameter.getType() != CommandActor.class) {
            throw new IllegalStateException("@Async sender parameters must use "
                    + CommandActor.class.getName() + " in " + method);
        }
        return new ParameterDefinition(
                parameter,
                parameter.getName(),
                parameter.getType(),
                parameter.getType(),
                true,
                false,
                false,
                io.github.hanielcota.commandframework.annotation.Optional.UNSET,
                false,
                0
        );
    }

    private ParameterDefinition argumentDefinition(
            Method method,
            Parameter parameter,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            int index,
            int totalParameters
    ) {
        Arg arg = parameter.getAnnotation(Arg.class);
        io.github.hanielcota.commandframework.annotation.Optional optional =
                parameter.getAnnotation(io.github.hanielcota.commandframework.annotation.Optional.class);
        boolean javaOptional = parameter.getType() == Optional.class;
        Class<?> resolvedType = javaOptional ? this.optionalType(method, parameter) : parameter.getType();
        String name = arg != null && !arg.value().isBlank() ? arg.value() : parameter.getName();
        boolean greedy = arg != null && arg.greedy();
        int maxLength = arg != null ? arg.maxLength() : DEFAULT_MAX_ARG_LENGTH;

        this.validateArgument(method, parameter, resolvedType, greedy, maxLength, optional, index, totalParameters, resolvers);

        String optionalValue = optional != null
                ? optional.value()
                : io.github.hanielcota.commandframework.annotation.Optional.UNSET;
        return new ParameterDefinition(
                parameter,
                name,
                parameter.getType(),
                resolvedType,
                false,
                javaOptional,
                optional != null || javaOptional,
                optionalValue,
                greedy,
                maxLength
        );
    }

    private void validateArgument(
            Method method,
            Parameter parameter,
            Class<?> resolvedType,
            boolean greedy,
            int maxLength,
            io.github.hanielcota.commandframework.annotation.Optional optional,
            int index,
            int totalParameters,
            Map<Class<?>, ArgumentResolver<?>> resolvers
    ) {
        if (maxLength <= 0) {
            throw new IllegalStateException("@Arg maxLength must be > 0 in " + method);
        }
        if (greedy && index != totalParameters - 1) {
            throw new IllegalStateException("@Arg(greedy) must be last parameter in " + method);
        }
        if (greedy && resolvedType != String.class) {
            throw new IllegalStateException("@Arg(greedy) only supports String in " + method);
        }
        if (optional != null && parameter.getType().isPrimitive()
                && io.github.hanielcota.commandframework.annotation.Optional.UNSET.equals(optional.value())) {
            throw new IllegalStateException("@Optional on primitive " + parameter.getType().getName()
                    + " requires default in " + method);
        }
        if (!this.hasResolver(resolvedType, resolvers)) {
            throw new IllegalStateException("No resolver for type " + resolvedType.getName() + " in " + method);
        }
    }

    private void validateLabel(String label, String context, Class<?> type) {
        if (label.isEmpty()) {
            throw new IllegalStateException("Empty " + context + " in " + type.getName());
        }
        if (!VALID_LABEL.matcher(label).matches()) {
            throw new IllegalStateException("Invalid " + context + " '" + label + "' in " + type.getName()
                    + " (only alphanumeric, hyphens and underscores are allowed)");
        }
    }

    private boolean hasResolver(Class<?> type, Map<Class<?>, ArgumentResolver<?>> resolvers) {
        return resolvers.containsKey(type) || type.isEnum();
    }

    private Class<?> optionalType(Method method, Parameter parameter) {
        Type genericType = parameter.getParameterizedType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type argument = parameterizedType.getActualTypeArguments()[0];
            if (argument instanceof Class<?> argumentClass) {
                return argumentClass;
            }
        }
        throw new IllegalStateException("No resolver for type java.util.Optional in " + method);
    }

    private String classPermission(Class<?> type, Command command) {
        Permission permission = type.getAnnotation(Permission.class);
        if (permission != null) {
            return permission.value();
        }
        return command.permission();
    }

    private String methodPermission(Method method, String classPermission) {
        Permission permission = method.getAnnotation(Permission.class);
        return permission != null ? permission.value() : classPermission;
    }

    private boolean requirePlayer(Method method, boolean classRequirePlayer) {
        return method.isAnnotationPresent(RequirePlayer.class) || classRequirePlayer;
    }

    private String description(Method method, Command command) {
        Description description = method.getAnnotation(Description.class);
        return description != null ? description.value() : command.description();
    }

    private CooldownDefinition cooldown(Method method) {
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        if (cooldown == null) {
            return null;
        }
        if (cooldown.value() <= 0) {
            throw new IllegalStateException("@Cooldown value must be > 0 in " + method);
        }
        return new CooldownDefinition(Duration.of(cooldown.value(), this.toChronoUnit(cooldown.unit())), cooldown.bypassPermission());
    }

    private ConfirmDefinition confirm(Method method) {
        Confirm confirm = method.getAnnotation(Confirm.class);
        if (confirm == null) {
            return null;
        }
        if (confirm.expireSeconds() <= 0) {
            throw new IllegalStateException("@Confirm expireSeconds must be > 0 in " + method);
        }
        return new ConfirmDefinition(Duration.ofSeconds(confirm.expireSeconds()), confirm.commandName().trim());
    }

    private ChronoUnit toChronoUnit(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> ChronoUnit.NANOS;
            case MICROSECONDS -> ChronoUnit.MICROS;
            case MILLISECONDS -> ChronoUnit.MILLIS;
            case SECONDS -> ChronoUnit.SECONDS;
            case MINUTES -> ChronoUnit.MINUTES;
            case HOURS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.DAYS;
        };
    }
}
