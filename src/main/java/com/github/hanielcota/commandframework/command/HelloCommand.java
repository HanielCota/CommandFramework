package com.github.hanielcota.commandframework.command;

import com.github.hanielcota.commandframework.Command;
import com.github.hanielcota.commandframework.CommandArgs;
import org.bukkit.entity.Player;

public class HelloCommand {
    @Command(name = "hello", aliases = { "testing" }, description = "This is a test command", usage = "This is how you use it")
    public void testCommands(CommandArgs args) {
        Player player = args.getPlayer();
        if (args.getArgs().length == 0) {
            player.sendMessage("Olha vc usou Args 0");
            return;
        }

        if (args.getArgs().length == 1) {
            player.sendMessage("Olha vc usou Args 1");
            return;
        }

        if (args.getArgs().length == 2) {
            player.sendMessage("Olha vc usou Args 2");
            return;
        }

        if (args.getArgs().length == 3) {
            player.sendMessage("Olha vc usou Args 3");
            return;
        }

        player.sendMessage("Qual args? " + args.length());
    }

}
