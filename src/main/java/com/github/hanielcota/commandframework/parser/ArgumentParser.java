package com.github.hanielcota.commandframework.parser;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public interface ArgumentParser<T> {

    String name();

    Class<T> type();

    ArgumentType<?> brigadierType();

    Optional<T> parse(CommandContext<CommandSender> context, String name);
}


