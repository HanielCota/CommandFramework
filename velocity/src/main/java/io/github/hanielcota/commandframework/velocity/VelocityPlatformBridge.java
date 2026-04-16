package io.github.hanielcota.commandframework.velocity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.*;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

final class VelocityPlatformBridge implements PlatformBridge<CommandSource> {

    private final ProxyServer server;
    private final Object plugin;
    private final Logger logger;

    VelocityPlatformBridge(ProxyServer server, Object plugin) {
        this.server = server;
        this.plugin = plugin;
        this.logger = Logger.getLogger(plugin.getClass().getName());
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
    public Logger logger() {
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
        return List.of(new VelocityPlayerResolver(this.server));
    }

    @Override
    public void register(CommandFramework<CommandSource> framework) {
        CommandManager manager = this.server.getCommandManager();
        for (RegisteredCommand command : framework.registeredCommands()) {
            this.logCollision(manager, command.name());
            command.aliases().forEach(alias -> this.logCollision(manager, alias));

            BrigadierCommand brigadierCommand = new BrigadierCommand(this.builder(framework, command));
            CommandMeta meta = manager.metaBuilder(command.name())
                    .plugin(this.plugin)
                    .aliases(command.aliases().toArray(String[]::new))
                    .build();
            this.tryRegister(manager, meta, brigadierCommand, command.name());
        }
        for (String confirmationLabel : framework.confirmationCommandLabels()) {
            this.logCollision(manager, confirmationLabel);
            BrigadierCommand brigadierCommand = new BrigadierCommand(this.confirmationBuilder(framework, confirmationLabel));
            CommandMeta meta = manager.metaBuilder(confirmationLabel)
                    .plugin(this.plugin)
                    .build();
            this.tryRegister(manager, meta, brigadierCommand, confirmationLabel);
        }
    }

    private void tryRegister(CommandManager manager, CommandMeta meta, BrigadierCommand command, String label) {
        try {
            manager.register(meta, command);
        } catch (RuntimeException exception) {
            this.logger.log(
                    java.util.logging.Level.SEVERE,
                    exception,
                    () -> "Failed to register command '" + label + "' with the Velocity proxy"
            );
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

        var args = BrigadierCommand.requiredArgumentBuilder("args", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    framework.suggest(context.getSource(), command.name(), builder.getRemaining()).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(context -> {
                    framework.dispatch(context.getSource(), command.name(), StringArgumentType.getString(context, "args"));
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

    private void logCollision(CommandManager manager, String label) {
        if (!manager.hasCommand(label)) {
            return;
        }
        CommandMeta meta = manager.getCommandMeta(label);
        Object owner = meta != null ? meta.getPlugin() : null;
        if (owner != null) {
            String ownerClassName = owner.getClass().getName();
            this.logger.warning(() -> "Command registration conflict for '" + label + "' with " + ownerClassName);
            return;
        }
        this.logger.warning(() -> "Command registration conflict for '" + label + "'");
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
            return UUID.nameUUIDFromBytes(("velocity:" + this.name()).getBytes(StandardCharsets.UTF_8));
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

    // Velocity proxies Bedrock clients whose names include '.' and '*', so the resolver
    // only enforces an upper bound and defers existence checks to the proxy lookup.
    private static final int MAX_PLAYER_NAME_LENGTH = 32;

    private record VelocityPlayerResolver(ProxyServer server) implements ArgumentResolver<Player> {
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
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            return this.server.getAllPlayers().stream()
                    .filter(Player::isActive)
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowered))
                    .sorted()
                    .toList();
        }
    }
}
