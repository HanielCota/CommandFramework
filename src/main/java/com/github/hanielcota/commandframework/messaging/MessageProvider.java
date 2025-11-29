package com.github.hanielcota.commandframework.messaging;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Optional;

public interface MessageProvider {

    Optional<Component> noPermission(CommandSender sender, String permission);

    Optional<Component> invalidUsage(CommandSender sender, String usage);

    Optional<Component> targetOffline(CommandSender sender, String targetName);

    Optional<Component> subCommandNotFound(CommandSender sender, String label);

    Optional<Component> internalError(CommandSender sender, Throwable error);

    Optional<Component> parsingError(CommandSender sender, String input);

    Optional<Component> cooldown(CommandSender sender, java.time.Duration remaining);

    Locale locale(CommandSender sender);
}


