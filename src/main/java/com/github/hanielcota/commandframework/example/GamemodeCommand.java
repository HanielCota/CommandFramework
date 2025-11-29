package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.*;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(
    name = "gm",
    description = "Comando de gamemode simplificado",
    aliases = {"gamemode", "gmode"}
)
@RequiredArgsConstructor
public class GamemodeCommand {

    @DefaultCommand
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
    @RequiredPermission("framework.gm.set")
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

    @SubCommand("player set")
    @RequiredPermission("framework.gm.player.set")
    @Cooldown(seconds = 5)
    public Component setPlayerGamemode(CommandSender sender, org.bukkit.entity.Player target, GameMode gamemode) {
        if (sender == null) {
            return Component.empty();
        }

        if (target == null) {
            return Component.text("Jogador não encontrado.", NamedTextColor.RED);
        }

        if (gamemode == null) {
            return Component.text("Gamemode inválido.", NamedTextColor.RED);
        }

        target.setGameMode(gamemode);
        var message = Component.text()
            .append(Component.text("Gamemode de ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text(" alterado para: ", NamedTextColor.GREEN))
            .append(Component.text(gamemode.name(), NamedTextColor.YELLOW))
            .build();

        return message;
    }
}

