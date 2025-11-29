package com.github.hanielcota.commandframework.cooldown;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CooldownService {

    Cache<CooldownKey, Long> cache;

    public static CooldownService create(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("duration não pode ser nulo");
        }

        Cache<CooldownKey, Long> cache = Caffeine.newBuilder()
            .expireAfterWrite(duration)
            .build();

        return new CooldownService(cache);
    }

    public boolean isOnCooldown(CooldownKey key) {
        if (key == null) {
            return false;
        }

        var now = System.nanoTime();
        var expiresAt = Optional.ofNullable(cache.getIfPresent(key));
        if (expiresAt.isEmpty()) {
            return false;
        }

        var remaining = expiresAt.get() - now;
        if (remaining <= 0L) {
            cache.invalidate(key);
            return false;
        }

        return true;
    }

    public void putOnCooldown(CooldownKey key, Duration duration) {
        if (key == null) {
            return;
        }

        if (duration == null) {
            return;
        }

        var now = System.nanoTime();
        var nanos = duration.toNanos();
        var expiresAt = now + nanos;
        cache.put(key, expiresAt);
    }
}


