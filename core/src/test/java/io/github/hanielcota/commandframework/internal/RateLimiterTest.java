package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.FrameworkLogger;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimiterTest {

    @Test
    void nonPlayerActorIsNeverSilenced() {
        FrameworkLogger logger = mock(FrameworkLogger.class);
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(5), logger);
        CommandActor console = mock(CommandActor.class);
        when(console.isPlayer()).thenReturn(false);

        for (int index = 0; index < 10; index++) {
            assertFalse(limiter.shouldSilence(console));
        }
    }

    @Test
    void playerBelowLimitIsNotSilenced() {
        FrameworkLogger logger = mock(FrameworkLogger.class);
        int limit = 5;
        RateLimiter limiter = new RateLimiter(limit, Duration.ofSeconds(5), logger);
        CommandActor player = playerActor();

        for (int index = 0; index < limit; index++) {
            assertFalse(limiter.shouldSilence(player));
        }
    }

    @Test
    void playerAtLimitIsSilenced() {
        FrameworkLogger logger = mock(FrameworkLogger.class);
        int limit = 3;
        RateLimiter limiter = new RateLimiter(limit, Duration.ofSeconds(5), logger);
        CommandActor player = playerActor();

        for (int index = 0; index < limit; index++) {
            assertFalse(limiter.shouldSilence(player));
        }

        assertTrue(limiter.shouldSilence(player));
    }

    @Test
    void newWindowReleasesLimit() throws InterruptedException {
        // TODO(F-02): inject Clock to replace this sleep-based timing assertion.
        FrameworkLogger logger = mock(FrameworkLogger.class);
        int limit = 2;
        RateLimiter limiter = new RateLimiter(limit, Duration.ofMillis(50), logger);
        CommandActor player = playerActor();

        for (int index = 0; index < limit; index++) {
            assertFalse(limiter.shouldSilence(player));
        }
        assertTrue(limiter.shouldSilence(player));

        Thread.sleep(60L);
        assertFalse(limiter.shouldSilence(player));
    }

    @Test
    void concurrentInvocationsNeverExceedLimit() throws InterruptedException {
        FrameworkLogger logger = mock(FrameworkLogger.class);
        int limit = 5;
        int total = 50;
        RateLimiter limiter = new RateLimiter(limit, Duration.ofSeconds(10), logger);
        CommandActor player = playerActor();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger silenced = new AtomicInteger();

        for (int index = 0; index < total; index++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    if (limiter.shouldSilence(player)) {
                        silenced.incrementAndGet();
                    } else {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "virtual threads did not complete in time");

        assertEquals(limit, allowed.get());
        assertEquals(total - limit, silenced.get());
    }

    private static CommandActor playerActor() {
        CommandActor actor = mock(CommandActor.class);
        when(actor.isPlayer()).thenReturn(true);
        when(actor.uniqueId()).thenReturn(UUID.randomUUID());
        when(actor.name()).thenReturn("player");
        return actor;
    }
}
