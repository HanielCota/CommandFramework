package io.github.hanielcota.commandframework.paper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolveException;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandFramework;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.PlatformBridge;
import io.github.hanielcota.commandframework.RegisteredCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class PaperPlatformBridge implements PlatformBridge<CommandSender> {

    // Bedrock/Floodgate usernames legitimately include '.' and '*', so the resolver
    // only enforces an upper bound and defers existence checks to the server lookup.
    private static final int MAX_PLAYER_NAME_LENGTH = 32;
    // Name of the single greedy-string Brigadier argument used to forward the raw tail of
    // every framework-registered command. Referenced both where the argument is declared and
    // where it is retrieved from the parsed context - keep both sides in sync.
    private static final String GREEDY_ARG_NAME = "args";
    // Initial StringBuilder capacity for the command-collision error message. Covers the fixed
    // prefix/suffix plus a couple of labels with conflict details before the buffer needs to grow.
    private static final int CONFLICT_MESSAGE_CAPACITY = 256;

    private final JavaPlugin plugin;
    private final Server server;
    private final FrameworkLogger logger;

    PaperPlatformBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = FrameworkLogger.jul(plugin.getLogger());
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
                this.assertRegistered(commands, command, registered);
            }
            for (String confirmationLabel : framework.confirmationCommandLabels()) {
                LiteralCommandNode<CommandSourceStack> node = this.confirmationNode(framework, confirmationLabel).build();
                Set<String> registered = commands.register(node, "Framework confirmation command", List.of());
                this.assertRegistered(commands, confirmationLabel, registered);
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

        var args = Commands.argument(GREEDY_ARG_NAME, StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    framework.suggest(context.getSource().getSender(), command.name(), builder.getRemaining())
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(context -> {
                    framework.dispatch(
                            context.getSource().getSender(),
                            command.name(),
                            StringArgumentType.getString(context, GREEDY_ARG_NAME)
                    );
                    return 1;
                });

        literal.then(args);
        return literal;
    }

    private LiteralArgumentBuilder<CommandSourceStack> confirmationNode(
            CommandFramework<CommandSender> framework,
            String confirmationLabel) {
        return Commands.literal(confirmationLabel)
                .executes(context -> {
                    framework.dispatch(context.getSource().getSender(), confirmationLabel, "");
                    return 1;
                });
    }

    private void assertRegistered(
            Commands commands,
            RegisteredCommand command,
            Set<String> registered
    ) {
        this.assertRegistered(commands, command.name(), command.aliases(), registered);
    }

    private void assertRegistered(Commands commands, String label, Set<String> registered) {
        this.assertRegistered(commands, label, List.of(), registered);
    }

    private void assertRegistered(Commands commands, String label, List<String> aliases, Set<String> registered) {
        Set<String> normalized = this.normalizedRegisteredLabels(registered);
        List<String> missing = new ArrayList<>();
        if (!normalized.contains(label.toLowerCase(Locale.ROOT))) {
            missing.add(label);
        }
        for (String alias : aliases) {
            if (!normalized.contains(alias.toLowerCase(Locale.ROOT))) {
                missing.add(alias);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder(CONFLICT_MESSAGE_CAPACITY)
                .append("Paper command registration conflict. Could not register ");
        for (int index = 0; index < missing.size(); index++) {
            if (index > 0) {
                message.append(", ");
            }
            String missingLabel = missing.get(index);
            message.append('\'').append(missingLabel).append('\'')
                    .append(this.conflictDetails(commands, missingLabel));
        }
        message.append(". Rename the conflicting command or alias.");
        throw new IllegalStateException(message.toString());
    }

    private Set<String> normalizedRegisteredLabels(Set<String> registered) {
        Set<String> normalized = new LinkedHashSet<>();

        for (String value : registered) {
            normalized.add(value.toLowerCase(Locale.ROOT));
            int separatorIndex = value.indexOf(':');

            if (separatorIndex >= 0 && separatorIndex + 1 < value.length()) {
                normalized.add(value.substring(separatorIndex + 1).toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
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
            return senderUniqueId(this.sender);
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
            if (!this.server.isPrimaryThread()) {
                if (!this.plugin.isEnabled()) {
                    return;
                }
                try {
                    this.server.getGlobalRegionScheduler().execute(this.plugin, () -> {
                        if (this.isAvailable()) {
                            this.sender.sendMessage(message);
                        }
                    });
                } catch (IllegalStateException | IllegalArgumentException ignored) {
                    // Plugin was disabled between the isEnabled() check and the schedule call.
                    // The scheduler rejects tasks for disabled plugins; dropping is correct because
                    // there is no main-thread context left to deliver the message on.
                }
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

        private static UUID senderUniqueId(CommandSender sender) {
            if (sender instanceof Entity entity) {
                return entity.getUniqueId();
            }
            return UUID.nameUUIDFromBytes(senderIdentityKey(sender).getBytes(StandardCharsets.UTF_8));
        }

        private static String senderIdentityKey(CommandSender sender) {
            if (sender instanceof RemoteConsoleCommandSender) {
                return "paper:remote-console";
            }
            if (sender instanceof ConsoleCommandSender) {
                return "paper:console";
            }
            if (sender instanceof BlockCommandSender blockSender) {
                Block block = blockSender.getBlock();
                return "paper:command-block:" + block.getWorld().getUID()
                        + ":" + block.getX()
                        + ":" + block.getY()
                        + ":" + block.getZ();
            }
            return "paper:" + sender.getClass().getName()
                    + ":" + sender.getName()
                    + ":" + Integer.toHexString(System.identityHashCode(sender));
        }
    }

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
            // Respect Player#canSee so vanished/hidden players are not leaked through
            // tab completion. When the actor is not a player (console, command block),
            // no visibility filter applies.
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            Player viewer = actor.platformSender() instanceof Player p ? p : null;
            return this.server.getOnlinePlayers().stream()
                    .filter(target -> viewer == null || viewer.canSee(target))
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
