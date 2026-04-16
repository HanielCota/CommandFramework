package io.github.hanielcota.commandframework;

import io.github.hanielcota.commandframework.internal.DefaultMessageProvider;
import io.github.hanielcota.commandframework.internal.DependencyContainer;
import io.github.hanielcota.commandframework.internal.InternalCommandBuilder;

import java.time.Duration;
import java.util.*;

/**
 * Base builder used by platform integrations.
 *
 * @param <S> the native sender type
 * @param <B> the concrete builder type
 */
public abstract class CommandFrameworkBuilder<S, B extends CommandFrameworkBuilder<S, B>> {

    private final PlatformBridge<S> bridge;
    private final DependencyContainer dependencies = new DependencyContainer();
    private final Map<MessageKey, String> messageOverrides = new EnumMap<>(MessageKey.class);
    private final List<ArgumentResolver<?>> resolvers = new ArrayList<>();
    private final List<CommandMiddleware> middlewares = new ArrayList<>();
    private final List<String> scanPackages = new ArrayList<>();
    private final List<Object> commandInstances = new ArrayList<>();
    private MessageProvider messageProvider = new DefaultMessageProvider();
    private int rateLimitCommands = 30;
    private Duration rateLimitWindow = Duration.ofSeconds(10);
    private boolean built;

    /**
     * Creates a new builder.
     *
     * @param bridge the platform bridge
     */
    protected CommandFrameworkBuilder(PlatformBridge<S> bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.dependencies.bind(PlatformBridge.class, bridge);
    }

    /**
     * Registers a package for auto-scanning.
     *
     * @param packageName the package name
     * @return this builder
     */
    public B scanPackage(String packageName) {
        this.scanPackages.add(Objects.requireNonNull(packageName, "packageName"));
        return this.self();
    }

    /**
     * Registers a command instance manually.
     *
     * @param commandInstance the command instance
     * @return this builder
     */
    public B command(Object commandInstance) {
        this.commandInstances.add(Objects.requireNonNull(commandInstance, "commandInstance"));
        return this.self();
    }

    /**
     * Registers multiple command instances manually.
     *
     * @param commandInstances the command instances
     * @return this builder
     */
    public B commands(Object... commandInstances) {
        Objects.requireNonNull(commandInstances, "commandInstances");
        for (Object commandInstance : commandInstances) {
            this.command(commandInstance);
        }
        return this.self();
    }

    /**
     * Registers a singleton binding for field injection.
     *
     * @param type     the exposed type
     * @param instance the singleton instance
     * @param <T>      the binding type
     * @return this builder
     */
    public <T> B bind(Class<T> type, T instance) {
        this.dependencies.bind(type, instance);
        return this.self();
    }

    /**
     * Registers a custom argument resolver.
     *
     * @param resolver the custom argument resolver
     * @return this builder
     */
    public B resolver(ArgumentResolver<?> resolver) {
        this.resolvers.add(Objects.requireNonNull(resolver, "resolver"));
        return this.self();
    }

    /**
     * Registers a custom middleware.
     *
     * @param middleware the middleware
     * @return this builder
     */
    public B middleware(CommandMiddleware middleware) {
        this.middlewares.add(Objects.requireNonNull(middleware, "middleware"));
        return this.self();
    }

    /**
     * Overrides an individual framework message.
     *
     * @param key      the message key
     * @param template the new template
     * @return this builder
     */
    public B message(MessageKey key, String template) {
        this.messageOverrides.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(template, "template"));
        return this.self();
    }

    /**
     * Replaces the complete framework message provider.
     *
     * @param provider the replacement provider
     * @return this builder
     */
    public B messages(MessageProvider provider) {
        this.messageProvider = Objects.requireNonNull(provider, "provider");
        return this.self();
    }

    /**
     * Configures the global player command rate limit.
     *
     * @param commands the maximum commands allowed in the window
     * @param window   the time window
     * @return this builder
     */
    public B rateLimit(int commands, Duration window) {
        if (commands <= 0) {
            throw new IllegalArgumentException("commands must be > 0");
        }
        this.rateLimitCommands = commands;
        this.rateLimitWindow = Objects.requireNonNull(window, "window");
        return this.self();
    }

    /**
     * Builds and registers the framework.
     *
     * @return the built framework
     */
    public CommandFramework<S> build() {
        if (this.built) {
            throw new IllegalStateException("This builder can only be used once");
        }
        this.built = true;
        CommandFramework<S> framework = new InternalCommandBuilder<>(
                this.bridge,
                this.dependencies,
                this.messageProvider,
                this.messageOverrides,
                this.resolvers,
                this.middlewares,
                this.scanPackages,
                this.commandInstances,
                this.rateLimitCommands,
                this.rateLimitWindow
        ).build();
        this.bridge.register(framework);
        return framework;
    }

    /**
     * Returns the concrete builder instance.
     *
     * @return the concrete builder instance
     */
    protected abstract B self();
}
