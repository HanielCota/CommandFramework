package io.github.hanielcota.commandframework.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.hanielcota.commandframework.CommandActor;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks pending confirmation prompts keyed by sender UUID and expires them automatically when the
 * configured TTL elapses.
 *
 * <p>Each entry records the command the sender must re-run to confirm, together with an absolute
 * nanosecond deadline. A Caffeine {@link Cache} performs expiration using a per-entry
 * {@link Expiry} that tracks remaining time rather than a fixed global TTL.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use; all state is held in the Caffeine cache, which
 * provides atomic put/get/invalidate semantics.
 */
public final class ConfirmationManager {

    private final Cache<UUID, Entry> pending = Caffeine.newBuilder()
            .expireAfter(new Expiry<UUID, Entry>() {
                @Override
                public long expireAfterCreate(UUID key, Entry value, long currentTime) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }

                @Override
                public long expireAfterUpdate(UUID key, Entry value, long currentTime, long currentDuration) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }

                @Override
                public long expireAfterRead(UUID key, Entry value, long currentTime, long currentDuration) {
                    return remainingNanos(value.expiresAtNanos(), currentTime);
                }
            })
            .build();

    private static long remainingNanos(long expiresAtNanos, long currentTime) {
        return Math.max(0L, expiresAtNanos - currentTime);
    }

    void put(CommandActor actor, PreparedInvocation invocation, ConfirmDefinition confirm) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(confirm, "confirm");
        UUID id = Objects.requireNonNull(actor.uniqueId(), "CommandActor.uniqueId() must not return null");
        this.pending.put(id, new Entry(invocation, this.expiresAt(System.nanoTime(), confirm.expiresIn())));
    }

    PreparedInvocation consume(CommandActor actor, String commandName) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(commandName, "commandName");
        UUID id = Objects.requireNonNull(actor.uniqueId(), "CommandActor.uniqueId() must not return null");
        Holder holder = new Holder();
        long now = System.nanoTime();
        this.pending.asMap().compute(id, (ignored, entry) -> {
            if (entry == null) {
                return null;
            }
            if (entry.expiresAtNanos() <= now) {
                return null;
            }
            ConfirmDefinition confirm = entry.invocation().executor().confirm();
            if (confirm == null || !confirm.commandName().equalsIgnoreCase(commandName)) {
                return entry;
            }
            holder.invocation = entry.invocation();
            return null;
        });
        return holder.invocation;
    }

    private long expiresAt(long now, Duration duration) {
        long nanos = duration.toNanos();
        long maxSafeStart = Long.MAX_VALUE - nanos;
        return now > maxSafeStart ? Long.MAX_VALUE : now + nanos;
    }

    private record Entry(PreparedInvocation invocation, long expiresAtNanos) {
    }

    private static final class Holder {
        private PreparedInvocation invocation;
    }
}
