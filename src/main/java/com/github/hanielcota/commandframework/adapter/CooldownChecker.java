package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Cooldown;
import com.github.hanielcota.commandframework.cooldown.CooldownKey;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

/**
 * Responsável por verificar e aplicar cooldowns em comandos.
 * Prioriza cooldowns definidos em métodos sobre cooldowns de classe.
 */
public class CooldownChecker {
    private final CooldownService cooldownService;
    private final GlobalErrorHandler errorHandler;

    /**
     * Cria uma instância do verificador de cooldown.
     *
     * @param cooldownService Serviço de gerenciamento de cooldowns
     * @param errorHandler    Handler para mensagens de erro
     */
    public CooldownChecker(CooldownService cooldownService, GlobalErrorHandler errorHandler) {
        this.cooldownService = cooldownService;
        this.errorHandler = errorHandler;
    }

    /**
     * Verifica se o comando está em cooldown e aplica o cooldown se necessário.
     * Se o comando estiver em cooldown, envia mensagem de erro ao sender.
     *
     * @param sender        Quem executou o comando
     * @param commandName   Nome do comando
     * @param methodName    Nome do método executado
     * @param methodCooldown Cooldown do método (prioritário)
     * @param classCooldown  Cooldown da classe (fallback)
     * @return true se o comando pode ser executado, false se está em cooldown
     */
    public boolean checkAndApplyCooldown(CommandSender sender, String commandName, String methodName, Cooldown methodCooldown, Cooldown classCooldown) {
        var cooldown = resolveCooldown(methodCooldown, classCooldown);
        if (cooldown == null) {
            return true;
        }
        var key = createCooldownKey(sender, commandName, methodName);
        if (isOnCooldown(key)) {
            sendCooldownMessage(sender, key);
            return false;
        }
        applyCooldown(key, cooldown);
        return true;
    }

    private Cooldown resolveCooldown(Cooldown methodCooldown, Cooldown classCooldown) {
        return methodCooldown != null ? methodCooldown : classCooldown;
    }

    private CooldownKey createCooldownKey(CommandSender sender, String commandName, String methodName) {
        var uuid = extractUuid(sender);
        return new CooldownKey(uuid, commandName, methodName);
    }

    private UUID extractUuid(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private boolean isOnCooldown(CooldownKey key) {
        var remainingTime = cooldownService.getRemainingTime(key);
        return remainingTime.isPresent();
    }

    private void sendCooldownMessage(CommandSender sender, CooldownKey key) {
        var remainingTime = cooldownService.getRemainingTime(key);
        remainingTime.ifPresent(duration -> errorHandler.handleCooldown(sender, duration));
    }

    private void applyCooldown(CooldownKey key, Cooldown cooldown) {
        var duration = Duration.ofSeconds(cooldown.seconds());
        cooldownService.putOnCooldown(key, duration);
    }
}

