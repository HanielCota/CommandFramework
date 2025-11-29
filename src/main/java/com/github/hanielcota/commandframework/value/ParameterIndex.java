package com.github.hanielcota.commandframework.value;

public record ParameterIndex(int value) {
    public ParameterIndex {
        if (value < 0) {
            throw new IllegalArgumentException("ParameterIndex não pode ser negativo");
        }
    }

    public boolean isWithinBounds(int arrayLength) {
        return value < arrayLength && value >= 0;
    }
}

