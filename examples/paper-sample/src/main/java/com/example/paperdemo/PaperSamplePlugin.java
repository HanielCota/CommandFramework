package com.example.paperdemo;

import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperSamplePlugin extends JavaPlugin {

    private EconomyService economy;

    @Override
    public void onEnable() {
        this.economy = new EconomyService();

        PaperCommandFramework.paper(this)
                .bind(EconomyService.class, this.economy)
                .scanPackage("com.example.paperdemo.commands")
                .build();

        this.getLogger().info("PaperSample loaded. Try: /heal, /eco, /eco pay <player> <amount>");
    }
}
