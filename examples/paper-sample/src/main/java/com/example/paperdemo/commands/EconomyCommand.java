package com.example.paperdemo.commands;

import com.example.paperdemo.EconomyService;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Inject;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Command(name = "eco", aliases = {"economy"}, description = "Economy commands")
public final class EconomyCommand {

    @Inject private EconomyService economy;

    @Execute
    @Description("Check your balance")
    public void balance(@Sender Player sender) {
        double amount = this.economy.getBalance(sender.getUniqueId());
        sender.sendMessage(Component.text("Balance: $" + amount, NamedTextColor.GOLD));
    }

    @Execute(sub = "pay")
    @Description("Send coins to another player")
    @Cooldown(value = 5, unit = TimeUnit.SECONDS)
    public void pay(@Sender Player sender, Player target, double amount) {
        if (!this.economy.transfer(sender.getUniqueId(), target.getUniqueId(), amount)) {
            sender.sendMessage(Component.text("Transfer failed: insufficient funds or invalid amount.",
                    NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Sent $" + amount + " to " + target.getName(),
                NamedTextColor.GREEN));
        target.sendMessage(Component.text("Received $" + amount + " from " + sender.getName(),
                NamedTextColor.GREEN));
    }

    @Execute(sub = "reset")
    @Description("Reset a player's balance (admin)")
    @Permission("papersample.eco.admin")
    @Confirm(expireSeconds = 10, commandName = "eco-confirm")
    public void reset(@Sender Player sender, Player target) {
        // The executor only runs after the sender confirms via /eco-confirm within 10s.
        sender.sendMessage(Component.text("Resetting " + target.getName() + " (stub)",
                NamedTextColor.DARK_RED));
    }
}
