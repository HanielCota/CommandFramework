package io.github.hanielcota.commandframework.core.rate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class DispatchThrottle implements AutoCloseable {

    private final Cache<String, Bucket> buckets;
    private final Clock clock;
    private final TimeMeter timeMeter;
    private final int maxRequests;
    private final Duration refillPeriod;

    public DispatchThrottle(int maxRequests, Duration window) {
        this(maxRequests, window, Clock.systemUTC());
    }

    public DispatchThrottle(int maxRequests, Duration window, Clock clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("Invalid max requests: expected at least one");
        }
        Duration checkedWindow = Objects.requireNonNull(window, "window");
        if (checkedWindow.isZero() || checkedWindow.isNegative()) {
            throw new IllegalArgumentException("Invalid throttle window: expected positive duration");
        }
        this.maxRequests = maxRequests;
        this.refillPeriod = expireAfterWindow(checkedWindow);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeMeter = new ClockTimeMeter(this.clock);
        this.buckets = Caffeine.newBuilder()
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(this.clock.millis()))
                .expireAfterAccess(refillPeriod)
                .build();
    }

    public ThrottleDecision claim(String actorId) {
        String checkedActorId = Objects.requireNonNull(actorId, "actorId");
        Bucket bucket = buckets.asMap().computeIfAbsent(checkedActorId, ignored -> newBucket());
        if (bucket.tryConsume(1)) {
            return ThrottleDecision.ALLOWED;
        }
        return ThrottleDecision.DENIED;
    }

    @Override
    public void close() {
        buckets.invalidateAll();
        buckets.cleanUp();
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limit -> limit.capacity(maxRequests).refillIntervally(maxRequests, refillPeriod))
                .build();
    }

    private static Duration expireAfterWindow(Duration window) {
        return window.plusNanos(1);
    }

    private record ClockTimeMeter(Clock clock) implements TimeMeter {

        private ClockTimeMeter {
            Objects.requireNonNull(clock, "clock");
        }

        @Override
        public long currentTimeNanos() {
            return TimeUnit.MILLISECONDS.toNanos(clock.millis());
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }
    }
}
