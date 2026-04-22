package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.annotation.platform.PlatformCommandAdapter;
import io.github.hanielcota.commandframework.annotation.scan.AnnotatedCommandScanner;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandInterceptor;
import io.github.hanielcota.commandframework.core.CommandLogger;
import io.github.hanielcota.commandframework.core.CommandMessageProvider;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import io.github.hanielcota.commandframework.core.cooldown.RouteCooldownStore;
import io.github.hanielcota.commandframework.core.rate.DispatchThrottle;
import io.github.hanielcota.commandframework.core.safety.InputSanitizer;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PaperCommandFramework extends PlatformCommandAdapter {

    private final JavaPlugin plugin;
    private final PaperCommandRegistrar registrar;

    public PaperCommandFramework(
            JavaPlugin plugin,
            CommandDispatcher dispatcher,
            AnnotatedCommandScanner scanner) {
        this(plugin, dispatcher, scanner, new BukkitPaperCommandRegistrar());
    }

    public PaperCommandFramework(
            JavaPlugin plugin,
            CommandDispatcher dispatcher,
            AnnotatedCommandScanner scanner,
            PaperCommandRegistrar registrar) {
        super(dispatcher, scanner);
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public static PaperCommandFramework create(JavaPlugin plugin) {
        return builder(plugin).build();
    }

    public static Builder builder(JavaPlugin plugin) {
        return new Builder(plugin);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    @Override
    protected void registerRoot(CommandRoot root) {
        registrar.register(plugin, root, dispatcher());
    }

    @Override
    protected void unregisterRoot(CommandRoot root) {
        registrar.unregister(plugin, root, dispatcher());
    }

    public static final class Builder {

        private final JavaPlugin plugin;
        private final CommandDispatcher.Builder dispatcherBuilder = CommandDispatcher.builder();
        private ParameterResolverRegistry resolvers = ParameterResolverRegistry.withDefaults();
        private @Nullable CommandDispatcher dispatcher;
        private @Nullable AnnotatedCommandScanner scanner;
        private PaperCommandRegistrar registrar = new BukkitPaperCommandRegistrar();

        private Builder(JavaPlugin plugin) {
            this.plugin = Objects.requireNonNull(plugin, "plugin");
        }

        public Builder dispatcher(CommandDispatcher dispatcher) {
            this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
            return this;
        }

        public Builder scanner(AnnotatedCommandScanner scanner) {
            this.scanner = Objects.requireNonNull(scanner, "scanner");
            return this;
        }

        public Builder resolvers(ParameterResolverRegistry resolvers) {
            this.resolvers = Objects.requireNonNull(resolvers, "resolvers");
            return this;
        }

        public <T> Builder resolver(ParameterResolver<T> resolver) {
            resolvers.register(resolver);
            return this;
        }

        public <T> Builder argumentResolver(ArgumentResolver<T> resolver) {
            resolvers.registerArgument(resolver);
            return this;
        }

        public Builder cooldownStore(RouteCooldownStore cooldownStore) {
            dispatcherBuilder.cooldownStore(cooldownStore);
            return this;
        }

        public Builder throttle(DispatchThrottle throttle) {
            dispatcherBuilder.throttle(throttle);
            return this;
        }

        public Builder sanitizer(InputSanitizer sanitizer) {
            dispatcherBuilder.sanitizer(sanitizer);
            return this;
        }

        public Builder messageProvider(CommandMessageProvider messages) {
            dispatcherBuilder.messageProvider(messages);
            return this;
        }

        public Builder logger(CommandLogger logger) {
            dispatcherBuilder.logger(logger);
            return this;
        }

        public Builder interceptor(CommandInterceptor interceptor) {
            dispatcherBuilder.interceptor(interceptor);
            return this;
        }

        public Builder registrar(PaperCommandRegistrar registrar) {
            this.registrar = Objects.requireNonNull(registrar, "registrar");
            return this;
        }

        public PaperCommandFramework build() {
            CommandDispatcher resolvedDispatcher = dispatcher == null ? dispatcherBuilder.build() : dispatcher;
            AnnotatedCommandScanner resolvedScanner = scanner == null
                    ? new AnnotatedCommandScanner(resolvers)
                    : scanner;
            return new PaperCommandFramework(plugin, resolvedDispatcher, resolvedScanner, registrar);
        }
    }
}
