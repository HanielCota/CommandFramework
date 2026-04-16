package com.example.paperdemo.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Command(name = "heal", aliases = {"curar"}, description = "Heal yourself or another player")
@Permission("papersample.heal")
public final class HealCommand {

    @Execute
    @Description("Heal yourself")
    @Cooldown(value = 30, unit = TimeUnit.SECONDS, bypassPermission = "papersample.heal.bypass")
    public void healSelf(@Sender Player sender) {
        sender.setHealth(sender.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        sender.setFoodLevel(20);
        sender.sendMessage(Component.text("You have been healed.", NamedTextColor.GREEN));
    }

    @Execute(sub = "other")
    @Description("Heal another player")
    @Permission("papersample.heal.other")
    public void healOther(@Sender Player sender, Player target) {
        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);
        target.sendMessage(Component.text("You have been healed by " + sender.getName(), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Healed " + target.getName(), NamedTextColor.GRAY));
    }
}
