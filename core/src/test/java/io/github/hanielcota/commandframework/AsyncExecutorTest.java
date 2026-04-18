package io.github.hanielcota.commandframework;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncExecutorTest {

    @Test
    @DisplayName("unbounded virtualThreads runs submitted tasks")
    void unboundedRunsTasks() throws InterruptedException {
        AsyncExecutor executor = AsyncExecutor.virtualThreads();
        CountDownLatch done = new CountDownLatch(1);

        executor.execute("smoke", done::countDown);

        assertTrue(done.await(5, TimeUnit.SECONDS), "task did not run within 5s");
    }

    @Test
    @DisplayName("virtualThreads(n) rejects construction with non-positive cap")
    void boundedRejectsNonPositiveCap() {
        assertThrows(IllegalArgumentException.class, () -> AsyncExecutor.virtualThreads(0));
        assertThrows(IllegalArgumentException.class, () -> AsyncExecutor.virtualThreads(-1));
    }

    @Test
    @DisplayName("bounded virtualThreads rejects submissions past the cap and recovers once tasks finish")
    void boundedEnforcesConcurrencyCap() throws InterruptedException {
        AsyncExecutor executor = AsyncExecutor.virtualThreads(2);
        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(2);
        AtomicInteger ran = new AtomicInteger();

        Runnable gated = () -> {
            ran.incrementAndGet();
            started.countDown();
            try {
                block.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };

        executor.execute("slot-1", gated);
        executor.execute("slot-2", gated);
        assertTrue(started.await(5, TimeUnit.SECONDS), "first two tasks did not start in time");

        assertThrows(RejectedExecutionException.class,
                () -> executor.execute("overflow", () -> { }),
                "third submission should have been rejected");

        block.countDown();

        // Poll until both gated tasks have released their permit; fall back with a bounded wait
        // so a flaky machine still fails fast rather than hanging.
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        CountDownLatch recovered = new CountDownLatch(1);
        while (System.nanoTime() < deadlineNanos) {
            try {
                executor.execute("post-release", recovered::countDown);
                break;
            } catch (RejectedExecutionException stillFull) {
                Thread.sleep(10);
            }
        }
        assertTrue(recovered.await(5, TimeUnit.SECONDS), "executor did not recover capacity");
        assertEquals(2, ran.get(), "only the two gated tasks should have counted as started");
    }
}
