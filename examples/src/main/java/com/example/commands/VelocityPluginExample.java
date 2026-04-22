package com.example.commands;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.velocity.VelocityCommandFramework;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(id = "command-framework-example", name = "Command Framework Example", version = "1.0.0")
public final class VelocityPluginExample {

    private final ProxyServer server;
    private final Logger logger;
    private VelocityCommandFramework commands;

    @Inject
    public VelocityPluginExample(ProxyServer server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        Objects.requireNonNull(event, "event");
        try {
            commands = VelocityCommandFramework.builder(server, this)
                    .messageProvider(new CustomMessageProvider())
                    .build();
            commands.registerAnnotated(new KitCommand());
            logger.info("Comandos registrados com sucesso.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Falha ao registrar comandos", exception);
        }
    }
}
