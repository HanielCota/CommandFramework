package com.github.hanielcota.commandframework.adapter;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.util.logging.Logger;

public class CommandMapProvider {
    private static final Logger LOGGER = Logger.getLogger(CommandMapProvider.class.getSimpleName());
    private static CommandMap cachedCommandMap;

    public CommandMap getCommandMap() {
        if (cachedCommandMap != null) {
            return cachedCommandMap;
        }
        return fetchAndCacheCommandMap();
    }

    private CommandMap fetchAndCacheCommandMap() {
        try {
            var server = Bukkit.getServer();
            var method = server.getClass().getMethod("getCommandMap");
            cachedCommandMap = (CommandMap) method.invoke(server);
            return cachedCommandMap;
        } catch (Exception e) {
            LOGGER.severe("[CommandFramework] Erro ao obter CommandMap: " + e.getMessage());
            return null;
        }
    }
}

