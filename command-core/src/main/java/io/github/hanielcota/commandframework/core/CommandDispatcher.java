package io.github.hanielcota.commandframework.core;

import io.github.hanielcota.commandframework.core.config.CommandConfiguration;
import io.github.hanielcota.commandframework.core.config.ConfigurationOverlay;
import io.github.hanielcota.commandframework.core.cooldown.RouteCooldownStore;
import io.github.hanielcota.commandframework.core.dispatch.CommandParameterParser;
import io.github.hanielcota.commandframework.core.message.DefaultCommandMessageProvider;
import io.github.hanielcota.commandframework.core.metrics.CommandMetrics;
import io.github.hanielcota.commandframework.core.pipeline.CommandDispatchStage;
import io.github.hanielcota.commandframework.core.pipeline.DispatchContinuation;
import io.github.hanielcota.commandframework.core.pipeline.ExecutionStage;
import io.github.hanielcota.commandframework.core.pipeline.GuardStage;
import io.github.hanielcota.commandframework.core.rate.DispatchThrottle;
import io.github.hanielcota.commandframework.core.safety.ActorMessageDebouncer;
import io.github.hanielcota.commandframework.core.safety.InputSanitizer;
import io.github.hanielcota.commandframework.core.safety.SafeLogText;
import io.github.hanielcota.commandframework.core.safety.SanitizedInput;
import io.github.hanielcota.commandframework.core.suggestion.CommandSuggestionEngine;
import io.github.hanielcota.commandframework.core.usage.UsageFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Central entry-point for command registration, dispatch and tab-completion.
 *
 * <p>The dispatcher is thread-safe for reads and is designed to be called
 * directly from platform command threads. Route registration, however, should
 * happen during plugin startup and is synchronized.</p>
 *
 * <p>Dispatch follows a pipeline: pre-validation (throttle + sanitizer) →
 * route resolution → guard stage (permission, sender, cooldown) →
 * execution stage (parse + invoke + interceptors).</p>
 *
 * <p><strong>Thread-safety warning for Paper/Bukkit:</strong> When a route is
 * marked as async ({@link CommandRoute#async()}), the entire pipeline runs on
 * the provided {@code asyncExecutor}. On Paper/Bukkit, most API calls (Player,
 * World, Inventory, etc.) must run on the main thread. Ensure that async
 * command executors either avoid Bukkit API calls or schedule work back to the
 * main thread. The Paper adapter's {@code sendMessage} handles thread-safety
 * automatically when a Plugin reference is provided.</p>
 */
public final class CommandDispatcher {

    private static final String ACTOR_PARAMETER = "actor";
    private static final String LABEL_PARAMETER = "label";

    private final CommandRouteRegistry registry;
    private final PreDispatchValidator validator;
    private final CommandMessenger messenger;
    private final CommandSuggestionEngine suggestions;
    private final CommandLogger logger;
    private final SafeLogText safeLogText;
    private final CommandDispatchStage pipeline;
    private final Executor asyncExecutor;
    private final CommandMetrics metrics;
    private final ConfigurationOverlay overlay;
    private final java.util.Map<CommandRoute, CommandRoute> originalToOverlay = new java.util.concurrent.ConcurrentHashMap<>();

    private CommandDispatcher(Builder builder) {
        this.registry = builder.registry;
        this.validator = new PreDispatchValidator(builder.throttle, builder.sanitizer);
        this.messenger = new CommandMessenger(builder.messages, builder.debouncer);
        this.suggestions = new CommandSuggestionEngine(registry);
        this.logger = builder.logger;
        this.safeLogText = new SafeLogText();
        this.pipeline = buildPipeline(
                builder.cooldownStore,
                messenger,
                List.copyOf(builder.interceptors),
                logger,
                safeLogText
        );
        this.asyncExecutor = builder.asyncExecutor;
        this.metrics = builder.metrics;
        this.overlay = new ConfigurationOverlay(builder.configuration);
    }

    private static CommandDispatchStage buildPipeline(
            RouteCooldownStore cooldownStore,
            CommandMessenger messenger,
            List<CommandInterceptor> interceptors,
            CommandLogger logger,
            SafeLogText safeLogText) {
        CommandDispatchStage guard = new GuardStage(cooldownStore, messenger, logger);
        CommandDispatchStage execution = new ExecutionStage(
                new CommandParameterParser(),
                new UsageFormatter(),
                messenger,
                interceptors,
                logger,
                safeLogText
        );
        return (context, continuation) -> guard.process(context, ctx -> execution.process(ctx, DispatchContinuation.terminal()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public void register(CommandRoute route) {
        CommandRoute applied = overlay.apply(route);
        originalToOverlay.put(route, applied);
        registry.register(applied);
    }

    public void unregister(CommandRoute route) {
        CommandRoute applied = originalToOverlay.remove(route);
        registry.unregister(applied != null ? applied : route);
    }

    public List<CommandRoot> roots() {
        return registry.roots();
    }

    public CommandLogger logger() {
        return logger;
    }

    public CommandResult dispatch(CommandActor actor, String label, String[] arguments) {
        return dispatch(actor, label, arguments == null ? List.of() : java.util.Arrays.asList(arguments));
    }

    public CommandResult dispatch(CommandActor actor, String label, List<String> arguments) {
        Objects.requireNonNull(actor, ACTOR_PARAMETER);
        Objects.requireNonNull(label, LABEL_PARAMETER);
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(actor.uniqueId(), "actor.uniqueId");
        logger.trace("Dispatch start: actor=%s, label=%s, args=%s".formatted(actor.uniqueId(), label, arguments));
        long startNanos = System.nanoTime();
        var validation = validator.validate(actor, arguments);
        if (!validation.isValid()) {
            logger.debug("Dispatch rejected: actor=%s, reason=%s".formatted(actor.uniqueId(), validation.rateLimited() ? "rate-limited" : "invalid-input"));
            metrics.increment("command.dispatch", CommandMetrics.tags("status", validation.rateLimited() ? "throttled" : "invalid-input", "route", label));
            if (validation.rateLimited()) {
                return messenger.rateLimited(actor);
            }
            return messenger.invalidInput(actor, validation.invalidValue(), validation.expectedValue());
        }
        var result = resolveAndDispatch(actor, label, validation.arguments());
        metrics.record("command.dispatch.duration", CommandMetrics.tags("route", label), Duration.ofNanos(System.nanoTime() - startNanos));
        metrics.increment("command.dispatch", CommandMetrics.tags("status", result.status().name().toLowerCase(), "route", label));
        return result;
    }

    public List<String> suggest(CommandActor actor, String label, String[] arguments) {
        return suggest(actor, label, arguments == null ? List.of() : java.util.Arrays.asList(arguments));
    }

    public List<String> suggest(CommandActor actor, String label, List<String> arguments) {
        Objects.requireNonNull(actor, ACTOR_PARAMETER);
        Objects.requireNonNull(label, LABEL_PARAMETER);
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(actor.uniqueId(), "actor.uniqueId");
        SanitizedInput input = validator.sanitize(arguments);
        if (!input.isValid()) {
            return List.of();
        }
        return suggestions.suggest(actor, label, input.arguments());
    }

    private CommandResult resolveAndDispatch(CommandActor actor, String label, List<String> arguments) {
        var resolution = registry.resolve(label, arguments);
        return switch (resolution) {
            case RouteResolution.Found found -> {
                var match = found.matchValue();
                logger.trace("Route resolved: label=%s -> %s".formatted(label, match.route().canonicalPath()));
                var context = new CommandContext(actor, match.route(), label, match.arguments());
                yield dispatchResolved(context);
            }
            case RouteResolution.NotFound notFound -> {
                logger.debug("Route not found: label=%s, args=%s".formatted(label, arguments));
                yield messenger.unknownCommand(actor, label);
            }
        };
    }

    private CommandResult dispatchResolved(CommandContext context) {
        if (context.route().async() && asyncExecutor != null) {
            try {
                asyncExecutor.execute(() -> runPipeline(context));
            } catch (RuntimeException exception) {
                logger.warn("Async dispatch rejected for route: " + context.route().canonicalPath(), exception);
                return runtimeErrorResult(context, exception);
            }
            return CommandResult.accepted();
        }
        return runPipeline(context);
    }

    private CommandResult runPipeline(CommandContext context) {
        try {
            var result = pipeline.process(context, DispatchContinuation.terminal());
            logger.trace("Pipeline finished: route=%s, status=%s".formatted(context.route().canonicalPath(), result.status()));
            return result;
        } catch (RuntimeException exception) {
            logger.warn("Pipeline runtime error for route: " + context.route().canonicalPath(), exception);
            return runtimeErrorResult(context, exception);
        }
    }

    private CommandResult runtimeErrorResult(CommandContext context, RuntimeException exception) {
        String route = safeLogText.clean(context.route().canonicalPath());
        logger.warn("Command route failed: " + route, exception);
        return messenger.internalError(context);
    }

    public static final class Builder {

        private final CommandRouteRegistry registry = new CommandRouteRegistry();
        private final List<CommandInterceptor> interceptors = new ArrayList<>();
        private RouteCooldownStore cooldownStore = new RouteCooldownStore();
        private DispatchThrottle throttle = new DispatchThrottle(30, Duration.ofSeconds(2));
        private InputSanitizer sanitizer = new InputSanitizer(32, 128);
        private final ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(750));
        private CommandMessageProvider messages = new DefaultCommandMessageProvider();
        private CommandLogger logger = CommandLogger.noop();
        private CommandMetrics metrics = CommandMetrics.noop();
        private CommandConfiguration configuration = CommandConfiguration.empty();
        private Executor asyncExecutor;

        public Builder cooldownStore(RouteCooldownStore cooldownStore) {
            this.cooldownStore = Objects.requireNonNull(cooldownStore, "cooldownStore");
            return this;
        }

        public Builder throttle(DispatchThrottle throttle) {
            this.throttle = Objects.requireNonNull(throttle, "throttle");
            return this;
        }

        public Builder sanitizer(InputSanitizer sanitizer) {
            this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
            return this;
        }

        public Builder messageProvider(CommandMessageProvider messages) {
            this.messages = Objects.requireNonNull(messages, "messages");
            return this;
        }

        public Builder logger(CommandLogger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        public Builder interceptor(CommandInterceptor interceptor) {
            this.interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        public Builder asyncExecutor(Executor executor) {
            this.asyncExecutor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder metrics(CommandMetrics metrics) {
            this.metrics = Objects.requireNonNull(metrics, "metrics");
            return this;
        }

        public Builder configuration(CommandConfiguration configuration) {
            this.configuration = Objects.requireNonNull(configuration, "configuration");
            return this;
        }

        public CommandDispatcher build() {
            return new CommandDispatcher(this);
        }
    }
}
