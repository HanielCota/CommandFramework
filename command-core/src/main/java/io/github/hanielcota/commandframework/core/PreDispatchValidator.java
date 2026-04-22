package io.github.hanielcota.commandframework.core;

import io.github.hanielcota.commandframework.core.rate.DispatchThrottle;
import io.github.hanielcota.commandframework.core.rate.ThrottleDecision;
import io.github.hanielcota.commandframework.core.safety.InputSanitizer;
import io.github.hanielcota.commandframework.core.safety.SanitizedInput;
import java.util.List;
import java.util.Objects;

public final class PreDispatchValidator {

    private final DispatchThrottle throttle;
    private final InputSanitizer sanitizer;

    public PreDispatchValidator(DispatchThrottle throttle, InputSanitizer sanitizer) {
        this.throttle = Objects.requireNonNull(throttle, "throttle");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    public ValidationResult validate(CommandActor actor, List<String> arguments) {
        Objects.requireNonNull(actor, "actor");
        SanitizedInput input = sanitizer.sanitize(arguments);
        if (!input.isValid()) {
            return ValidationResult.invalid(input.invalidValue(), input.expectedValue());
        }
        if (throttle.claim(actor.uniqueId()) == ThrottleDecision.DENIED) {
            return ValidationResult.throttled();
        }
        return ValidationResult.valid(input.arguments());
    }

    public SanitizedInput sanitize(List<String> arguments) {
        return sanitizer.sanitize(Objects.requireNonNull(arguments, "arguments"));
    }
}
