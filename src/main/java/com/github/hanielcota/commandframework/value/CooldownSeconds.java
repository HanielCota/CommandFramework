package com.github.hanielcota.commandframework.value;

import java.time.Duration;

public record CooldownSeconds(int value) {
    public CooldownSeconds {
        if (value < 0) {
            throw new IllegalArgumentException("CooldownSeconds não pode ser negativo");
        }
    }

    public Duration toDuration() {
        return Duration.ofSeconds(value);
    }
}

