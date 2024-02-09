package com.github.hanielcota.commandframework;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@Slf4j
public class BukkitCommand extends org.bukkit.command.Command {

    private final Plugin owningPlugin;
    private final CommandExecutor executor;
    protected BukkitCompleter completer;

    protected BukkitCommand(String label, CommandExecutor executor, Plugin owner) {
        super(label);
        this.executor = executor;
        this.owningPlugin = owner;
        this.usageMessage = "";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!checkPluginStateAndPermissions(sender)) {
            return false;
        }

        if (!executeCommand(sender, commandLabel, args)) {
            sendUsageMessage(sender, commandLabel);
            return false;
        }

        return true;
    }

    private boolean checkPluginStateAndPermissions(CommandSender sender) {
        return owningPlugin.isEnabled() && testPermission(sender);
    }

    private boolean executeCommand(CommandSender sender, String commandLabel, String[] args) {
        try {
            return executor.onCommand(sender, this, commandLabel, args);
        } catch (CommandException e) {
            sender.sendMessage(e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid argument: " + e.getMessage());
            return false;
        } catch (Exception e) {
            sender.sendMessage("An unexpected error occurred. Please try again later.");
            return false;
        }
    }

    private void sendUsageMessage(CommandSender sender, String commandLabel) {
        if (!usageMessage.isEmpty()) {
            for (String line : usageMessage.replace("<command>", commandLabel).split("\n")) {
                sender.sendMessage(line);
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        try {
            Validate.notNull(sender, "Sender cannot be null");
            Validate.notNull(args, "Arguments cannot be null");
            Validate.notNull(alias, "Alias cannot be null");

            return handleTabCompletion(sender, alias, args);

        } catch (IllegalArgumentException | CommandException e) {
            sender.sendMessage(e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error during tab completion", e);
            sender.sendMessage("An unexpected error occurred during tab completion.");
            return Collections.emptyList();
        }
    }

    private List<String> handleTabCompletion(CommandSender sender, String alias, String[] args) {
        try {
            return getCompletionsFromCompleterOrExecutor(sender, alias, args);
        } catch (IllegalArgumentException | CommandException e) {
            sender.sendMessage(e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error during tab completion", e);
            sender.sendMessage("An unexpected error occurred during tab completion.");
            return Collections.emptyList();
        }
    }

    private List<String> getCompletionsFromCompleterOrExecutor(CommandSender sender, String alias, String[] args) {
        if (completer != null) {
            return completer.onTabComplete(sender, this, alias, args);
        }

        if (executor instanceof TabCompleter tabCompleter) {
            return tabCompleter.onTabComplete(sender, this, alias, args);
        }

        return Collections.emptyList();
    }
}
