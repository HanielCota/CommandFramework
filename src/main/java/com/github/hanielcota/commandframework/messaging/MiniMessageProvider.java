package com.github.hanielcota.commandframework.messaging;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MiniMessageProvider implements MessageProvider {

    BukkitAudiences audiences;
    MiniMessage miniMessage;

    @Override
    public Optional<Component> noPermission(CommandSender sender, String permission) {
        if (permission == null) {
            return Optional.empty();
        }
        var template = "<red>Você não tem permissão para este comando. <gray>({permission})";
        return Optional.of(parse(template.replace("{permission}", permission)));
    }

    @Override
    public Optional<Component> invalidUsage(CommandSender sender, String usage) {
        if (usage == null) {
            return Optional.empty();
        }
        var template = "<red>Uso inválido. <gray>{usage}";
        return Optional.of(parse(template.replace("{usage}", usage)));
    }

    @Override
    public Optional<Component> targetOffline(CommandSender sender, String targetName) {
        if (targetName == null) {
            return Optional.empty();
        }
        var template = "<red>Jogador {name} está offline.";
        return Optional.of(parse(template.replace("{name}", targetName)));
    }

    @Override
    public Optional<Component> subCommandNotFound(CommandSender sender, String label) {
        if (label == null) {
            return Optional.empty();
        }
        var template = "<red>Subcomando {label} não encontrado.";
        return Optional.of(parse(template.replace("{label}", label)));
    }

    @Override
    public Optional<Component> internalError(CommandSender sender, Throwable error) {
        if (error == null) {
            return Optional.empty();
        }
        var template = "<red>Ocorreu um erro interno ao executar o comando.";
        return Optional.of(parse(template));
    }

    @Override
    public Optional<Component> parsingError(CommandSender sender, String input) {
        if (input == null) {
            return Optional.empty();
        }
        var template = "<red>Não foi possível interpretar \"{input}\".";
        return Optional.of(parse(template.replace("{input}", input)));
    }

    @Override
    public Optional<Component> cooldown(CommandSender sender, java.time.Duration remaining) {
        if (remaining == null) {
            return Optional.empty();
        }
        var seconds = remaining.getSeconds();
        var template = "<red>Você precisa aguardar <white>{seconds}s</white> antes de usar este comando novamente.";
        return Optional.of(parse(template.replace("{seconds}", String.valueOf(seconds))));
    }

    @Override
    public Locale locale(CommandSender sender) {
        return Locale.getDefault();
    }

    public void send(CommandSender sender, Component component) {
        if (sender == null || component == null) {
            return;
        }
        var audience = audiences.sender(sender);
        audience.sendMessage(component);
    }

    private Component parse(String template) {
        if (template == null) {
            return Component.empty();
        }
        return miniMessage.deserialize(template);
    }
}


