package com.github.hanielcota.commandframework.value;

public record ArgumentIndex(int value) {
    public ArgumentIndex {
        if (value < 0) {
            throw new IllegalArgumentException("ArgumentIndex não pode ser negativo");
        }
    }

    public boolean isWithinBounds(int arrayLength) {
        return value < arrayLength;
    }

    public ArgumentIndex increment() {
        return new ArgumentIndex(value + 1);
    }
}

