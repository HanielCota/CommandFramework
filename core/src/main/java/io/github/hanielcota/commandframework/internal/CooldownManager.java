package io.github.hanielcota.commandframework.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.hanielcota.commandframework.CommandActor;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Enforces per-sender-per-command cooldowns.
 *
 * <p>Entries are keyed by the composite {@code sender-UUID + command-name} and expire automatically
 * via a Caffeine {@link Cache} with a per-entry {@link Expiry} tracking the remaining cooldown
 * window, so keys are reclaimed as soon as the cooldown ends.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use; all state is held in the Caffeine cache.
 */
public final class CooldownManager {

    private final Cache<String, CooldownEntry> entries = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, CooldownEntry>() {
                @Override
                public long expireAfterCreate(String key, CooldownEntry value, long currentTime) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }

                @Override
                public long expireAfterUpdate(String key, CooldownEntry value, long currentTime, long currentDuration) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }

                @Override
                public long expireAfterRead(String key, CooldownEntry value, long currentTime, long currentDuration) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }
            })
            .build();

    private static long remainingNanos(long expiresAtNanos, long currentTime) {
        return Math.max(0L, expiresAtNanos - currentTime);
    }

    CooldownResult checkAndConsume(String commandPath, CommandActor actor, CooldownDefinition cooldown) {
        Objects.requireNonNull(commandPath, "commandPath");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(cooldown, "cooldown");
        if (!cooldown.bypassPermission().isBlank() && actor.hasPermission(cooldown.bypassPermission())) {
            return new CooldownResult(true, Duration.ZERO);
        }

        String key = this.key(actor.uniqueId(), commandPath);
        long now = System.nanoTime();
        Holder holder = new Holder();
        this.entries.asMap().compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtNanos() <= now) {
                holder.result = new CooldownResult(true, Duration.ZERO);
                return new CooldownEntry(this.expiresAt(now, cooldown.duration()));
            }

            holder.result = new CooldownResult(false, Duration.ofNanos(existing.expiresAtNanos() - now));
            return existing;
        });
        return holder.result;
    }

    private String key(UUID actorId, String commandPath) {
        Objects.requireNonNull(actorId, "CommandActor.uniqueId() must not return null");
        return actorId + "|" + commandPath;
    }

    private long expiresAt(long now, Duration duration) {
        long nanos = duration.toNanos();
        long maxSafeStart = Long.MAX_VALUE - nanos;
        return now > maxSafeStart ? Long.MAX_VALUE : now + nanos;
    }

    public record CooldownResult(boolean allowed, Duration remaining) {
        public CooldownResult {
            Objects.requireNonNull(remaining, "remaining");
        }
    }

    private record CooldownEntry(long expiresAtNanos) {
    }

    private static final class Holder {
        private CooldownResult result;
    }
}
