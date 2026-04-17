package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.AsyncExecutor;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandFramework;
import io.github.hanielcota.commandframework.CommandMiddleware;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.MessageProvider;
import io.github.hanielcota.commandframework.PlatformBridge;
import io.github.hanielcota.commandframework.RegisteredCommand;
import io.github.hanielcota.commandframework.annotation.Arg;
import io.github.hanielcota.commandframework.annotation.Async;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Inject;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.RequirePlayer;
import io.github.hanielcota.commandframework.annotation.Sender;
import io.github.hanielcota.commandframework.generated.CommandDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Builds the immutable command metadata used by the runtime.
 *
 * <p>Generated descriptors discovered through {@link ServiceLoader} are preferred for both
 * scan-package registration and manual command instances. Manual commands without generated
 * metadata still fall back to reflective introspection so existing direct registration keeps
 * working, but package scanning now relies on compile-time generated descriptors.
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
    private final AsyncExecutor asyncExecutor;
    private final List<String> scanPackages;
    private final List<Object> commandInstances;
    private final int rateLimitCommands;
    private final Duration rateLimitWindow;
    private final boolean debug;

    public InternalCommandBuilder(
            PlatformBridge<S> bridge,
            DependencyContainer dependencies,
            MessageProvider messageProvider,
            Map<MessageKey, String> messageOverrides,
            List<ArgumentResolver<?>> customResolvers,
            List<CommandMiddleware> middlewares,
            AsyncExecutor asyncExecutor,
            List<String> scanPackages,
            List<Object> commandInstances,
            int rateLimitCommands,
            Duration rateLimitWindow,
            boolean debug
    ) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
        this.messageProvider = Objects.requireNonNull(messageProvider, "messageProvider");
        this.messageOverrides = Map.copyOf(Objects.requireNonNull(messageOverrides, "messageOverrides"));
        this.customResolvers = List.copyOf(Objects.requireNonNull(customResolvers, "customResolvers"));
        this.middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.scanPackages = List.copyOf(Objects.requireNonNull(scanPackages, "scanPackages"));
        this.commandInstances = List.copyOf(Objects.requireNonNull(commandInstances, "commandInstances"));
        this.rateLimitCommands = rateLimitCommands;
        this.rateLimitWindow = Objects.requireNonNull(rateLimitWindow, "rateLimitWindow");
        this.debug = debug;
    }

    public CommandFramework<S> build() {
        Map<Class<?>, ArgumentResolver<?>> resolvers = new LinkedHashMap<>(DefaultArgumentResolvers.create());
        List<ArgumentResolver<?>> platformResolvers = this.bridge.platformResolvers();
        if (platformResolvers != null) {
            platformResolvers.forEach(resolver -> {
                Objects.requireNonNull(resolver.type(), () ->
                        "ArgumentResolver.type() must not return null in " + resolver.getClass().getName());
                resolvers.put(resolver.type(), resolver);
            });
        }
        this.customResolvers.forEach(resolver -> {
            Objects.requireNonNull(resolver.type(), () ->
                    "ArgumentResolver.type() must not return null in " + resolver.getClass().getName());
            resolvers.put(resolver.type(), resolver);
        });

        MessageProvider mergedProvider = key -> this.messageOverrides.getOrDefault(key, this.messageProvider.message(key));
        MessageService messages = new MessageService(mergedProvider);

        RuntimeAccessFactory accessFactory = new RuntimeAccessFactory(this.bridge.classLoader());
        Map<Class<?>, CommandDescriptor> generatedDescriptors = this.loadGeneratedDescriptors();

        List<Object> instances = new ArrayList<>(this.commandInstances);
        Set<Class<?>> knownTypes = new LinkedHashSet<>();
        for (Object instance : instances) {
            knownTypes.add(instance.getClass());
        }
        for (CommandDescriptor descriptor : this.scannedDescriptors(generatedDescriptors)) {
            Class<?> type = descriptor.commandType();
            if (knownTypes.add(type)) {
                instances.add(accessFactory.instantiate(type));
            }
        }
        if (instances.isEmpty()) {
            String message = this.scanPackages.isEmpty()
                    ? "No command classes found. Add the CommandFramework annotation processor to enable scanPackage()."
                    : "No command classes found for packages " + this.scannedPackageList()
                    + ". Add the CommandFramework annotation processor to enable scanPackage().";
            throw new IllegalStateException(message);
        }

        for (Object instance : instances) {
            this.injectFields(instance, generatedDescriptors.get(instance.getClass()), accessFactory);
        }

        Map<String, CommandDefinition> labels = new LinkedHashMap<>();
        Set<String> confirmationCommands = new LinkedHashSet<>();
        List<RegisteredCommand> registeredCommands = new ArrayList<>(instances.size());
        for (Object instance : instances) {
            CommandDescriptor descriptor = generatedDescriptors.get(instance.getClass());
            CommandDefinition definition = descriptor != null
                    ? this.buildGeneratedDefinition(instance, descriptor, resolvers, accessFactory)
                    : this.buildReflectiveDefinition(instance, resolvers, accessFactory);
            registeredCommands.add(new RegisteredCommand(definition.name(), definition.aliases(), definition.description()));
            confirmationCommands.addAll(definition.confirmationCommandNames());
            for (String label : definition.labels()) {
                String normalized = label.toLowerCase(Locale.ROOT);
                CommandDefinition previous = labels.putIfAbsent(normalized, definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate command label '" + normalized + "' — "
                            + "declared by " + previous.instance().getClass().getName()
                            + " and " + definition.instance().getClass().getName()
                            + ". Rename one of them or drop the duplicate alias.");
                }
            }
        }

        for (String confirmLabel : confirmationCommands) {
            CommandDefinition owner = labels.get(confirmLabel);
            if (owner != null) {
                throw new IllegalStateException("Confirmation command '" + confirmLabel
                        + "' collides with a registered command label in "
                        + owner.instance().getClass().getName()
                        + ". Change @Confirm(commandName=\"...\") on the affected executor.");
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
                this.bridge.logger(),
                this.asyncExecutor,
                this.debug
        );

        return new CommandFramework<>(
                this.bridge,
                registeredCommands,
                dispatcher,
                messages,
                new RateLimiter(this.rateLimitCommands, this.rateLimitWindow, this.bridge.logger())
        );
    }

    private Map<Class<?>, CommandDescriptor> loadGeneratedDescriptors() {
        Map<Class<?>, CommandDescriptor> descriptors = new LinkedHashMap<>();
        ServiceLoader.load(CommandDescriptor.class, this.bridge.classLoader()).forEach(descriptor -> {
            Class<?> type = descriptor.commandType();
            CommandDescriptor previous = descriptors.putIfAbsent(type, descriptor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate generated command descriptors for " + type.getName());
            }
        });
        return Map.copyOf(descriptors);
    }

    private List<CommandDescriptor> scannedDescriptors(Map<Class<?>, CommandDescriptor> descriptors) {
        List<String> packages = this.scanPackages.isEmpty()
                ? List.of(this.bridge.defaultScanPackage())
                : this.scanPackages;
        List<CommandDescriptor> matches = new ArrayList<>();
        for (CommandDescriptor descriptor : descriptors.values()) {
            if (this.matchesScanPackages(descriptor.packageName(), packages)) {
                matches.add(descriptor);
            }
        }
        return List.copyOf(matches);
    }

    private boolean matchesScanPackages(String packageName, List<String> packages) {
        for (String candidate : packages) {
            if (packageName.equals(candidate) || packageName.startsWith(candidate + ".")) {
                return true;
            }
        }
        return false;
    }

    private String scannedPackageList() {
        List<String> packages = this.scanPackages.isEmpty()
                ? List.of(this.bridge.defaultScanPackage())
                : this.scanPackages;
        return packages.toString();
    }

    private void injectFields(Object instance, CommandDescriptor descriptor, RuntimeAccessFactory accessFactory) {
        if (descriptor != null) {
            this.injectGeneratedFields(instance, descriptor.command().injectedFields(), accessFactory);
            return;
        }
        this.injectReflectiveFields(instance, accessFactory);
    }

    private void injectGeneratedFields(
            Object instance,
            List<CommandDescriptor.Field> fields,
            RuntimeAccessFactory accessFactory
    ) {
        for (CommandDescriptor.Field generatedField : fields) {
            Field field = accessFactory.field(generatedField.declaringClassName(), generatedField.fieldName());
            accessFactory.verifyInjectable(field);
            Class<?> fieldType = accessFactory.resolveClass(generatedField.fieldTypeName());
            Object dependency = this.dependencies.resolve(fieldType);
            if (dependency == null) {
                throw new IllegalStateException("No binding for " + fieldType.getName()
                        + " required by @Inject field " + field.getDeclaringClass().getName() + "#"
                        + field.getName()
                        + ". Register it via builder.bind(" + fieldType.getSimpleName() + ".class, instance).");
            }
            accessFactory.injectField(instance, field, dependency);
        }
    }

    private void injectReflectiveFields(Object instance, RuntimeAccessFactory accessFactory) {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                accessFactory.verifyInjectable(field);
                Object dependency = this.dependencies.resolve(field.getType());
                if (dependency == null) {
                    throw new IllegalStateException("No binding for " + field.getType().getName()
                            + " required by @Inject field " + current.getName() + "#" + field.getName()
                            + ". Register it via builder.bind(" + field.getType().getSimpleName() + ".class, instance).");
                }
                accessFactory.injectField(instance, field, dependency);
            }
            current = current.getSuperclass();
        }
    }

    private CommandDefinition buildGeneratedDefinition(
            Object instance,
            CommandDescriptor descriptor,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            RuntimeAccessFactory accessFactory
    ) {
        CommandDescriptor.Command command = descriptor.command();
        Class<?> type = instance.getClass();
        this.validateLabel(command.name(), "command name", type);
        List<String> aliases = new ArrayList<>(command.aliases().size());
        for (String alias : command.aliases()) {
            this.validateLabel(alias, "alias '" + alias + "'", type);
            aliases.add(alias);
        }

        ExecutorDefinition root = null;
        Map<String, ExecutorDefinition> executors = new LinkedHashMap<>();
        Set<String> confirmationCommands = new LinkedHashSet<>();

        for (CommandDescriptor.Executor generatedExecutor : command.executors()) {
            ExecutorDefinition definition = this.buildGeneratedExecutor(generatedExecutor, resolvers, accessFactory);
            if (definition.confirm() != null) {
                confirmationCommands.add(definition.confirm().commandName().toLowerCase(Locale.ROOT));
            }
            if (definition.subcommand().isEmpty()) {
                if (root != null) {
                    throw new IllegalStateException("Duplicate sub '' in " + type.getName());
                }
                root = definition;
                continue;
            }
            ExecutorDefinition previous = executors.putIfAbsent(definition.subcommand(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate sub '" + definition.subcommand() + "' in " + type.getName());
            }
        }

        if (root == null && executors.isEmpty()) {
            throw new IllegalStateException("No executors found in " + type.getName());
        }

        return new CommandDefinition(
                instance,
                command.name(),
                aliases,
                command.description(),
                root,
                Map.copyOf(executors),
                confirmationCommands
        );
    }

    private ExecutorDefinition buildGeneratedExecutor(
            CommandDescriptor.Executor executor,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            RuntimeAccessFactory accessFactory
    ) {
        String location = executor.declaringClassName() + "#" + executor.methodName();
        List<ParameterDefinition> parameters = this.generatedParameters(location, executor.parameters(), resolvers, accessFactory);
        List<Class<?>> parameterTypes = parameters.stream().map(ParameterDefinition::rawType).toList();
        Method method = accessFactory.method(executor.declaringClassName(), executor.methodName(), parameterTypes);
        if (!(method.getReturnType() == Void.TYPE || CommandResult.class.isAssignableFrom(method.getReturnType()))) {
            throw new IllegalStateException("Invalid return type " + method.getReturnType().getName()
                    + " in " + method);
        }
        String subcommand = executor.subcommand().trim().toLowerCase(Locale.ROOT);
        if (subcommand.contains(" ")) {
            throw new IllegalStateException("Sub-command paths must be a single token in " + location);
        }
        if (executor.async()) {
            for (ParameterDefinition parameter : parameters) {
                if (parameter.sender() && parameter.rawType() != CommandActor.class) {
                    throw new IllegalStateException("@Async sender parameters must use "
                            + CommandActor.class.getName() + " in " + location);
                }
            }
        }
        return new ExecutorDefinition(
                accessFactory.invoker(method),
                subcommand,
                executor.description(),
                executor.permission(),
                this.requirePlayer(executor.requirePlayer(), parameters),
                executor.async(),
                this.cooldown(executor.cooldown(), location),
                this.confirm(executor.confirm(), accessFactory.resolveClass(executor.declaringClassName())),
                parameters
        );
    }

    private List<ParameterDefinition> generatedParameters(
            String location,
            List<CommandDescriptor.Parameter> parameters,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            RuntimeAccessFactory accessFactory
    ) {
        List<ParameterDefinition> definitions = new ArrayList<>(parameters.size());
        for (int index = 0; index < parameters.size(); index++) {
            CommandDescriptor.Parameter parameter = parameters.get(index);
            Class<?> rawType = accessFactory.resolveClass(parameter.rawTypeName());
            Class<?> resolvedType = accessFactory.resolveClass(parameter.resolvedTypeName());
            if (parameter.sender()) {
                if (!this.bridge.supportsSenderType(rawType)) {
                    throw new IllegalStateException("Unsupported sender type " + rawType.getName()
                            + " in " + location);
                }
                definitions.add(new ParameterDefinition(
                        parameter.name(),
                        rawType,
                        resolvedType,
                        true,
                        false,
                        false,
                        io.github.hanielcota.commandframework.annotation.Optional.UNSET,
                        false,
                        0
                ));
                continue;
            }

            this.validateGeneratedArgument(
                    location,
                    rawType,
                    resolvedType,
                    parameter.greedy(),
                    parameter.maxLength(),
                    parameter.optional(),
                    parameter.optionalValue(),
                    index,
                    parameters.size(),
                    resolvers
            );
            definitions.add(new ParameterDefinition(
                    parameter.name(),
                    rawType,
                    resolvedType,
                    false,
                    parameter.javaOptional(),
                    parameter.optional(),
                    parameter.optionalValue(),
                    parameter.greedy(),
                    parameter.maxLength()
            ));
        }
        return List.copyOf(definitions);
    }

    private CommandDefinition buildReflectiveDefinition(
            Object instance,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            RuntimeAccessFactory accessFactory
    ) {
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
            if (!(method.getReturnType() == Void.TYPE || CommandResult.class.isAssignableFrom(method.getReturnType()))) {
                throw new IllegalStateException("Invalid return type " + method.getReturnType().getName()
                        + " in " + method);
            }

            String subcommand = execute.sub().trim().toLowerCase(Locale.ROOT);
            if (subcommand.contains(" ")) {
                throw new IllegalStateException("Sub-command paths must be a single token in " + method);
            }

            List<ParameterDefinition> parameters = this.parameters(method, resolvers);
            ExecutorDefinition definition = new ExecutorDefinition(
                    accessFactory.invoker(method),
                    subcommand,
                    this.description(method, command),
                    this.methodPermission(method, classPermission),
                    this.requirePlayer(method, classRequirePlayer, parameters),
                    method.isAnnotationPresent(Async.class),
                    this.cooldown(method),
                    this.confirm(method),
                    parameters
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

        this.validateReflectiveArgument(method, parameter, resolvedType, greedy, maxLength, optional, index, totalParameters, resolvers);

        String optionalValue = optional != null
                ? optional.value()
                : io.github.hanielcota.commandframework.annotation.Optional.UNSET;
        return new ParameterDefinition(
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

    private void validateReflectiveArgument(
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

    private void validateGeneratedArgument(
            String location,
            Class<?> rawType,
            Class<?> resolvedType,
            boolean greedy,
            int maxLength,
            boolean optional,
            String optionalValue,
            int index,
            int totalParameters,
            Map<Class<?>, ArgumentResolver<?>> resolvers
    ) {
        if (maxLength <= 0) {
            throw new IllegalStateException("@Arg maxLength must be > 0 in " + location);
        }
        if (greedy && index != totalParameters - 1) {
            throw new IllegalStateException("@Arg(greedy) must be last parameter in " + location);
        }
        if (greedy && resolvedType != String.class) {
            throw new IllegalStateException("@Arg(greedy) only supports String in " + location);
        }
        if (optional && rawType.isPrimitive()
                && io.github.hanielcota.commandframework.annotation.Optional.UNSET.equals(optionalValue)) {
            throw new IllegalStateException("@Optional on primitive " + rawType.getName()
                    + " requires default in " + location);
        }
        if (!this.hasResolver(resolvedType, resolvers)) {
            throw new IllegalStateException("No resolver for type " + resolvedType.getName() + " in " + location);
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

    private boolean requirePlayer(Method method, boolean classRequirePlayer, List<ParameterDefinition> parameters) {
        return this.requirePlayer(
                method.isAnnotationPresent(RequirePlayer.class) || classRequirePlayer,
                parameters
        );
    }

    private boolean requirePlayer(boolean explicitRequirePlayer, List<ParameterDefinition> parameters) {
        if (explicitRequirePlayer) {
            return true;
        }
        for (ParameterDefinition parameter : parameters) {
            if (parameter.sender() && this.bridge.isPlayerSenderType(parameter.rawType())) {
                return true;
            }
        }
        return false;
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

    private CooldownDefinition cooldown(CommandDescriptor.Cooldown cooldown, String location) {
        if (cooldown == null) {
            return null;
        }
        if (cooldown.value() <= 0) {
            throw new IllegalStateException("@Cooldown value must be > 0 in " + location);
        }
        return new CooldownDefinition(Duration.of(cooldown.value(), ChronoUnit.valueOf(cooldown.unitName())), cooldown.bypassPermission());
    }

    private ConfirmDefinition confirm(Method method) {
        Confirm confirm = method.getAnnotation(Confirm.class);
        if (confirm == null) {
            return null;
        }
        if (confirm.expireSeconds() <= 0) {
            throw new IllegalStateException("@Confirm expireSeconds must be > 0 in " + method);
        }
        String commandName = confirm.commandName().trim();
        this.validateLabel(commandName, "@Confirm command name", method.getDeclaringClass());
        return new ConfirmDefinition(Duration.ofSeconds(confirm.expireSeconds()), commandName);
    }

    private ConfirmDefinition confirm(CommandDescriptor.Confirm confirm, Class<?> ownerType) {
        if (confirm == null) {
            return null;
        }
        if (confirm.expireSeconds() <= 0) {
            throw new IllegalStateException("@Confirm expireSeconds must be > 0 in " + ownerType.getName());
        }
        String commandName = confirm.commandName().trim();
        this.validateLabel(commandName, "@Confirm command name", ownerType);
        return new ConfirmDefinition(Duration.ofSeconds(confirm.expireSeconds()), commandName);
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
