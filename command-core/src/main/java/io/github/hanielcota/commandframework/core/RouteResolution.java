package io.github.hanielcota.commandframework.core;

import java.util.Objects;
import java.util.Optional;

public sealed interface RouteResolution permits RouteResolution.Found, RouteResolution.NotFound {

    static RouteResolution found(RouteMatch match) {
        return new Found(match);
    }

    static RouteResolution notFound(String invalidValue, String expectedValue) {
        return new NotFound(invalidValue, expectedValue);
    }

    boolean isFound();

    default Optional<RouteMatch> match() {
        return Optional.empty();
    }

    default String invalidValue() {
        return "";
    }

    default String expectedValue() {
        return "";
    }

    record Found(RouteMatch matchValue) implements RouteResolution {

        public Found {
            Objects.requireNonNull(matchValue, "matchValue");
        }

        @Override
        public boolean isFound() {
            return true;
        }

        @Override
        public Optional<RouteMatch> match() {
            return Optional.of(matchValue);
        }
    }

    record NotFound(String invalidValue, String expectedValue) implements RouteResolution {

        public NotFound {
            Objects.requireNonNull(invalidValue, "invalidValue");
            Objects.requireNonNull(expectedValue, "expectedValue");
        }

        @Override
        public boolean isFound() {
            return false;
        }
    }
}
