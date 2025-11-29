package com.github.hanielcota.commandframework.value;

/**
 * Representa um pacote base para escaneamento de comandos.
 *
 * @param value Nome do pacote base (não pode ser nulo ou vazio)
 */
public record BasePackage(String value) {
    public BasePackage {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BasePackage não pode ser nulo ou vazio");
        }
    }

    public boolean isBlank() {
        return value.isBlank();
    }
}

