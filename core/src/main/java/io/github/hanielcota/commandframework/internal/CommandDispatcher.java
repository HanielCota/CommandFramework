package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolveException;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.AsyncExecutor;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandContext;
import io.github.hanielcota.commandframework.CommandMiddleware;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.PlatformBridge;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pipeline orchestrator that dispatches a parsed command invocation through its stages:
 * permission check, player-only check, cooldown, argument parsing, confirmation and execution.
 *
 * <p>A single instance is shared by all commands registered in a framework. It holds no per-call
 * state - all request-scoped state lives in {@link CommandContext} arguments passed through the
 * pipeline.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use. Mutable collaborator state lives inside the
 * dispatcher's thread-safe managers (cooldown, rate limit, confirmation) and the platform-provided
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
    private final FrameworkLogger logger;
    private final AsyncExecutor asyncExecutor;
    private final boolean debug;
    private final CommandSuggestionEngine suggestionEngine;
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
            FrameworkLogger logger,
            AsyncExecutor asyncExecutor,
            boolean debug
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
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.debug = debug;
        this.suggestionEngine = new CommandSuggestionEngine(
                this.commandsByLabel,
                this.tokenizer,
                this.messages,
                this.logger,
                this::allowed,
                this::resolver
        );
    }

    public CommandResult dispatch(CommandActor actor, String label, String rawArguments) {
        long startedNanos = this.debug ? System.nanoTime() : 0L;
        try {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            if (this.confirmationCommands.contains(normalizedLabel)) {
                CommandResult result = this.dispatchConfirmation(actor, normalizedLabel);
                if (this.debug) {
                    this.traceDispatch(actor, label, "confirmation", result, startedNanos);
                }
                return result;
            }

            CommandDefinition command = this.commandsByLabel.get(normalizedLabel);
            if (command == null) {
                if (this.debug) {
                    this.traceDispatch(actor, label, "unknown", CommandResult.handled(), startedNanos);
                }
                return CommandResult.handled();
            }

            TokenizedInput tokenizedInput = this.tokenizer.tokenize(rawArguments);
            Selection selection = this.select(command, tokenizedInput);
            if (selection == null) {
                this.suggestionEngine.emitDidYouMean(actor, command, tokenizedInput);
                this.sendHelp(actor, command, label);
                return new CommandResult.HelpShown();
            }

            CommandContext context = new CommandContext(actor, label, rawArguments, selection.argumentTokens(), selection.commandPath());
            CommandResult result = this.invokeMiddleware(0, context, ctx -> this.executeSelection(ctx.actor(), command, ctx.label(), selection));
            CommandResult emitted = this.emit(actor, Objects.requireNonNull(result, "Middleware chain must not return null"));
            if (this.debug) {
                this.traceDispatch(actor, label, selection.commandPath(), emitted, startedNanos);
            }
            return emitted;
        } catch (MissingArgumentException exception) {
            this.messages.send(actor, MessageKey.MISSING_ARGUMENT, Map.of("name", exception.argumentName));
            return CommandResult.handled();
        } catch (TooManyArgumentsException exception) {
            this.messages.send(actor, MessageKey.TOO_MANY_ARGUMENTS, Map.of("input", exception.input));
            return CommandResult.handled();
        } catch (InvalidInputException exception) {
            return this.emit(actor, new CommandResult.InvalidArgs(exception.argumentName, exception.input));
        } catch (RuntimeException exception) {
            this.logger.error("Unhandled command exception while dispatching " + LogSafety.sanitize(label), exception);
            return this.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
        }
    }

    public CommandResult dispatchConfirmation(CommandActor actor, String label) {
        PreparedInvocation invocation = this.confirmationManager.consume(actor, label);
        if (invocation == null) {
            return this.emit(actor, CommandResult.failure(MessageKey.CONFIRM_NOTHING_PENDING));
        }
        try {
            CommandResult blocked = this.preflight(actor, invocation.executor());
            if (blocked != null) {
                return this.emit(actor, blocked);
            }
            CommandResult cooldownBlocked = this.consumeCooldown(actor, invocation.executor(), invocation.commandPath());
            if (cooldownBlocked != null) {
                return this.emit(actor, cooldownBlocked);
            }
            CommandResult result = this.executePrepared(actor, invocation);
            return this.emit(actor, result);
        } catch (RuntimeException exception) {
            this.logger.error("Unhandled command exception while confirming " + LogSafety.sanitize(label), exception);
            return this.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
        }
    }

    public List<String> suggest(CommandActor actor, String label, String rawArguments) {
        return this.suggestionEngine.suggest(actor, label, rawArguments);
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
        CommandResult blocked = this.preflight(actor, executor);
        if (blocked != null) {
            return blocked;
        }

        // Cooldown is evaluated AFTER argument parsing so that a syntax error (MissingArgumentException /
        // ArgumentResolveException raised from prepareInvocation) does not consume the sender's cooldown
        // window. Otherwise a user who typo'd arguments would be locked out on their legitimate retry.
        PreparedInvocation invocation = this.prepareInvocation(actor, command, label, selection);

        if (executor.confirm() != null) {
            // Peek cooldown without consuming - if the sender is already on cooldown from a prior
            // successful confirmation, reject now instead of queueing another prompt that would
            // only hit the same cooldown at consume time. Cooldown is actually consumed in
            // dispatchConfirmation, so abandoned or expired prompts never eat the window.
            if (executor.cooldown() != null) {
                CooldownManager.CooldownResult status =
                        this.cooldownManager.status(selection.commandPath(), actor, executor.cooldown());
                if (!status.allowed()) {
                    return new CommandResult.CooldownActive(status.remaining());
                }
            }
            this.confirmationManager.put(actor, invocation, executor.confirm());
            return new CommandResult.PendingConfirmation(executor.confirm().commandName(), executor.confirm().expiresIn());
        }

        CommandResult cooldownBlocked = this.consumeCooldown(actor, executor, selection.commandPath());
        if (cooldownBlocked != null) {
            return cooldownBlocked;
        }
        return this.executePrepared(actor, invocation);
    }

    private CommandResult consumeCooldown(CommandActor actor, ExecutorDefinition executor, String commandPath) {
        if (executor.cooldown() == null) {
            return null;
        }
        CooldownManager.CooldownResult cooldownResult =
                this.cooldownManager.checkAndConsume(commandPath, actor, executor.cooldown());
        if (!cooldownResult.allowed()) {
            return new CommandResult.CooldownActive(cooldownResult.remaining());
        }
        return null;
    }

    private PreparedInvocation prepareInvocation(CommandActor actor, CommandDefinition command, String label, Selection selection) {
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
     * Executes a prepared invocation either synchronously or on the async executor.
     *
     * <p><b>Async caveat:</b> for {@code @Async} executors this method returns
     * {@link CommandResult#success()} immediately and dispatches emission from the async callback.
     * The middleware chain therefore sees {@code Success}, not the executor's real result - any
     * post-processing middleware (metrics, audit, cooldown rollback) cannot observe failures
     * produced inside an async body. Middlewares that need the real outcome must run on the
     * async thread themselves or the executor must be synchronous.
     */
    private CommandResult executePrepared(CommandActor actor, PreparedInvocation invocation) {
        if (invocation.executor().async()) {
            this.asyncExecutor.execute(invocation.commandPath(), () -> {
                CommandResult result;
                try {
                    result = this.invokeMethod(actor, invocation);
                } catch (Exception exception) {
                    // Narrowed from Throwable - let Error (OOM, StackOverflow) reach the
                    // virtual-thread uncaught handler so the operator sees a real signal.
                    this.logger.error("Unhandled exception in async command " + invocation.commandPath(), exception);
                    return;
                }
                try {
                    this.emit(actor, result);
                } catch (Exception exception) {
                    // Command body completed; only result emission failed (e.g. plugin disabled
                    // mid-flight, message renderer threw). Log as warn to avoid flagging a
                    // successful command as a runtime failure in operator dashboards.
                    this.logger.warn("Failed to emit result for async command " + invocation.commandPath(), exception);
                }
            });
            return CommandResult.success();
        }
        return this.invokeMethod(actor, invocation);
    }

    private CommandResult invokeMethod(CommandActor actor, PreparedInvocation invocation) {
        try {
            Object[] arguments = this.buildArguments(actor, invocation.executor(), invocation.preparedArguments());
            Object result = invocation.executor().invoker().invoke(invocation.command().instance(), arguments);
            if (result == null) {
                return CommandResult.success();
            }
            return (CommandResult) result;
        } catch (PlayerOnlySignal ignored) {
            return new CommandResult.PlayerOnly();
        } catch (Error error) {
            // Let Error (OOM, StackOverflow, LinkageError) propagate to the JVM - swallowing
            // them hides irrecoverable JVM state from the operator and keeps a degraded process
            // running with inconsistent guarantees.
            throw error;
        } catch (Throwable exception) {
            // Broad Throwable catch is required because the invoker signature declares
            // `throws Throwable`; Errors have already been re-thrown above.
            this.logger.error("Unhandled command exception in " + invocation.commandPath(), exception);
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

        ExecutorDefinition rootExecutor = command.rootExecutor();
        if (rootExecutor == null) {
            return null;
        }
        if (!command.executorsBySubcommand().isEmpty() && !this.hasNonSenderParameters(rootExecutor)) {
            return null;
        }
        return new Selection(rootExecutor, tokenizedInput.tokens(), command.name());
    }

    private boolean hasNonSenderParameters(ExecutorDefinition executor) {
        for (ParameterDefinition parameter : executor.parameters()) {
            if (!parameter.sender()) {
                return true;
            }
        }
        return false;
    }

    // Derived from preflight() so both paths cannot drift - any new gate added to preflight
    // (world restriction, feature flag, etc.) is automatically reflected in tab completion,
    // help rendering, and did-you-mean filtering without a second edit.
    private boolean allowed(CommandActor actor, ExecutorDefinition executor) {
        return this.preflight(actor, executor) == null;
    }

    private CommandResult preflight(CommandActor actor, ExecutorDefinition executor) {
        Objects.requireNonNull(actor, "actor");
        if (executor.requirePlayer() && !actor.isPlayer()) {
            return new CommandResult.PlayerOnly();
        }
        if (!executor.permission().isBlank() && !actor.hasPermission(executor.permission())) {
            return new CommandResult.NoPermission(executor.permission());
        }
        return null;
    }

    private void traceDispatch(CommandActor actor, String label, String path, CommandResult result, long startedNanos) {
        long tookMicros = (System.nanoTime() - startedNanos) / 1_000L;
        String resultName = result.getClass().getSimpleName();
        this.logger.info("[cf] dispatch actor=" + LogSafety.sanitize(actor.name())
                + " label=" + LogSafety.sanitize(label) + " path=" + path
                + " result=" + resultName + " tookUs=" + tookMicros);
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
            default -> { /* Success / Handled / HelpShown / RateLimited - nothing to emit */ }
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
            return this.enumResolverCache.get(type);
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

    private static final class TooManyArgumentsException extends RuntimeException {
        private final String input;

        private TooManyArgumentsException(String input) {
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
