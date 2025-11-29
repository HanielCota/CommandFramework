package com.github.hanielcota.commandframework.value;

public record SubCommandPath(String value) {
    public SubCommandPath {
        if (value == null) {
            throw new IllegalArgumentException("SubCommandPath não pode ser nulo");
        }
    }

    public String toLowerCase() {
        return value.toLowerCase();
    }

    public String[] splitParts() {
        return value.split(" ");
    }

    public boolean startsWith(String prefix) {
        return value.toLowerCase().startsWith(prefix.toLowerCase());
    }
}

