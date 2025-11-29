package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Exemplo de subcomandos de dois níveis.
 * Demonstra: "admin player set", "admin player get", "admin config reload"
 */
@Command(
    name = "admin",
    description = "Comando administrativo com subcomandos de dois níveis"
)
@RequiredPermission("framework.admin.use")
public class TwoLevelSubCommandExample {

    @DefaultCommand
    public Component defaultHandler(CommandSender sender) {
        if (sender == null) {
            return Component.empty();
        }

        var message = Component.text()
            .append(Component.text("Comandos disponíveis:", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("  /admin player set <player> <value>", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  /admin player get <player>", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  /admin config reload", NamedTextColor.GRAY))
            .build();

        return message;
    }

    @SubCommand("player set")
    @RequiredPermission("framework.admin.player.set")
    public Component setPlayerValue(CommandSender sender, Player target, Integer value) {
        if (sender == null) {
            return Component.empty();
        }

        if (target == null) {
            return Component.text("Jogador não encontrado.", NamedTextColor.RED);
        }

        if (value == null) {
            return Component.text("Valor inválido.", NamedTextColor.RED);
        }

        var message = Component.text()
            .append(Component.text("Valor de ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text(" definido para: ", NamedTextColor.GREEN))
            .append(Component.text(value, NamedTextColor.YELLOW))
            .build();

        return message;
    }

    @SubCommand("player get")
    @RequiredPermission("framework.admin.player.get")
    public Component getPlayerValue(CommandSender sender, Player target) {
        if (sender == null) {
            return Component.empty();
        }

        if (target == null) {
            return Component.text("Jogador não encontrado.", NamedTextColor.RED);
        }

        var message = Component.text()
            .append(Component.text("Valor de ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text(": ", NamedTextColor.GREEN))
            .append(Component.text("100", NamedTextColor.YELLOW))
            .build();

        return message;
    }

    @SubCommand("config reload")
    @RequiredPermission("framework.admin.config.reload")
    @Cooldown(seconds = 10)
    public Component reloadConfig(CommandSender sender) {
        if (sender == null) {
            return Component.empty();
        }

        var message = Component.text("Configuração recarregada com sucesso!", NamedTextColor.GREEN);
        return message;
    }
}

