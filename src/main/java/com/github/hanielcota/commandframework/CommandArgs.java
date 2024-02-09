package com.github.hanielcota.commandframework;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
public class CommandArgs {

    private final CommandSender sender;
    private final org.bukkit.command.Command command;
    private final String label;
    private final String[] args;

    protected CommandArgs(CommandSender sender, org.bukkit.command.Command command, String label, String[] args, int subCommand) {
        String[] modArgs = new String[args.length - subCommand];
        if (args.length - subCommand >= 0) {
            System.arraycopy(args, subCommand, modArgs, 0, args.length - subCommand);
        }

        StringBuilder builder = new StringBuilder(label);
        for (int x = 0; x < subCommand; x++) {
            builder.append(".").append(args[x]);
        }
        String cmdLabel = builder.toString();

        this.sender = sender;
        this.command = command;
        this.label = cmdLabel;
        this.args = modArgs;
    }

    public String getArgs(int index) {
        return args[index];
    }

    public int length() {
        return args.length;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player getPlayer() {
        if (!(sender instanceof Player)) {
            return null;
        }

        return (Player) sender;
    }
}
