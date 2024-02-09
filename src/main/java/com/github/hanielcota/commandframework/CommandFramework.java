package com.github.hanielcota.commandframework;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicComparator;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
public class CommandFramework implements CommandExecutor {

    private final Map<String, Entry<Method, Object>> commandMap = new HashMap<>();
    private CommandMap map;
    private final Plugin plugin;

    public CommandFramework(Plugin plugin) {
        this.plugin = plugin;
        PluginManager pluginManager = Bukkit.getPluginManager();
        try {
            Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            map = (CommandMap) commandMapField.get(pluginManager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error setting up CommandFramework: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull org.bukkit.command.Command cmd,
            @NotNull String label,
            @NotNull  String[] args) {
        return handleCommand(sender, cmd, label, args);
    }

    public boolean handleCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        for (int i = args.length; i >= 0; i--) {
            String cmdLabel = buildCommandLabel(label, args, i);
            if (processCommandLabel(sender, cmd, cmdLabel, args)) {
                return true;
            }
        }
        defaultCommand(new CommandArgs(sender, cmd, label, args, 0));
        return false;
    }

    private String buildCommandLabel(String label, String[] args, int endIndex) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(label.toLowerCase());
        for (int x = 0; x < endIndex; x++) {
            buffer.append(".").append(args[x].toLowerCase());
        }
        return buffer.toString();
    }

    private boolean processCommandLabel(CommandSender sender, org.bukkit.command.Command cmd, String cmdLabel, String[] args) {
        if (commandMap.containsKey(cmdLabel)) {
            Method method = commandMap.get(cmdLabel).getKey();
            Object methodObject = commandMap.get(cmdLabel).getValue();
            Command command = method.getAnnotation(Command.class);

            if (checkPermission(sender, command)) {
                return true;
            }

            if (checkInGameOnly(sender, command)) {
                return true;
            }

            invokeCommandMethod(sender, cmd, cmdLabel, method, methodObject, args);
            return true;
        }

        return false;
    }


    private boolean checkPermission(CommandSender sender, Command command) {
        if (!command.permission().isEmpty() && !sender.hasPermission(command.permission())) {
            sender.sendMessage(command.noPerm());
            return true;
        }
        return false;
    }

    private boolean checkInGameOnly(CommandSender sender, Command command) {
        if (command.inGameOnly() && !(sender instanceof Player)) {
            sender.sendMessage("This command is only performable in-game.");
            return true;
        }
        return false;
    }

    private void invokeCommandMethod(
            CommandSender sender, org.bukkit.command.Command cmd, String cmdLabel, Method method, Object methodObject, String[] args) {
        try {
            method.invoke(methodObject, new CommandArgs(sender, cmd, cmdLabel, args, cmdLabel.split("\\.").length - 1));
        } catch (Exception e) {
            log.error("Error invoking command {}: {}", cmdLabel, e.getMessage(), e);
        }
    }

    public void registerCommands(Object obj) {
        for (Method method : obj.getClass().getMethods()) {
            registerCommandOrCompleter(obj, method);
        }
    }

    private void registerCommandOrCompleter(Object obj, Method method) {
        if (isCommandMethod(method)) {
            registerCommandForMethod(obj, method);
        } else if (isCompleterMethod(method)) {
            registerCompleterForMethod(obj, method);
        }
    }

    private boolean isCommandMethod(Method method) {
        return method.getAnnotation(Command.class) != null
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == CommandArgs.class;
    }

    private boolean isCompleterMethod(Method method) {
        return method.getAnnotation(Completer.class) != null
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == CommandArgs.class
                && method.getReturnType() == List.class;
    }

    private void registerCommandForMethod(Object obj, Method method) {
        Command command = method.getAnnotation(Command.class);
        registerCommand(command, command.name(), method, obj);
        for (String alias : command.aliases()) {
            registerCommand(command, alias, method, obj);
        }
    }

    private void registerCompleterForMethod(Object obj, Method method) {
        Completer completer = method.getAnnotation(Completer.class);
        registerCompleter(completer.name(), method, obj);
        for (String alias : completer.aliases()) {
            registerCompleter(alias, method, obj);
        }
    }

    public void registerHelp() {
        if (plugin == null || map == null) {
            return;
        }

        Set<HelpTopic> help = new TreeSet<>(HelpTopicComparator.helpTopicComparatorInstance());

        for (String label : commandMap.keySet()) {
            if (label.contains(".")) {
                continue;
            }

            org.bukkit.command.Command cmd = map.getCommand(label);
            if (cmd != null) {
                HelpTopic topic = new GenericCommandHelpTopic(cmd);
                help.add(topic);
            }
        }

        IndexHelpTopic topic = new IndexHelpTopic(
                plugin.getName(),
                "All commands for " + plugin.getName(),
                null,
                help,
                "Below is a list of all " + plugin.getName() + " commands:");

        Bukkit.getServer().getHelpMap().addTopic(topic);
    }

    public void registerCommand(Command command, String label, Method method, Object obj) {
        if (command == null || label == null || method == null || obj == null) {
            return;
        }

        String lowercaseLabel = label.toLowerCase();
        commandMap.put(lowercaseLabel, new AbstractMap.SimpleEntry<>(method, obj));
        commandMap.put(plugin.getName() + ':' + lowercaseLabel, new AbstractMap.SimpleEntry<>(method, obj));

        String cmdLabel = lowercaseLabel.split("\\.")[0];
        if (map.getCommand(cmdLabel) == null) {
            org.bukkit.command.Command cmd = new BukkitCommand(cmdLabel, this, plugin);
            map.register(plugin.getName(), cmd);
        }

        org.bukkit.command.Command registeredCommand = map.getCommand(cmdLabel);
        if (registeredCommand != null) {
            if (!command.description().isEmpty() && cmdLabel.equals(lowercaseLabel)) {
                registeredCommand.setDescription(command.description());
            }

            if (!command.usage().isEmpty() && cmdLabel.equals(lowercaseLabel)) {
                registeredCommand.setUsage(command.usage());
            }
        }
    }

    public void registerCompleter(String label, Method m, Object obj) {
        String cmdLabel = label.split("\\.")[0].toLowerCase();
        org.bukkit.command.Command existingCommand = map.getCommand(cmdLabel);

        if (existingCommand == null) {
            registerNewCommand(cmdLabel);
            return;
        }

        if (existingCommand instanceof BukkitCommand command) {
            registerCompleterForBukkitCommand(command, label, m, obj);
            return;
        }

        if (!(existingCommand instanceof PluginCommand)) {
            return;
        }

        try {
            registerCompleterForPluginCommand(existingCommand, label, m, obj);
        } catch (Exception ex) {
            log.error(
                    "Unable to register tab completer {} for command '{}'. A tab completer is already registered for that command!",
                    m.getName(),
                    label);
        }
    }

    private void registerNewCommand(String cmdLabel) {
        org.bukkit.command.Command newCommand = new BukkitCommand(cmdLabel, this, plugin);
        map.register(plugin.getName(), newCommand);
    }

    private void registerCompleterForBukkitCommand(BukkitCommand command, String label, Method m, Object obj) {
        if (command.completer == null) {
            command.completer = new BukkitCompleter();
        }
        command.completer.addCompleter(label, m, obj);
    }

    private void registerCompleterForPluginCommand(Object command, String label, Method m, Object obj) throws Exception {
        Field field = command.getClass().getDeclaredField("completer");
        field.setAccessible(true);

        Object completer = field.get(command);
        if (completer == null) {
            BukkitCompleter newCompleter = new BukkitCompleter();
            newCompleter.addCompleter(label, m, obj);
            field.set(command, newCompleter);
            return;
        }

        if (completer instanceof BukkitCompleter) {
            ((BukkitCompleter) completer).addCompleter(label, m, obj);
            return;
        }

        log.error("Unable to register tab completer {} for command '{}'. A tab completer is already registered for that command!",
                m.getName(),
                label);
    }

    private void defaultCommand(CommandArgs args) {
        args.getSender().sendMessage(args.getLabel() + " is not handled! Oh noes!");
    }
}
