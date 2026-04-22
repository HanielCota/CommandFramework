package io.github.hanielcota.commandframework.core.rate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hanielcota.commandframework.core.MutableClock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class DispatchThrottleTest {

    @Test
    void resetsAfterWindowExpires() {
        MutableClock clock = new MutableClock();
        DispatchThrottle throttle = new DispatchThrottle(2, Duration.ofSeconds(1), clock);

        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("actor"));
        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("actor"));
        assertEquals(ThrottleDecision.DENIED, throttle.claim("actor"));

        clock.advance(Duration.ofSeconds(1));

        assertEquals(ThrottleDecision.DENIED, throttle.claim("actor"));

        clock.advance(Duration.ofMillis(1));

        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("actor"));
    }

    @Test
    void doesNotRefillGraduallyInsideWindow() {
        MutableClock clock = new MutableClock();
        DispatchThrottle throttle = new DispatchThrottle(2, Duration.ofSeconds(1), clock);

        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("actor"));
        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("actor"));

        clock.advance(Duration.ofMillis(999));

        assertEquals(ThrottleDecision.DENIED, throttle.claim("actor"));
    }

    @Test
    void tracksActorsIndependently() {
        DispatchThrottle throttle = new DispatchThrottle(1, Duration.ofSeconds(1));

        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("first"));
        assertEquals(ThrottleDecision.DENIED, throttle.claim("first"));
        assertEquals(ThrottleDecision.ALLOWED, throttle.claim("second"));
    }

    @Test
    void allowsOnlyConfiguredConcurrentClaims() throws Exception {
        DispatchThrottle throttle = new DispatchThrottle(3, Duration.ofSeconds(1));
        int workers = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    if (throttle.claim("actor") == ThrottleDecision.ALLOWED) {
                        allowed.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(3, allowed.get());
    }
}
