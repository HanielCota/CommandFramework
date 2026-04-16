package com.example.velocitydemo;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.velocity.VelocityCommandFramework;
import java.util.logging.Logger;

@Plugin(
        id = "velocity-sample",
        name = "VelocitySample",
        version = "1.0.0",
        description = "CommandFramework velocity-sample demonstrating find/kick commands.",
        authors = {"HanielCota"}
)
public final class VelocitySamplePlugin {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public VelocitySamplePlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        VelocityCommandFramework.velocity(this.server, this)
                .scanPackage("com.example.velocitydemo.commands")
                .build();

        this.logger.info("VelocitySample loaded. Try: /find <player>, /kick <player>");
    }
}
