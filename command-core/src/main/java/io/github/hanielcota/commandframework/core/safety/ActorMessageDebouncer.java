package io.github.hanielcota.commandframework.core.safety;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ActorMessageDebouncer {

    private final Cache<DebounceKey, Long> recentMessages;
    private final Clock clock;
    private final Duration window;

    public ActorMessageDebouncer(Duration window) {
        this(window, Clock.systemUTC());
    }

    public ActorMessageDebouncer(Duration window, Clock clock) {
        Duration checkedWindow = Objects.requireNonNull(window, "window");
        if (checkedWindow.isNegative()) {
            throw new IllegalArgumentException("Invalid debounce window: expected zero or positive duration");
        }
        this.window = checkedWindow;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.recentMessages = Caffeine.newBuilder()
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(this.clock.millis()))
                .expireAfterWrite(expireAfterWindow(checkedWindow))
                .build();
    }

    public boolean shouldSend(String actorId, String message) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(message, "message");
        DebounceKey key = new DebounceKey(actorId, message);

        long now = clock.millis();
        java.util.concurrent.atomic.AtomicBoolean allowed = new java.util.concurrent.atomic.AtomicBoolean(false);
        recentMessages.asMap().compute(key, (ignored, previous) -> {
            if (previous != null && now - previous <= window.toMillis()) {
                return previous;
            }
            allowed.set(true);
            return now;
        });
        return allowed.get();
    }

    private static Duration expireAfterWindow(Duration window) {
        if (window.isZero()) {
            return Duration.ofNanos(1);
        }
        return window.plusNanos(1);
    }

    private record DebounceKey(String actorId, String message) {

        private DebounceKey {
            Objects.requireNonNull(actorId, "actorId");
            Objects.requireNonNull(message, "message");
        }
    }
}
