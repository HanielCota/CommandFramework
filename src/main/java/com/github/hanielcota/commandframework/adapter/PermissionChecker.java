package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import org.bukkit.command.CommandSender;

/**
 * Verifica permissões de comandos e envia mensagens de erro quando necessário.
 */
public class PermissionChecker {
    private final GlobalErrorHandler errorHandler;

    /**
     * Cria uma instância do verificador de permissões.
     *
     * @param errorHandler Handler para mensagens de erro
     */
    public PermissionChecker(GlobalErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Verifica a permissão da classe do comando.
     *
     * @param sender     Quem executou o comando
     * @param permission Permissão requerida (null = sem permissão necessária)
     * @return true se tem permissão ou se não há permissão requerida
     */
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

    /**
     * Verifica a permissão do método do comando.
     *
     * @param sender     Quem executou o comando
     * @param permission Permissão requerida (null = sem permissão necessária)
     * @return true se tem permissão ou se não há permissão requerida
     */
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

