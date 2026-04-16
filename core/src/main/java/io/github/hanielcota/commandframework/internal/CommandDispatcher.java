package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.*;
import net.kyori.adventure.text.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pipeline orchestrator that dispatches a parsed command invocation through its stages:
 * permission check, player-only check, cooldown, argument parsing, confirmation and execution.
 *
 * <p>A single instance is shared by all commands registered in a framework. It holds no per-call
 * state — all request-scoped state lives in {@link CommandContext} arguments passed through the
 * pipeline.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use. Mutable collaborator state is confined to
 * {@link ConcurrentHashMap} instances owned by the dispatcher and to the platform-provided
 * {@link CommandMiddleware} chain; middlewares themselves must be thread-safe.
 */
public final class CommandDispatcher {

    private static final Object SENDER_ARGUMENT = new Object();

    private final PlatformBridge<?> bridge;
    private final Map<String, CommandDefinition> commandsByLabel;
    private final Set<String> confirmationCommands;
    private final Map<Class<?>, ArgumentResolver<?>> resolvers;
    private final List<CommandMiddleware> middlewares;
    private final MessageService messages;
    private final CommandTokenizer tokenizer;
    private final CooldownManager cooldownManager;
    private final ConfirmationManager confirmationManager;
    private final Logger logger;
    private final Map<Class<?>, ArgumentResolver<Object>> enumResolverCache = new ConcurrentHashMap<>();

    CommandDispatcher(
            PlatformBridge<?> bridge,
            Map<String, CommandDefinition> commandsByLabel,
            Set<String> confirmationCommands,
            Map<Class<?>, ArgumentResolver<?>> resolvers,
            List<CommandMiddleware> middlewares,
            MessageService messages,
            CommandTokenizer tokenizer,
            CooldownManager cooldownManager,
            ConfirmationManager confirmationManager,
            Logger logger
    ) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.commandsByLabel = Map.copyOf(Objects.requireNonNull(commandsByLabel, "commandsByLabel"));
        this.confirmationCommands = Set.copyOf(Objects.requireNonNull(confirmationCommands, "confirmationCommands"));
        this.resolvers = Map.copyOf(Objects.requireNonNull(resolvers, "resolvers"));
        this.middlewares = List.copyOf(Objects.requireNonNull(middlewares, "middlewares"));
        this.messages = Objects.requireNonNull(messages, "messages");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
        this.cooldownManager = Objects.requireNonNull(cooldownManager, "cooldownManager");
        this.confirmationManager = Objects.requireNonNull(confirmationManager, "confirmationManager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public CommandResult dispatch(CommandActor actor, String label, String rawArguments) {
        try {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            if (this.confirmationCommands.contains(normalizedLabel)) {
                return this.dispatchConfirmation(actor, normalizedLabel);
            }

            CommandDefinition command = this.commandsByLabel.get(normalizedLabel);
            if (command == null) {
                return CommandResult.handled();
            }

            TokenizedInput tokenizedInput = this.tokenizer.tokenize(rawArguments);
            Selection selection = this.select(command, tokenizedInput);
            if (selection == null) {
                this.sendHelp(actor, command, label);
                return new CommandResult.HelpShown();
            }

            CommandContext context = new CommandContext(actor, label, rawArguments, selection.argumentTokens(), selection.commandPath());
            CommandResult result = this.invokeMiddleware(0, context, ctx -> this.executeSelection(ctx.actor(), command, ctx.label(), selection));
            return this.emit(actor, Objects.requireNonNull(result, "Middleware chain must not return null"));
        } catch (MissingArgumentException exception) {
            this.messages.send(actor, MessageKey.MISSING_ARGUMENT, Map.of("name", exception.argumentName));
            return CommandResult.handled();
        } catch (InvalidInputException exception) {
            return this.emit(actor, new CommandResult.InvalidArgs(exception.argumentName, exception.input));
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Unhandled command exception while dispatching " + label, exception);
            return this.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
        }
    }

    public CommandResult dispatchConfirmation(CommandActor actor, String label) {
        PreparedInvocation invocation = this.confirmationManager.consume(actor, label);
        if (invocation == null) {
            return this.emit(actor, CommandResult.failure(MessageKey.CONFIRM_NOTHING_PENDING));
        }
        try {
            CommandResult result = this.executePrepared(actor, invocation);
            return this.emit(actor, result);
        } catch (RuntimeException exception) {
            this.logger.log(Level.SEVERE, "Unhandled command exception while confirming " + label, exception);
            return this.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
        }
    }

    public List<String> suggest(CommandActor actor, String label, String rawArguments) {
        try {
            CommandDefinition command = this.commandsByLabel.get(label.toLowerCase(Locale.ROOT));
            if (command == null) {
                return List.of();
            }
            TokenizedInput tokenizedInput = this.tokenizer.tokenize(rawArguments);
            if (tokenizedInput.tokens().isEmpty()) {
                return this.emptyTokenSuggestions(actor, command, tokenizedInput.trailingSpace());
            }

            String firstToken = tokenizedInput.tokens().getFirst().toLowerCase(Locale.ROOT);
            ExecutorDefinition subcommand = command.executorsBySubcommand().get(firstToken);
            if (subcommand == null) {
                return this.rootExecutorSuggestions(actor, command, tokenizedInput, firstToken);
            }

            if (tokenizedInput.tokens().size() == 1 && !tokenizedInput.trailingSpace()) {
                return this.subcommandSuggestions(actor, command, firstToken);
            }
            if (!this.allowed(actor, subcommand)) {
                return List.of();
            }
            return this.argumentSuggestions(actor, subcommand, tokenizedInput.tokens(), tokenizedInput.trailingSpace(), 1);
        } catch (RuntimeException exception) {
            this.logger.log(Level.WARNING, "Exception during tab completion for " + label, exception);
            return List.of();
        }
    }

    private List<String> emptyTokenSuggestions(CommandActor actor, CommandDefinition command, boolean trailingSpace) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>(this.subcommandSuggestions(actor, command, ""));
        ExecutorDefinition rootExecutor = command.rootExecutor();
        if (trailingSpace && rootExecutor != null && this.allowed(actor, rootExecutor)) {
            suggestions.addAll(this.argumentSuggestions(actor, rootExecutor, List.of(), true, 0));
        }
        return List.copyOf(suggestions);
    }

    private List<String> rootExecutorSuggestions(
            CommandActor actor,
            CommandDefinition command,
            TokenizedInput tokenizedInput,
            String firstToken
    ) {
        ExecutorDefinition rootExecutor = command.rootExecutor();
        if (!tokenizedInput.trailingSpace()) {
            List<String> subSuggestions = this.subcommandSuggestions(actor, command, firstToken);
            if (!subSuggestions.isEmpty() || rootExecutor == null) {
                return subSuggestions;
            }
        }
        if (rootExecutor == null || !this.allowed(actor, rootExecutor)) {
            return List.of();
        }
        return this.argumentSuggestions(actor, rootExecutor, tokenizedInput.tokens(), tokenizedInput.trailingSpace(), 0);
    }

    public ConfirmationManager confirmationManager() {
        return this.confirmationManager;
    }

    public Set<String> commandLabels() {
        return this.commandsByLabel.keySet();
    }

    public Set<String> confirmationCommandLabels() {
        return this.confirmationCommands;
    }

    public CooldownManager cooldownManager() {
        return this.cooldownManager;
    }

    private CommandResult invokeMiddleware(int index, CommandContext context, CommandMiddleware.Chain terminal) {
        Objects.requireNonNull(context, "Middleware passed null context to chain");
        if (index >= this.middlewares.size()) {
            return terminal.proceed(context);
        }
        CommandMiddleware middleware = this.middlewares.get(index);
        return middleware.handle(context, next -> this.invokeMiddleware(index + 1, next, terminal));
    }

    private CommandResult executeSelection(CommandActor actor, CommandDefinition command, String label, Selection selection) {
        ExecutorDefinition executor = selection.executor();
        if (executor.requirePlayer() && !actor.isPlayer()) {
            return new CommandResult.PlayerOnly();
        }
        if (!executor.permission().isBlank() && !actor.hasPermission(executor.permission())) {
            return new CommandResult.NoPermission(executor.permission());
        }

        // Cooldown is evaluated AFTER argument parsing so that a syntax error (MissingArgumentException /
        // ArgumentResolveException raised from prepareInvocation) does not consume the sender's cooldown
        // window. Otherwise a user who typo'd arguments would be locked out on their legitimate retry.
        PreparedInvocation invocation = this.prepareInvocation(actor, command, label, selection);

        if (executor.cooldown() != null) {
            CooldownManager.CooldownResult cooldownResult =
                    this.cooldownManager.checkAndConsume(selection.commandPath(), actor, executor.cooldown());
            if (!cooldownResult.allowed()) {
                return new CommandResult.CooldownActive(cooldownResult.remaining());
            }
        }

        if (executor.confirm() != null) {
            this.confirmationManager.put(actor, invocation, executor.confirm());
            return new CommandResult.PendingConfirmation(executor.confirm().commandName(), executor.confirm().expiresIn());
        }
        return this.executePrepared(actor, invocation);
    }

    private PreparedInvocation prepareInvocation(CommandActor actor, CommandDefinition command, String label, Selection selection) {
        List<Object> resolvedValues = new ArrayList<>();
        List<Object> previousArguments = new ArrayList<>();
        int tokenIndex = 0;

        for (ParameterDefinition parameter : selection.executor().parameters()) {
            if (parameter.sender()) {
                resolvedValues.add(SENDER_ARGUMENT);
                continue;
            }

            String input;
            if (parameter.greedy()) {
                input = tokenIndex >= selection.argumentTokens().size()
                        ? null
                        : String.join(" ", selection.argumentTokens().subList(tokenIndex, selection.argumentTokens().size()));
                tokenIndex = selection.argumentTokens().size();
            } else if (tokenIndex < selection.argumentTokens().size()) {
                input = selection.argumentTokens().get(tokenIndex++);
            } else {
                input = null;
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

        Object resolved = this.resolveArgument(actor, label, commandPath, parameter, parameter.optionalValue(), previousArguments);
        return parameter.javaOptional() ? Optional.of(resolved) : resolved;
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
                throw new ArgumentResolveException(
                        parameter.name(), input, "Resolver returned null for " + parameter.resolvedType().getName());
            }
            return parameter.javaOptional() ? Optional.of(value) : value;
        } catch (ArgumentResolveException exception) {
            throw new InvalidInputException(parameter.name(), input, exception);
        }
    }

    private CommandResult executePrepared(CommandActor actor, PreparedInvocation invocation) {
        if (invocation.executor().async()) {
            Thread.ofVirtual().name("commandframework-" + invocation.commandPath()).start(() -> {
                try {
                    CommandResult result = this.invokeMethod(actor, invocation);
                    this.emit(actor, result);
                } catch (Exception exception) {
                    this.logger.log(Level.SEVERE, "Unhandled exception in async command " + invocation.commandPath(), exception);
                }
            });
            return CommandResult.success();
        }
        return this.invokeMethod(actor, invocation);
    }

    private CommandResult invokeMethod(CommandActor actor, PreparedInvocation invocation) {
        try {
            Object[] arguments = this.buildArguments(actor, invocation.executor(), invocation.preparedArguments());
            Object result = invocation.executor().method().invoke(invocation.command().instance(), arguments);
            if (result == null) {
                return CommandResult.success();
            }
            return (CommandResult) result;
        } catch (PlayerOnlySignal ignored) {
            return new CommandResult.PlayerOnly();
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getTargetException() != null ? exception.getTargetException() : exception;
            this.logger.log(Level.SEVERE, "Unhandled command exception in " + invocation.commandPath(), cause);
            return CommandResult.failure(MessageKey.COMMAND_ERROR);
        } catch (ReflectiveOperationException exception) {
            this.logger.log(Level.SEVERE, "Unable to invoke command " + invocation.commandPath(), exception);
            return CommandResult.failure(MessageKey.COMMAND_ERROR);
        }
    }

    private Object[] buildArguments(CommandActor actor, ExecutorDefinition executor, List<Object> preparedArguments) {
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

    private Selection select(CommandDefinition command, TokenizedInput tokenizedInput) {
        if (tokenizedInput.tokens().isEmpty()) {
            return command.rootExecutor() == null ? null : new Selection(command.rootExecutor(), List.of(), command.name());
        }

        String firstToken = tokenizedInput.tokens().getFirst().toLowerCase(Locale.ROOT);
        ExecutorDefinition subcommand = command.executorsBySubcommand().get(firstToken);
        if (subcommand != null) {
            return new Selection(
                    subcommand,
                    tokenizedInput.tokens().subList(1, tokenizedInput.tokens().size()),
                    command.name() + " " + subcommand.subcommand()
            );
        }

        if (command.rootExecutor() == null) {
            return null;
        }
        return new Selection(command.rootExecutor(), tokenizedInput.tokens(), command.name());
    }

    private List<String> subcommandSuggestions(CommandActor actor, CommandDefinition command, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<String, ExecutorDefinition> entry : command.executorsBySubcommand().entrySet()) {
            if (!entry.getKey().startsWith(lowered)) {
                continue;
            }
            if (!this.allowed(actor, entry.getValue())) {
                continue;
            }
            suggestions.add(entry.getKey());
        }
        return List.copyOf(suggestions);
    }

    private List<String> argumentSuggestions(
            CommandActor actor,
            ExecutorDefinition executor,
            List<String> tokens,
            boolean trailingSpace,
            int tokenOffset
    ) {
        List<ParameterDefinition> arguments = executor.parameters().stream().filter(parameter -> !parameter.sender()).toList();
        if (arguments.isEmpty()) {
            return List.of();
        }

        int consumed = trailingSpace ? tokens.size() - tokenOffset : tokens.size() - tokenOffset - 1;
        if (consumed < 0) {
            consumed = 0;
        }
        int parameterIndex = Math.min(consumed, arguments.size() - 1);
        ParameterDefinition parameter = arguments.get(parameterIndex);
        if (parameter.greedy()) {
            return List.of();
        }

        String currentInput = trailingSpace ? "" : tokens.getLast();
        List<String> suggestions = this.resolver(parameter.resolvedType()).suggest(actor, currentInput);
        return suggestions != null ? suggestions : List.of();
    }

    private boolean allowed(CommandActor actor, ExecutorDefinition executor) {
        return executor.permission().isBlank() || actor.hasPermission(executor.permission());
    }

    private CommandResult emit(CommandActor actor, CommandResult result) {
        switch (result) {
            case CommandResult.Failure(MessageKey key, Map<String, String> placeholders) ->
                this.messages.send(actor, key, placeholders);
            case CommandResult.InvalidArgs(String argumentName, String input) ->
                this.messages.send(actor, MessageKey.INVALID_ARGUMENT, Map.of(
                        "name", argumentName,
                        "input", input
                ));
            case CommandResult.NoPermission ignored ->
                this.messages.send(actor, MessageKey.NO_PERMISSION);
            case CommandResult.PlayerOnly ignored ->
                this.messages.send(actor, MessageKey.PLAYER_ONLY);
            case CommandResult.CooldownActive(Duration remaining) ->
                this.messages.send(actor, MessageKey.COOLDOWN_ACTIVE, Map.of(
                        "remaining", this.messages.formatDuration(remaining)
                ));
            case CommandResult.PendingConfirmation(String commandName, Duration expiresIn) ->
                this.messages.send(actor, MessageKey.CONFIRM_PROMPT, Map.of(
                        "command", commandName,
                        "seconds", String.valueOf(Math.max(1L, expiresIn.toSeconds()))
                ));
            default -> { /* Success / Handled / HelpShown / RateLimited — nothing to emit */ }
        }
        return result;
    }

    private void sendHelp(CommandActor actor, CommandDefinition command, String label) {
        List<Component> lines = new ArrayList<>();
        lines.add(this.messages.render(MessageKey.HELP_HEADER, Map.of("command", label)));

        if (command.rootExecutor() != null && this.allowed(actor, command.rootExecutor())) {
            lines.add(this.messages.render(MessageKey.HELP_ENTRY, Map.of(
                    "usage", label,
                    "description", command.rootExecutor().description()
            )));
        }

        for (Map.Entry<String, ExecutorDefinition> entry : command.executorsBySubcommand().entrySet()) {
            if (!this.allowed(actor, entry.getValue())) {
                continue;
            }
            lines.add(this.messages.render(MessageKey.HELP_ENTRY, Map.of(
                    "usage", label + " " + entry.getKey(),
                    "description", entry.getValue().description()
            )));
        }

        actor.sendMessage(this.messages.renderLines(lines.toArray(Component[]::new)));
    }

    @SuppressWarnings("unchecked")
    private ArgumentResolver<Object> resolver(Class<?> type) {
        if (type.isEnum()) {
            return this.enumResolverCache.computeIfAbsent(type,
                    t -> (ArgumentResolver<Object>) (ArgumentResolver<?>) DefaultArgumentResolvers.enumResolver(t));
        }
        ArgumentResolver<?> resolver = this.resolvers.get(type);
        if (resolver == null) {
            throw new IllegalStateException("No argument resolver registered for " + type.getName());
        }
        return (ArgumentResolver<Object>) resolver;
    }

    private static final class MissingArgumentException extends RuntimeException {
        private final String argumentName;

        private MissingArgumentException(String argumentName) {
            this.argumentName = argumentName;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class InvalidInputException extends RuntimeException {
        private final String argumentName;
        private final String input;

        private InvalidInputException(String argumentName, String input) {
            this(argumentName, input, null);
        }

        private InvalidInputException(String argumentName, String input, Throwable cause) {
            super(cause);
            this.argumentName = argumentName;
            this.input = input;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class PlayerOnlySignal extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
