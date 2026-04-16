package com.example.velocitydemo.commands;

import com.velocitypowered.api.proxy.Player;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "find", description = "Find which backend server a player is on")
@Permission("velocitysample.find")
public final class FindCommand {

    @Execute
    @Description("Locate a player on the network")
    public void find(@Sender CommandSource sender, Player target) {
        target.getCurrentServer().ifPresentOrElse(
                connection -> sender.sendMessage(Component.text(
                        target.getUsername() + " is on " + connection.getServerInfo().getName(),
                        NamedTextColor.GREEN)),
                () -> sender.sendMessage(Component.text(
                        target.getUsername() + " is connected but not on any backend",
                        NamedTextColor.YELLOW)));
    }
}
