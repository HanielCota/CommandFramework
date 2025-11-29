package com.github.hanielcota.commandframework.value;

public record RemainingArguments(String[] value) {
    public RemainingArguments {
        if (value == null) {
            throw new IllegalArgumentException("RemainingArguments não pode ser nulo");
        }
    }

    public int length() {
        return value.length;
    }

    public boolean isEmpty() {
        return value.length == 0;
    }

    public String get(int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return value[index];
    }
}

