package io.github.hanielcota.commandframework.core.cooldown;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandRoute;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class RouteCooldownStore implements AutoCloseable {

    private final Cache<CooldownKey, Long> expiresAtMillis;
    private final Clock clock;

    public RouteCooldownStore() {
        this(Clock.systemUTC());
    }

    public RouteCooldownStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.expiresAtMillis = Caffeine.newBuilder()
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(this.clock.millis()))
                .expireAfter(new ExpirationTimeExpiry())
                .build();
    }

    public CooldownClaim claim(CommandActor actor, CommandRoute route) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(route, "route");
        if (!route.hasCooldown()) {
            return CooldownClaim.allowed();
        }
        CooldownKey key = new CooldownKey(actor.uniqueId(), route.canonicalPath());
        return claimKey(key, route.cooldown());
    }

    private CooldownClaim claimKey(CooldownKey key, Duration cooldown) {
        long now = clock.millis();
        AtomicReference<CooldownClaim> claim = new AtomicReference<>();
        expiresAtMillis.asMap().compute(key, (CooldownKey ignored, Long expiresAt) -> {
            if (expiresAt != null && expiresAt > now) {
                claim.set(CooldownClaim.denied(Duration.ofMillis(expiresAt - now)));
                return expiresAt;
            }
            claim.set(CooldownClaim.allowed());
            return now + cooldown.toMillis();
        });
        return claim.get();
    }

    @Override
    public void close() {
        expiresAtMillis.invalidateAll();
        expiresAtMillis.cleanUp();
    }

    private static final class ExpirationTimeExpiry implements Expiry<CooldownKey, Long> {

        @Override
        public long expireAfterCreate(CooldownKey key, Long expiresAtMillis, long currentTime) {
            return nanosUntil(expiresAtMillis, currentTime);
        }

        @Override
        public long expireAfterUpdate(CooldownKey key, Long expiresAtMillis, long currentTime, long currentDuration) {
            return nanosUntil(expiresAtMillis, currentTime);
        }

        @Override
        public long expireAfterRead(CooldownKey key, Long expiresAtMillis, long currentTime, long currentDuration) {
            return currentDuration;
        }

        private long nanosUntil(long expiresAtMillis, long currentTime) {
            long expiresAtNanos = TimeUnit.MILLISECONDS.toNanos(expiresAtMillis);
            return Math.max(0, expiresAtNanos - currentTime);
        }
    }
}
