package io.github.hanielcota.commandframework.core.dispatch;

import io.github.hanielcota.commandframework.core.ParsedParameter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface ParameterParseOutcome permits ParameterParseOutcome.Success, ParameterParseOutcome.Failure {

    static ParameterParseOutcome success(List<ParsedParameter<?>> parameters) {
        return new Success(parameters);
    }

    static ParameterParseOutcome failure(String invalidValue, String expectedValue) {
        return new Failure(new ParameterParseFailure(invalidValue, expectedValue));
    }

    boolean isSuccess();

    default List<ParsedParameter<?>> parameters() {
        return List.of();
    }

    default Optional<ParameterParseFailure> failure() {
        return Optional.empty();
    }

    record Success(List<ParsedParameter<?>> parameters) implements ParameterParseOutcome {

        public Success(List<ParsedParameter<?>> parameters) {
            Objects.requireNonNull(parameters, "parameters");
            this.parameters = List.copyOf(parameters);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure(ParameterParseFailure failureValue) implements ParameterParseOutcome {

        public Failure {
            Objects.requireNonNull(failureValue, "failureValue");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<ParameterParseFailure> failure() {
            return Optional.of(failureValue);
        }
    }
}
