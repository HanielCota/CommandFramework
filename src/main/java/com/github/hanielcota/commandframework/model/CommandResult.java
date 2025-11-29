package com.github.hanielcota.commandframework.model;

import net.kyori.adventure.text.Component;

public sealed interface CommandResult permits CommandResult.Success, CommandResult.Failure, CommandResult.NoOp {

    record Success(Component message) implements CommandResult {
    }

    record Failure(Component message) implements CommandResult {
    }

    record NoOp() implements CommandResult {
    }
}


