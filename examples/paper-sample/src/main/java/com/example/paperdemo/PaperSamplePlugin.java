package com.example.paperdemo;

import io.github.hanielcota.commandframework.MessageProvider;
import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class PaperSamplePlugin extends JavaPlugin {

    private EconomyService economy;

    @Override
    public void onEnable() {
        this.economy = new EconomyService();
        this.saveResource("messages.yml", false);

        PaperCommandFramework.paper(this)
                .bind(EconomyService.class, this.economy)
                .scanPackage("com.example.paperdemo.commands")
                .messages(MessageProvider.fromStringMap(loadMessages()))
                .build();

        this.getLogger().info("PaperSample loaded. Try: /heal, /eco, /eco pay <player> <amount>");
    }

    private Map<String, String> loadMessages() {
        FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(this.getDataFolder(), "messages.yml"));
        Map<String, String> templates = new HashMap<>();
        for (String key : config.getKeys(false)) {
            String value = config.getString(key);
            if (value != null) {
                templates.put(key, value);
            }
        }
        return templates;
    }
}
