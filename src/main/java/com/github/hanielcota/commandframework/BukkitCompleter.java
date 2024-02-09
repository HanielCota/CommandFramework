package com.github.hanielcota.commandframework;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
public class BukkitCompleter implements TabCompleter {

    private final Map<String, Entry<Method, Object>> completer = new HashMap<>();

    public void addCompleter(String label, Method m, Object obj) {
        completer.put(label, new AbstractMap.SimpleEntry<>(m, obj));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        for (int i = args.length; i >= 0; i--) {
            String cmdLabel = buildCmdLabel(label, args, i);
            if (completer.containsKey(cmdLabel)) {
                List<String> completions = invokeCompleter(sender, command, label, args, cmdLabel);
                if (!completions.isEmpty()) {
                    return completions;
                }
            }
        }

        return Collections.emptyList();
    }

    private String buildCmdLabel(String label, String[] args, int index) {
        StringBuilder buffer = new StringBuilder(label.toLowerCase());
        for (int x = 0; x < index; x++) {
            if (!args[x].isEmpty() && !args[x].equals(" ")) {
                buffer.append(".").append(args[x].toLowerCase());
            }
        }
        return buffer.toString();
    }

    private List<String> invokeCompleter(CommandSender sender, Command command, String label, String[] args, String cmdLabel) {
        Entry<Method, Object> entry = completer.get(cmdLabel);

        if (entry == null) {
            log.error("Completer for command '{}' and label '{}' not found.", label, cmdLabel);
            return Collections.emptyList();
        }

        try {
            Object result = entry.getKey()
                    .invoke(entry.getValue(),
                            new CommandArgs(sender, command, label, args, cmdLabel.split("\\.").length - 1));

            if (result instanceof List) {
                return (List<String>) result;
            }

        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Error invoking completer for command '{}' and label '{}': {}", label, cmdLabel, e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}
