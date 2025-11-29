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
    Duration maxCooldownDuration;

    /**
     * Cria um CooldownService com duração máxima padrão de 1 hora.
     * Os cooldowns individuais podem ser menores, mas não maiores que este valor.
     */
    public static CooldownService create() {
        return create(Duration.ofHours(1));
    }

    /**
     * Cria um CooldownService com duração máxima especificada.
     * O cache será configurado para expirar entradas após este tempo.
     * 
     * @param maxDuration Duração máxima permitida para cooldowns
     */
    public static CooldownService create(Duration maxDuration) {
        if (maxDuration == null) {
            throw new IllegalArgumentException("maxDuration não pode ser nulo");
        }

        // Adiciona margem de segurança para evitar expiração prematura
        var cacheExpiration = maxDuration.plusMinutes(1);
        
        Cache<CooldownKey, Long> cache = Caffeine.newBuilder()
            .expireAfterWrite(cacheExpiration)
            .maximumSize(10_000)
            .build();

        return new CooldownService(cache, maxDuration);
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

    public Optional<Duration> getRemainingTime(CooldownKey key) {
        if (key == null) {
            return Optional.empty();
        }

        var now = System.nanoTime();
        var expiresAt = Optional.ofNullable(cache.getIfPresent(key));
        if (expiresAt.isEmpty()) {
            return Optional.empty();
        }

        var remainingNanos = expiresAt.get() - now;
        if (remainingNanos <= 0L) {
            cache.invalidate(key);
            return Optional.empty();
        }

        return Optional.of(Duration.ofNanos(remainingNanos));
    }

    public void putOnCooldown(CooldownKey key, Duration duration) {
        if (key == null) {
            return;
        }

        if (duration == null) {
            return;
        }

        // Limita a duração ao máximo permitido
        var effectiveDuration = duration.compareTo(maxCooldownDuration) > 0 
            ? maxCooldownDuration 
            : duration;

        var now = System.nanoTime();
        var nanos = effectiveDuration.toNanos();
        var expiresAt = now + nanos;
        cache.put(key, expiresAt);
    }

    public void removeCooldown(CooldownKey key) {
        if (key == null) {
            return;
        }
        cache.invalidate(key);
    }

    public void clearAll() {
        cache.invalidateAll();
    }
}
