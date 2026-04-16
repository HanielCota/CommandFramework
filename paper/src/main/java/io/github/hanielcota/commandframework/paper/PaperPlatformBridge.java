package io.github.hanielcota.commandframework.paper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.hanielcota.commandframework.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

final class PaperPlatformBridge implements PlatformBridge<CommandSender> {

    private final JavaPlugin plugin;
    private final Server server;

    PaperPlatformBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
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
        return this.plugin.getLogger();
    }

    @Override
    public CommandActor createActor(CommandSender sender) {
        return new PaperActor(sender, this.plugin, this.server);
    }

    @Override
    public boolean supportsSenderType(Class<?> type) {
        return type == CommandActor.class
                || type == CommandSender.class
                || type == Player.class;
    }

    @Override
    public boolean isPlayerSenderType(Class<?> type) {
        return type == Player.class;
    }

    @Override
    public List<ArgumentResolver<?>> platformResolvers() {
        return List.of(new PaperPlayerResolver(this.server), new PaperWorldResolver(this.server));
    }

    @Override
    public void register(CommandFramework<CommandSender> framework) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var commands = event.registrar();
            for (RegisteredCommand command : framework.registeredCommands()) {
                LiteralCommandNode<CommandSourceStack> node = this.node(framework, command).build();
                Set<String> registered = commands.register(node, command.description(), command.aliases());
                this.logRegistrationConflicts(commands, command, registered);
            }
            for (String confirmationLabel : framework.confirmationCommandLabels()) {
                LiteralCommandNode<CommandSourceStack> node = this.confirmationNode(framework, confirmationLabel).build();
                Set<String> registered = commands.register(node, "Framework confirmation command", List.of());
                this.logRegistrationConflicts(commands, confirmationLabel, registered);
            }
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> node(
            CommandFramework<CommandSender> framework,
            RegisteredCommand command
    ) {
        var literal = Commands.literal(command.name())
                .executes(context -> {
                    framework.dispatch(context.getSource().getSender(), command.name(), "");
                    return 1;
                });

        var args = Commands.argument("args", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    framework.suggest(context.getSource().getSender(), command.name(), builder.getRemaining())
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(context -> {
                    framework.dispatch(
                            context.getSource().getSender(),
                            command.name(),
                            StringArgumentType.getString(context, "args")
                    );
                    return 1;
                });

        literal.then(args);
        return literal;
    }

    private LiteralArgumentBuilder<CommandSourceStack> confirmationNode(
            CommandFramework<CommandSender> framework,
            String confirmationLabel
    ) {
        return Commands.literal(confirmationLabel)
                .executes(context -> {
                    framework.dispatch(context.getSource().getSender(), confirmationLabel, "");
                    return 1;
                });
    }

    private void logRegistrationConflicts(
            Commands commands,
            RegisteredCommand command,
            Set<String> registered
    ) {
        this.logRegistrationConflicts(commands, command.name(), command.aliases(), registered);
    }

    private void logRegistrationConflicts(Commands commands, String label, Set<String> registered) {
        this.logRegistrationConflicts(commands, label, List.of(), registered);
    }

    private void logRegistrationConflicts(Commands commands, String label, List<String> aliases, Set<String> registered) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : registered) {
            normalized.add(value.toLowerCase(Locale.ROOT));
            int separatorIndex = value.indexOf(':');
            if (separatorIndex >= 0 && separatorIndex + 1 < value.length()) {
                normalized.add(value.substring(separatorIndex + 1).toLowerCase(Locale.ROOT));
            }
        }

        if (!normalized.contains(label.toLowerCase(Locale.ROOT))) {
            this.logger().warning("Command registration conflict for '" + label + "'" + this.conflictDetails(commands, label));
        }
        for (String alias : aliases) {
            if (!normalized.contains(alias.toLowerCase(Locale.ROOT))) {
                this.logger().warning("Command alias registration conflict for '" + alias + "'" + this.conflictDetails(commands, alias));
            }
        }
    }

    private String conflictDetails(Commands commands, String label) {
        CommandNode<CommandSourceStack> existing =
                commands.getDispatcher().getRoot().getChild(label);
        if (existing == null) {
            return "";
        }
        Command<CommandSourceStack> command = existing.getCommand();
        String commandType = command == null ? "unknown-command" : command.getClass().getName();
        return " (existing node=" + existing.getClass().getName() + ", command=" + commandType + ")";
    }

    private static final class PaperActor implements CommandActor {
        private final CommandSender sender;
        private final JavaPlugin plugin;
        private final Server server;

        private PaperActor(CommandSender sender, JavaPlugin plugin, Server server) {
            this.sender = sender;
            this.plugin = plugin;
            this.server = server;
        }

        @Override
        public String name() {
            return this.sender.getName();
        }

        @Override
        public UUID uniqueId() {
            if (this.sender instanceof Entity entity) {
                return entity.getUniqueId();
            }
            return UUID.nameUUIDFromBytes(("paper:" + this.sender.getName()).getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isPlayer() {
            return this.sender instanceof Player;
        }

        @Override
        public boolean hasPermission(String permission) {
            return this.sender.hasPermission(permission);
        }

        @Override
        public void sendMessage(Component message) {
            if (!this.isAvailable()) {
                return;
            }
            if (Thread.currentThread().isVirtual()) {
                this.server.getGlobalRegionScheduler().execute(this.plugin, () -> {
                    if (this.isAvailable()) {
                        this.sender.sendMessage(message);
                    }
                });
                return;
            }
            this.sender.sendMessage(message);
        }

        @Override
        public boolean isAvailable() {
            return !(this.sender instanceof Player player) || player.isOnline();
        }

        @Override
        public Object platformSender() {
            return this.sender;
        }
    }

    // Bedrock/Floodgate usernames legitimately include '.' and '*', so the resolver
    // only enforces an upper bound and defers existence checks to the server lookup.
    private static final int MAX_PLAYER_NAME_LENGTH = 32;

    private record PaperPlayerResolver(Server server) implements ArgumentResolver<Player> {
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
            Player player = this.server.getPlayerExact(input);
            if (player == null || !player.isOnline()) {
                throw new ArgumentResolveException("player", input, "Player not found");
            }
            return player;
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            return this.server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowered))
                    .sorted()
                    .toList();
        }
    }

    private record PaperWorldResolver(Server server) implements ArgumentResolver<World> {
        @Override
        public Class<World> type() {
            return World.class;
        }

        @Override
        public World resolve(ArgumentResolutionContext context, String input)
                throws ArgumentResolveException {
            World world = this.server.getWorld(input);
            if (world == null) {
                throw new ArgumentResolveException("world", input, "World not found");
            }
            return world;
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            return this.server.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowered))
                    .sorted()
                    .toList();
        }
    }
}
