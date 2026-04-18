package io.github.hanielcota.commandframework.velocity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolveException;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandFramework;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.PlatformBridge;
import io.github.hanielcota.commandframework.RegisteredCommand;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

final class VelocityPlatformBridge implements PlatformBridge<CommandSource> {

    // Velocity proxies Bedrock clients whose names include '.' and '*', so the resolver
    // only enforces an upper bound and defers existence checks to the proxy lookup.
    private static final int MAX_PLAYER_NAME_LENGTH = 32;
    // Name of the single greedy-string Brigadier argument used to forward the raw tail of
    // every framework-registered command. Referenced both where the argument is declared and
    // where it is retrieved from the parsed context - keep both sides in sync.
    private static final String GREEDY_ARG_NAME = "args";

    private final ProxyServer server;
    private final Object plugin;
    private final FrameworkLogger logger;
    private final BiPredicate<CommandActor, Player> playerSuggestFilter;

    VelocityPlatformBridge(ProxyServer server, Object plugin) {
        this(server, plugin, (actor, target) -> true);
    }

    VelocityPlatformBridge(
            ProxyServer server,
            Object plugin,
            BiPredicate<CommandActor, Player> playerSuggestFilter
    ) {
        this.server = server;
        this.plugin = plugin;
        this.logger = FrameworkLogger.jul(Logger.getLogger(plugin.getClass().getName()));
        this.playerSuggestFilter = Objects.requireNonNull(playerSuggestFilter, "playerSuggestFilter");
    }

    @Override
    public ClassLoader classLoader() {
        return this.plugin.getClass().getClassLoader();
    }

    @Override
    public String defaultScanPackage() {
        return this.plugin.getClass().getPackageName();
    }

    @Override
    public FrameworkLogger logger() {
        return this.logger;
    }

    @Override
    public CommandActor createActor(CommandSource sender) {
        return new VelocityActor(sender);
    }

    @Override
    public boolean supportsSenderType(Class<?> type) {
        return type == CommandActor.class
                || type == CommandSource.class
                || type == Player.class;
    }

    @Override
    public boolean isPlayerSenderType(Class<?> type) {
        return type == Player.class;
    }

    @Override
    public List<ArgumentResolver<?>> platformResolvers() {
        return List.of(new VelocityPlayerResolver(this.server, this.playerSuggestFilter));
    }

    @Override
    public void register(CommandFramework<CommandSource> framework) {
        CommandManager manager = this.server.getCommandManager();
        for (RegisteredCommand command : framework.registeredCommands()) {
            this.assertNoCollision(manager, command.name());
            command.aliases().forEach(alias -> this.assertNoCollision(manager, alias));

            BrigadierCommand brigadierCommand = new BrigadierCommand(this.builder(framework, command));
            CommandMeta meta = manager.metaBuilder(command.name())
                    .plugin(this.plugin)
                    .aliases(command.aliases().toArray(String[]::new))
                    .build();
            this.registerOrThrow(manager, meta, brigadierCommand, command.name());
        }
        for (String confirmationLabel : framework.confirmationCommandLabels()) {
            this.assertNoCollision(manager, confirmationLabel);
            BrigadierCommand brigadierCommand = new BrigadierCommand(this.confirmationBuilder(framework, confirmationLabel));
            CommandMeta meta = manager.metaBuilder(confirmationLabel)
                    .plugin(this.plugin)
                    .build();
            this.registerOrThrow(manager, meta, brigadierCommand, confirmationLabel);
        }
    }

    private void registerOrThrow(CommandManager manager, CommandMeta meta, BrigadierCommand command, String label) {
        try {
            manager.register(meta, command);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to register command '" + label + "' with the Velocity proxy", exception);
        }
    }

    private LiteralArgumentBuilder<CommandSource> builder(
            CommandFramework<CommandSource> framework,
            RegisteredCommand command
    ) {
        var literal = BrigadierCommand.literalArgumentBuilder(command.name())
                .executes(context -> {
                    framework.dispatch(context.getSource(), command.name(), "");
                    return 1;
                });

        var args = BrigadierCommand.requiredArgumentBuilder(GREEDY_ARG_NAME, StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    framework.suggest(context.getSource(), command.name(), builder.getRemaining()).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(context -> {
                    framework.dispatch(context.getSource(), command.name(), StringArgumentType.getString(context, GREEDY_ARG_NAME));
                    return 1;
                });

        literal.then(args);
        return literal;
    }

    private LiteralArgumentBuilder<CommandSource> confirmationBuilder(
            CommandFramework<CommandSource> framework,
            String confirmationLabel
    ) {
        return BrigadierCommand.literalArgumentBuilder(confirmationLabel)
                .executes(context -> {
                    framework.dispatch(context.getSource(), confirmationLabel, "");
                    return 1;
                });
    }

    private void assertNoCollision(CommandManager manager, String label) {
        if (!manager.hasCommand(label)) {
            return;
        }
        CommandMeta meta = manager.getCommandMeta(label);
        Object owner = meta != null ? meta.getPlugin() : null;
        if (owner != null) {
            String ownerClassName = owner.getClass().getName();
            throw new IllegalStateException("Command registration conflict for '" + label + "' with " + ownerClassName);
        }
        throw new IllegalStateException("Command registration conflict for '" + label + "'");
    }

    private static final class VelocityActor implements CommandActor {
        private final CommandSource source;

        private VelocityActor(CommandSource source) {
            this.source = source;
        }

        @Override
        public String name() {
            if (this.source instanceof Player player) {
                return player.getUsername();
            }
            return this.source.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        }

        @Override
        public UUID uniqueId() {
            if (this.source instanceof Player player) {
                return player.getUniqueId();
            }
            return UUID.nameUUIDFromBytes((
                    "velocity:" + this.source.getClass().getName()
                            + ":" + Integer.toHexString(System.identityHashCode(this.source)))
                    .getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isPlayer() {
            return this.source instanceof Player;
        }

        @Override
        public boolean hasPermission(String permission) {
            return this.source.hasPermission(permission);
        }

        @Override
        public void sendMessage(Component message) {
            // Velocity API is thread-safe (Netty-based), no scheduler needed unlike Paper.
            // A TOCTOU between this check and the send is harmless: Velocity's sendMessage is a
            // no-op on a disconnected player, not an exception. Any exception propagating from
            // here is a genuine bug and must not be silenced.
            if (!this.isAvailable()) {
                return;
            }
            this.source.sendMessage(message);
        }

        @Override
        public boolean isAvailable() {
            return !(this.source instanceof Player player) || player.isActive();
        }

        @Override
        public Object platformSender() {
            return this.source;
        }
    }

    private record VelocityPlayerResolver(
            ProxyServer server,
            BiPredicate<CommandActor, Player> visibilityFilter
    ) implements ArgumentResolver<Player> {
        @Override
        public Class<Player> type() {
            return Player.class;
        }

        @Override
        public Player resolve(ArgumentResolutionContext context, String input)
                throws ArgumentResolveException {
            if (input.isEmpty() || input.length() > MAX_PLAYER_NAME_LENGTH) {
                throw new ArgumentResolveException("player", input, "Invalid player name");
            }
            Optional<Player> player = this.server.getPlayer(input);
            if (player.isEmpty() || !player.get().isActive()) {
                throw new ArgumentResolveException("player", input, "Player not found");
            }
            return player.get();
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            // Velocity has no native canSee/vanish concept, so the default filter accepts every
            // player. Consumers that care about proxy-level visibility (vanish plugins, network
            // permission gates) can inject a predicate via VelocityCommandFramework.velocity(..).
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            return this.server.getAllPlayers().stream()
                    .filter(Player::isActive)
                    .filter(target -> this.visibilityFilter.test(actor, target))
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowered))
                    .sorted()
                    .toList();
        }
    }
}
