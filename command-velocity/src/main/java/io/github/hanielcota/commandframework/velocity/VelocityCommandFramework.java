package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.annotation.platform.PlatformCommandAdapter;
import io.github.hanielcota.commandframework.annotation.scan.AnnotatedCommandScanner;
import io.github.hanielcota.commandframework.core.*;
import io.github.hanielcota.commandframework.core.cooldown.RouteCooldownStore;
import io.github.hanielcota.commandframework.core.rate.DispatchThrottle;
import io.github.hanielcota.commandframework.core.safety.InputSanitizer;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class VelocityCommandFramework<P> extends PlatformCommandAdapter {

    private final ProxyServer server;
    private final P plugin;
    private final VelocityCommandRegistrar registrar;

    public VelocityCommandFramework(
            ProxyServer server, P plugin, CommandDispatcher dispatcher, AnnotatedCommandScanner scanner) {
        this(server, plugin, dispatcher, scanner, new DefaultVelocityCommandRegistrar());
    }

    public VelocityCommandFramework(
            ProxyServer server,
            P plugin,
            CommandDispatcher dispatcher,
            AnnotatedCommandScanner scanner,
            VelocityCommandRegistrar registrar) {
        super(dispatcher, scanner);
        this.server = Objects.requireNonNull(server, "server");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public static <P> VelocityCommandFramework<P> create(ProxyServer server, P plugin) {
        return builder(server, plugin).build();
    }

    public static <P> Builder<P> builder(ProxyServer server, P plugin) {
        return new Builder<>(server, plugin);
    }

    public ProxyServer server() {
        return server;
    }

    public P plugin() {
        return plugin;
    }

    @Override
    protected void registerRoot(CommandRoot root) {
        registrar.register(server, plugin, root, dispatcher());
    }

    @Override
    protected void unregisterRoot(CommandRoot root) {
        registrar.unregister(server, plugin, root, dispatcher());
    }

    public static final class Builder<P> {

        private final ProxyServer server;
        private final P plugin;
        private final CommandDispatcher.Builder dispatcherBuilder = CommandDispatcher.builder();
        private ParameterResolverRegistry resolvers = ParameterResolverRegistry.withDefaults();

        private @Nullable CommandDispatcher dispatcher;

        private @Nullable AnnotatedCommandScanner scanner;

        private VelocityCommandRegistrar registrar = new DefaultVelocityCommandRegistrar();

        private Builder(ProxyServer server, P plugin) {
            this.server = Objects.requireNonNull(server, "server");
            this.plugin = Objects.requireNonNull(plugin, "plugin");
        }

        public Builder<P> dispatcher(CommandDispatcher dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
            return this;
        }

        public Builder<P> scanner(AnnotatedCommandScanner scanner) {
            this.scanner = Objects.requireNonNull(scanner, "scanner");
            return this;
        }

        public Builder<P> resolvers(ParameterResolverRegistry resolvers) {
            this.resolvers = Objects.requireNonNull(resolvers, "resolvers");
            return this;
        }

        public <T> Builder<P> resolver(ParameterResolver<T> resolver) {
            resolvers.register(resolver);
            return this;
        }

        public <T> Builder<P> argumentResolver(ArgumentResolver<T> resolver) {
            resolvers.registerArgument(resolver);
            return this;
        }

        public Builder<P> cooldownStore(RouteCooldownStore cooldownStore) {
            dispatcherBuilder.cooldownStore(cooldownStore);
            return this;
        }

        public Builder<P> throttle(DispatchThrottle throttle) {
            dispatcherBuilder.throttle(throttle);
            return this;
        }

        public Builder<P> sanitizer(InputSanitizer sanitizer) {
            dispatcherBuilder.sanitizer(sanitizer);
            return this;
        }

        public Builder<P> messageProvider(CommandMessageProvider messages) {
            dispatcherBuilder.messageProvider(messages);
            return this;
        }

        public Builder<P> logger(CommandLogger logger) {
            dispatcherBuilder.logger(logger);
            return this;
        }

        public Builder<P> interceptor(CommandInterceptor interceptor) {
            dispatcherBuilder.interceptor(interceptor);
            return this;
        }

        public Builder<P> registrar(VelocityCommandRegistrar registrar) {
            this.registrar = Objects.requireNonNull(registrar, "registrar");
            return this;
        }

        public VelocityCommandFramework<P> build() {
            CommandDispatcher resolvedDispatcher = dispatcher == null ? dispatcherBuilder.build() : dispatcher;
            AnnotatedCommandScanner resolvedScanner =
                    scanner == null ? new AnnotatedCommandScanner(resolvers) : scanner;
            return new VelocityCommandFramework<>(server, plugin, resolvedDispatcher, resolvedScanner, registrar);
        }
    }
}
