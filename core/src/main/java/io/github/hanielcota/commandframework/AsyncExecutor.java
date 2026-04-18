package io.github.hanielcota.commandframework;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Schedules asynchronous command executions.
 */
@FunctionalInterface
public interface AsyncExecutor {

    void execute(String taskName, Runnable task);

    /**
     * Virtual-thread factory with no concurrency cap: every call starts a brand-new virtual
     * thread. Cheap individually, but in pathological cases (a command that spawns async
     * commands in a loop) the scheduler load and ThreadLocal accumulation can degrade the
     * server. Prefer {@link #virtualThreads(int)} when you can pick a sensible upper bound.
     */
    static AsyncExecutor virtualThreads() {
        return (taskName, task) -> {
            Objects.requireNonNull(taskName, "taskName");
            Objects.requireNonNull(task, "task");
            Thread.ofVirtual()
                    .name("commandframework-" + taskName)
                    .start(task);
        };
    }

    /**
     * Virtual-thread factory gated by a {@link Semaphore} of size {@code maxConcurrent}. Once the
     * cap is reached, further submissions fail fast with {@link RejectedExecutionException}
     * instead of piling up unbounded threads - the caller decides how to recover (retry, degrade,
     * drop). A bounded variant is strongly recommended for production plugins.
     *
     * @param maxConcurrent maximum number of simultaneously running tasks; must be positive
     * @throws IllegalArgumentException if {@code maxConcurrent <= 0}
     */
    static AsyncExecutor virtualThreads(int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be positive, got " + maxConcurrent);
        }
        Semaphore gate = new Semaphore(maxConcurrent);
        return (taskName, task) -> {
            Objects.requireNonNull(taskName, "taskName");
            Objects.requireNonNull(task, "task");
            if (!gate.tryAcquire()) {
                throw new RejectedExecutionException(
                        "AsyncExecutor reached concurrency cap (" + maxConcurrent + "); task "
                                + taskName + " rejected");
            }
            try {
                Thread.ofVirtual()
                        .name("commandframework-" + taskName)
                        .start(() -> {
                            try {
                                task.run();
                            } finally {
                                gate.release();
                            }
                        });
            } catch (RuntimeException | Error startFailure) {
                // Thread.start() failed before the task could claim responsibility for the
                // permit (e.g. OOM spinning up the carrier). Return it so the gate does not
                // silently leak capacity.
                gate.release();
                throw startFailure;
            }
        };
    }
}
