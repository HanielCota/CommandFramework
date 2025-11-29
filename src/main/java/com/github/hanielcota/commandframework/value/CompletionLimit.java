package com.github.hanielcota.commandframework.value;

public record CompletionLimit(int value) {
    public static final CompletionLimit DEFAULT = new CompletionLimit(50);

    public CompletionLimit {
        if (value < 0) {
            throw new IllegalArgumentException("CompletionLimit não pode ser negativo");
        }
    }

    public boolean isReached(int currentCount) {
        return currentCount >= value;
    }
}

