package io.github.hanielcota.commandframework.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.FrameworkLogger;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Fixed-window rate limiter keyed by sender UUID that allows up to {@code limit} invocations per
 * {@code window}; overflow calls are rejected and logged as suspicious activity.
 *
 * <p>Windows are tracked per sender and reset lazily when the previous window's deadline has
 * passed. A Caffeine {@link Cache} with a per-entry {@link Expiry} reclaims state as soon as the
 * window closes, so idle senders consume no memory.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use; per-key counter updates go through
 * {@link Cache#asMap()}'s {@code compute} to ensure atomic increments.
 */
public final class RateLimiter {

    private final int limit;
    private final Duration window;
    private final FrameworkLogger logger;
    private final Cache<UUID, State> states = Caffeine.newBuilder()
            .expireAfter(new Expiry<UUID, State>() {
                @Override
                public long expireAfterCreate(UUID key, State value, long currentTime) {
                    return RateLimiter.this.remainingNanos(value.windowStartedNanos(), currentTime);
                }

                @Override
                public long expireAfterUpdate(UUID key, State value, long currentTime, long currentDuration) {
                    return RateLimiter.this.remainingNanos(value.windowStartedNanos(), currentTime);
                }

                @Override
                public long expireAfterRead(UUID key, State value, long currentTime, long currentDuration) {
                    return RateLimiter.this.remainingNanos(value.windowStartedNanos(), currentTime);
                }
            })
            .build();

    public RateLimiter(int limit, Duration window, FrameworkLogger logger) {
        this.limit = limit;
        this.window = Objects.requireNonNull(window, "window");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public boolean shouldSilence(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        if (!actor.isPlayer()) {
            return false;
        }

        UUID key = Objects.requireNonNull(actor.uniqueId(), "CommandActor.uniqueId() must not return null");
        long now = System.nanoTime();
        Holder holder = new Holder();
        this.states.asMap().compute(key, (ignored, existing) -> {
            if (existing == null || this.windowExpired(existing.windowStartedNanos(), now)) {
                return new State(now, 1);
            }
            if (existing.count() >= this.limit) {
                holder.blocked = true;
                return existing;
            }
            return new State(existing.windowStartedNanos(), existing.count() + 1);
        });
        if (!holder.blocked) {
            return false;
        }

        this.logger.debug("Silently rate-limited command execution for " + actor.name());
        return true;
    }

    private boolean windowExpired(long windowStartedNanos, long currentTime) {
        return currentTime >= this.windowEnd(windowStartedNanos);
    }

    private long remainingNanos(long windowStartedNanos, long currentTime) {
        return Math.max(0L, this.windowEnd(windowStartedNanos) - currentTime);
    }

    private long windowEnd(long windowStartedNanos) {
        long nanos = this.window.toNanos();
        long maxSafeStart = Long.MAX_VALUE - nanos;
        return windowStartedNanos > maxSafeStart ? Long.MAX_VALUE : windowStartedNanos + nanos;
    }

    private record State(long windowStartedNanos, int count) {
    }

    private static final class Holder {
        private boolean blocked;
    }
}
