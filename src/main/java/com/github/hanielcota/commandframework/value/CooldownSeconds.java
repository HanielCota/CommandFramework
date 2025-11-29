package com.github.hanielcota.commandframework.value;

import java.time.Duration;

/**
 * Representa uma duração de cooldown em segundos.
 *
 * @param value Número de segundos (deve ser não negativo)
 */
public record CooldownSeconds(int value) {
    public CooldownSeconds {
        if (value < 0) {
            throw new IllegalArgumentException("CooldownSeconds não pode ser negativo");
        }
    }

    /**
     * Converte os segundos em uma Duration.
     *
     * @return Duration correspondente aos segundos
     */
    public Duration toDuration() {
        return Duration.ofSeconds(value);
    }
}

