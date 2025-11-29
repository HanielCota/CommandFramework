package com.github.hanielcota.commandframework.error;

import com.github.hanielcota.commandframework.messaging.MessageProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GlobalErrorHandler {

    MessageProvider messageProvider;

    public Optional<Component> handleNoPermission(CommandSender sender, String permission) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.noPermission(sender, permission);
    }

    public Optional<Component> handleInvalidUsage(CommandSender sender, String usage) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.invalidUsage(sender, usage);
    }

    public Optional<Component> handleTargetOffline(CommandSender sender, String targetName) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.targetOffline(sender, targetName);
    }

    public Optional<Component> handleSubCommandNotFound(CommandSender sender, String label) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.subCommandNotFound(sender, label);
    }

    public Optional<Component> handleInternalError(CommandSender sender, Throwable error) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.internalError(sender, error);
    }

    public Optional<Component> handleParsingError(CommandSender sender, String input) {
        if (sender == null) {
            return Optional.empty();
        }

        return messageProvider.parsingError(sender, input);
    }

    public void handleCooldown(CommandSender sender, java.time.Duration remaining) {
        if (sender == null) {
            return;
        }

        if (remaining == null) {
            return;
        }

        var message = messageProvider.cooldown(sender, remaining);
        if (message.isEmpty()) {
            return;
        }

        sender.sendMessage(message.get());
    }
}


