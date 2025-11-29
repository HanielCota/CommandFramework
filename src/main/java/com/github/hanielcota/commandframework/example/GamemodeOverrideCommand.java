package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.Command;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Exemplo de override seguro de comando vanilla.
 * O framework só faz override se overrideVanilla = true.
 */
@Command(
    name = "gamemode",
    description = "Override seguro do comando /gamemode vanilla",
    overrideVanilla = true
)
public class GamemodeOverrideCommand {

    @DefaultCommand
    @RequiredPermission("framework.gamemode.use")
    public Component defaultHandler(CommandSender sender) {
        if (sender == null) {
            return Component.empty();
        }

        if (!(sender instanceof Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }

        var current = player.getGameMode();
        var message = Component.text()
            .append(Component.text("Seu gamemode atual: ", NamedTextColor.GRAY))
            .append(Component.text(current.name(), NamedTextColor.GREEN))
            .build();

        return message;
    }

    @SubCommand("set")
    @RequiredPermission("framework.gamemode.set")
    public Component setGamemode(CommandSender sender, GameMode gamemode) {
        if (sender == null) {
            return Component.empty();
        }

        if (gamemode == null) {
            return Component.text("Gamemode inválido.", NamedTextColor.RED);
        }

        if (!(sender instanceof Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }

        player.setGameMode(gamemode);
        var message = Component.text()
            .append(Component.text("Gamemode alterado para: ", NamedTextColor.GREEN))
            .append(Component.text(gamemode.name(), NamedTextColor.YELLOW))
            .build();

        return message;
    }
}

