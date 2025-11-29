package com.github.hanielcota.commandframework.cooldown;

import java.util.UUID;

public record CooldownKey(UUID senderId, String commandName, String subCommandName) {
}


