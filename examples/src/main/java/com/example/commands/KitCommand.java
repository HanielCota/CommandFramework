package com.example.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Default;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.OnlyPlayer;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.CommandActor;
import java.util.concurrent.TimeUnit;

@Command("kit")
@Description("Sistema de kits")
public final class KitCommand {

    @Default
    @OnlyPlayer
    @Description("Comando principal do kit")
    public void onDefault(CommandActor actor) {
        actor.sendMessage("§eUse §f/kit give <jogador>§e.");
    }

    @Subcommand("give")
    @Permission("kit.give")
    @Cooldown(value = 3, unit = TimeUnit.SECONDS)
    @Description("Envia um kit para um jogador")
    public void onGive(CommandActor actor, String target) {
        if (target.isBlank()) {
            actor.sendMessage("§cInforme o nome do jogador.");
            return;
        }
        actor.sendMessage("§aKit enviado para §f" + target + "§a.");
    }
}
