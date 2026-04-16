package com.example.velocitydemo.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import io.github.hanielcota.commandframework.annotation.Arg;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Optional;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "kick", description = "Kick a player from the proxy")
@Permission("velocitysample.kick")
public final class KickCommand {

    @Execute
    @Description("Kick a player (with optional reason)")
    public void kick(
            @Sender CommandSource sender,
            Player target,
            @Arg(greedy = true) @Optional("You have been kicked.") String reason
    ) {
        target.disconnect(Component.text(reason, NamedTextColor.RED));
        sender.sendMessage(Component.text(
                "Kicked " + target.getUsername() + ": " + reason,
                NamedTextColor.GRAY));
    }

    @Execute(sub = "all")
    @Description("Kick every online player (destructive)")
    @Permission("velocitysample.kick.all")
    @Confirm(expireSeconds = 15, commandName = "kick-confirm")
    public void kickAll(@Sender CommandSource sender) {
        sender.sendMessage(Component.text("Kicking all players... (stub)", NamedTextColor.DARK_RED));
    }
}
