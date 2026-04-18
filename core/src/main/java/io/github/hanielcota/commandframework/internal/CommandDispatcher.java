package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.AsyncExecutor;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandContext;
import io.github.hanielcota.commandframework.CommandMiddleware;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.PlatformBridge;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

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
    private final ArgumentPreparer argumentPreparer;
    private final CommandSuggestionEngine suggestionEngine;
    private final CommandResultEmitter resultEmitter;

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
        this.argumentPreparer = new ArgumentPreparer(this.resolvers, this.bridge);
        BiPredicate<CommandActor, ExecutorDefinition> permissionGate = this::allowed;
        this.resultEmitter = new CommandResultEmitter(this.messages, permissionGate);
        this.suggestionEngine = new CommandSuggestionEngine(
                this.commandsByLabel,
                this.tokenizer,
                this.messages,
                this.logger,
                permissionGate,
                this.argumentPreparer::resolver
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
                this.resultEmitter.sendHelp(actor, command, label);
                return new CommandResult.HelpShown();
            }

            CommandContext context = new CommandContext(actor, label, rawArguments, selection.argumentTokens(), selection.commandPath());
            CommandResult result = this.invokeMiddleware(0, context, ctx -> this.executeSelection(ctx.actor(), command, ctx.label(), selection));
            CommandResult emitted = this.resultEmitter.emit(actor, Objects.requireNonNull(result, "Middleware chain must not return null"));
            if (this.debug) {
                this.traceDispatch(actor, label, selection.commandPath(), emitted, startedNanos);
            }
            return emitted;
        } catch (ArgumentPreparer.MissingArgumentException exception) {
            this.messages.send(actor, MessageKey.MISSING_ARGUMENT, Map.of("name", exception.argumentName));
            return CommandResult.handled();
        } catch (ArgumentPreparer.TooManyArgumentsException exception) {
            this.messages.send(actor, MessageKey.TOO_MANY_ARGUMENTS, Map.of("input", exception.input));
            return CommandResult.handled();
        } catch (ArgumentPreparer.InvalidInputException exception) {
            return this.resultEmitter.emit(actor, new CommandResult.InvalidArgs(exception.argumentName, exception.input));
        } catch (RuntimeException exception) {
            this.logger.error("Unhandled command exception while dispatching " + LogSafety.sanitize(label), exception);
            return this.resultEmitter.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
        }
    }

    public CommandResult dispatchConfirmation(CommandActor actor, String label) {
        PreparedInvocation invocation = this.confirmationManager.consume(actor, label);
        if (invocation == null) {
            return this.resultEmitter.emit(actor, CommandResult.failure(MessageKey.CONFIRM_NOTHING_PENDING));
        }
        try {
            CommandResult blocked = this.preflight(actor, invocation.executor());
            if (blocked != null) {
                return this.resultEmitter.emit(actor, blocked);
            }
            CommandResult cooldownBlocked = this.consumeCooldown(actor, invocation.executor(), invocation.commandPath());
            if (cooldownBlocked != null) {
                return this.resultEmitter.emit(actor, cooldownBlocked);
            }
            CommandResult result = this.executePrepared(actor, invocation);
            return this.resultEmitter.emit(actor, result);
        } catch (RuntimeException exception) {
            this.logger.error("Unhandled command exception while confirming " + LogSafety.sanitize(label), exception);
            return this.resultEmitter.emit(actor, CommandResult.failure(MessageKey.COMMAND_ERROR));
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
        // InvalidInputException raised from ArgumentPreparer.prepare) does not consume the sender's
        // cooldown window. Otherwise a user who typo'd arguments would be locked out on their
        // legitimate retry.
        PreparedInvocation invocation = this.argumentPreparer.prepare(actor, command, label, selection);

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
                    this.resultEmitter.emit(actor, result);
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
            Object[] arguments = this.argumentPreparer.bindArguments(actor, invocation);
            Object result = invocation.executor().invoker().invoke(invocation.command().instance(), arguments);
            if (result == null) {
                return CommandResult.success();
            }
            return (CommandResult) result;
        } catch (ArgumentPreparer.PlayerOnlySignal ignored) {
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
}
