package com.github.hanielcota.commandframework.value;

public record CommandName(String value) {
    public CommandName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CommandName não pode ser nulo ou vazio");
        }
    }

    public String toLowerCase() {
        return value.toLowerCase();
    }
}

