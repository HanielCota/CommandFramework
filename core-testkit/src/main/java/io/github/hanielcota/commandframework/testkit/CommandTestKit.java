package io.github.hanielcota.commandframework.testkit;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandFramework;
import io.github.hanielcota.commandframework.CommandFrameworkBuilder;
import io.github.hanielcota.commandframework.CommandMiddleware;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.PlatformBridge;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * High-level harness for testing annotation-based commands without standing up a real
 * Paper or Velocity runtime.
 *
 * <p>Typical usage:
 * <pre>{@code
 * var kit = CommandTestKit.builder()
 *         .bind(EconomyService.class, economyMock)
 *         .build(new EconomyCommand());
 *
 * kit.dispatch("eco", "pay alice 10").assertSuccess();
 * kit.dispatch("eco", "pay alice 10").assertCooldownActive();
 * kit.dispatch("eco", "reset alice").assertPendingConfirmation("eco-confirm");
 * }</pre>
 *
 * <p>The harness wires a stub {@link PlatformBridge} whose sender type is
 * {@link TestSender}; use {@link #sender(String)} / {@link #player(String)} to create
 * callers with specific permissions.
 */
public final class CommandTestKit {

    private final CommandFramework<TestSender> framework;

    private CommandTestKit(CommandFramework<TestSender> framework) {
        this.framework = framework;
    }

    /**
     * Creates a kit with no custom bindings. Equivalent to {@code builder().build(commands)}.
     *
     * @param commands command instances to register
     * @return a ready-to-use test kit
     */
    public static CommandTestKit create(Object... commands) {
        return builder().build(commands);
    }

    /**
     * Starts a new fluent builder for advanced configuration.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the underlying framework for lower-level access (suggest, registered labels, etc.).
     *
     * @return the underlying framework
     */
    public CommandFramework<TestSender> framework() {
        return this.framework;
    }

    /**
     * Creates a test sender (console-like) with the given name. Grant permissions via
     * {@link TestSender#grant(String)}.
     *
     * @param name display name
     * @return a non-player sender
     */
    public TestSender sender(String name) {
        return new TestSender(Objects.requireNonNull(name, "name"), false);
    }

    /**
     * Creates a test player with the given name. Grant permissions via
     * {@link TestSender#grant(String)}.
     *
     * @param name player name
     * @return a player sender
     */
    public TestSender player(String name) {
        return new TestSender(Objects.requireNonNull(name, "name"), true);
    }

    /**
     * Dispatches a command from an anonymous console sender.
     *
     * @param label       command label
     * @param rawArguments raw argument string
     * @return a dispatch assertion handle
     */
    public DispatchAssert dispatch(String label, String rawArguments) {
        return this.dispatch(this.sender("console"), label, rawArguments);
    }

    /**
     * Dispatches a command from the given sender.
     *
     * @param sender       caller
     * @param label        command label
     * @param rawArguments raw argument string
     * @return a dispatch assertion handle
     */
    public DispatchAssert dispatch(TestSender sender, String label, String rawArguments) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rawArguments, "rawArguments");
        CommandResult result = this.framework.dispatch(sender, label, rawArguments);
        return new DispatchAssert(sender, label, result);
    }

    /**
     * Returns suggestions for the given partial input.
     *
     * @param label        command label
     * @param rawArguments raw argument string with optional trailing space
     * @return suggestions from the dispatcher
     */
    public List<String> suggest(String label, String rawArguments) {
        return this.suggest(this.sender("console"), label, rawArguments);
    }

    /**
     * Returns suggestions for a specific sender.
     *
     * @param sender       caller
     * @param label        command label
     * @param rawArguments raw argument string
     * @return suggestions from the dispatcher
     */
    public List<String> suggest(TestSender sender, String label, String rawArguments) {
        return this.framework.suggest(sender, label, rawArguments);
    }

    /**
     * Fluent builder for configuring a kit before construction.
     */
    public static final class Builder {
        private final TestBridge bridge = new TestBridge();
        private final TestFrameworkBuilder inner = new TestFrameworkBuilder(this.bridge);

        private Builder() {
        }

        /**
         * Binds a dependency for {@code @Inject} resolution.
         *
         * @param type     bound type
         * @param instance instance to return
         * @param <T>      dependency type
         * @return this builder
         */
        public <T> Builder bind(Class<T> type, T instance) {
            this.inner.bind(type, instance);
            return this;
        }

        /**
         * Registers a custom argument resolver.
         *
         * @param resolver the resolver
         * @return this builder
         */
        public Builder resolver(ArgumentResolver<?> resolver) {
            this.inner.resolver(resolver);
            return this;
        }

        /**
         * Registers a middleware.
         *
         * @param middleware the middleware
         * @return this builder
         */
        public Builder middleware(CommandMiddleware middleware) {
            this.inner.middleware(middleware);
            return this;
        }

        /**
         * Overrides the default rate-limit window.
         *
         * @param commands allowed commands per window
         * @param window   window length
         * @return this builder
         */
        public Builder rateLimit(int commands, Duration window) {
            this.inner.rateLimit(commands, window);
            return this;
        }

        /**
         * Overrides a message template.
         *
         * @param key      message key
         * @param template MiniMessage template
         * @return this builder
         */
        public Builder message(MessageKey key, String template) {
            this.inner.message(key, template);
            return this;
        }

        /**
         * Enables verbose dispatch logging during tests.
         *
         * @param enabled whether to log
         * @return this builder
         */
        public Builder debug(boolean enabled) {
            this.inner.debug(enabled);
            return this;
        }

        /**
         * Escape hatch for configuration not covered by the builder shortcuts.
         *
         * @param configurator consumer that receives the underlying framework builder
         * @return this builder
         */
        public Builder configure(Consumer<CommandFrameworkBuilder<TestSender, TestFrameworkBuilder>> configurator) {
            configurator.accept(this.inner);
            return this;
        }

        /**
         * Finishes the kit with the provided command instances.
         *
         * @param commands command instances
         * @return a ready kit
         */
        public CommandTestKit build(Object... commands) {
            if (commands != null && commands.length > 0) {
                this.inner.commands(commands);
            }
            return new CommandTestKit(this.inner.build());
        }
    }

    /**
     * Minimal {@link PlatformBridge} used by the kit. Registration is a no-op — callers
     * invoke {@link CommandFramework#dispatch} directly.
     */
    static final class TestBridge implements PlatformBridge<TestSender> {
        private final FrameworkLogger logger = FrameworkLogger.jul(Logger.getLogger("CommandTestKit"));

        @Override public ClassLoader classLoader() { return this.getClass().getClassLoader(); }
        @Override public String defaultScanPackage() { return ""; }
        @Override public FrameworkLogger logger() { return this.logger; }
        @Override public CommandActor createActor(TestSender sender) { return sender; }

        @Override
        public boolean supportsSenderType(Class<?> type) {
            return type == CommandActor.class || type == TestSender.class;
        }

        @Override
        public boolean isPlayerSenderType(Class<?> type) {
            return false;
        }

        @Override
        public void register(CommandFramework<TestSender> framework) {
            // intentionally empty — tests dispatch directly
        }
    }

    /**
     * Concrete {@link CommandFrameworkBuilder} used internally. Public so that
     * {@link Builder#configure(Consumer)} can expose it as an escape hatch.
     */
    public static final class TestFrameworkBuilder
            extends CommandFrameworkBuilder<TestSender, TestFrameworkBuilder> {
        TestFrameworkBuilder(TestBridge bridge) { super(bridge); }
        @Override protected TestFrameworkBuilder self() { return this; }
    }
}
