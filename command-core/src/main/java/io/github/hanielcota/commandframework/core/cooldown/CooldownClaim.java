package io.github.hanielcota.commandframework.core.cooldown;

import java.time.Duration;
import java.util.Objects;

public record CooldownClaim(boolean accepted, Duration remaining) {

    public CooldownClaim {
        Objects.requireNonNull(remaining, "remaining");
        if (remaining.isNegative()) {
            throw new IllegalArgumentException("Invalid remaining duration: expected zero or positive");
        }
    }

    public static CooldownClaim allowed() {
        return new CooldownClaim(true, Duration.ZERO);
    }

    public static CooldownClaim denied(Duration remaining) {
        return new CooldownClaim(false, remaining);
    }

    public boolean isAllowed() {
        return accepted;
    }
}
