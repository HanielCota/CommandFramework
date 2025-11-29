package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import org.bukkit.command.CommandSender;

public class PermissionChecker {
    private final GlobalErrorHandler errorHandler;

    public PermissionChecker(GlobalErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public boolean checkClassPermission(CommandSender sender, RequiredPermission permission) {
        if (permission == null) {
            return true;
        }
        if (sender.hasPermission(permission.value())) {
            return true;
        }
        sendNoPermissionMessage(sender, permission.value());
        return false;
    }

    public boolean checkMethodPermission(CommandSender sender, RequiredPermission permission) {
        if (permission == null) {
            return true;
        }
        if (sender.hasPermission(permission.value())) {
            return true;
        }
        sendNoPermissionMessage(sender, permission.value());
        return false;
    }

    private void sendNoPermissionMessage(CommandSender sender, String permission) {
        errorHandler.handleNoPermission(sender, permission)
                .ifPresent(sender::sendMessage);
    }
}

