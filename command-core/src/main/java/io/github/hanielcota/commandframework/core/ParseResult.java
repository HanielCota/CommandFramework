package io.github.hanielcota.commandframework.core;

import java.util.Objects;

public sealed interface ParseResult<T> permits ParseResult.Success, ParseResult.Failure {

    static <T> ParseResult<T> success(T value, int consumedTokens) {
        return new Success<>(value, consumedTokens);
    }

    static <T> ParseResult<T> failure(String invalidValue, String expectedValue) {
        return new Failure<>(invalidValue, expectedValue);
    }

    boolean isSuccess();

    default T value() {
        throw new IllegalStateException("Invalid parse access: expected successful result");
    }

    default int consumedTokens() {
        return 0;
    }

    default String invalidValue() {
        return "";
    }

    default String expectedValue() {
        return "";
    }

    record Success<T>(T value, int consumedTokens) implements ParseResult<T> {

        public Success {
            Objects.requireNonNull(value, "value");
            if (consumedTokens < 0) {
                throw new IllegalArgumentException("Invalid consumed tokens: expected zero or positive");
            }
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure<T>(String invalidValue, String expectedValue) implements ParseResult<T> {

        public Failure {
            Objects.requireNonNull(invalidValue, "invalidValue");
            Objects.requireNonNull(expectedValue, "expectedValue");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}
