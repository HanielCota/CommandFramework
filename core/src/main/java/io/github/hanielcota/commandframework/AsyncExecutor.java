package io.github.hanielcota.commandframework;

import java.util.Objects;

/**
 * Schedules asynchronous command executions.
 */
@FunctionalInterface
public interface AsyncExecutor {

    void execute(String taskName, Runnable task);

    static AsyncExecutor virtualThreads() {
        return (taskName, task) -> {
            Objects.requireNonNull(taskName, "taskName");
            Objects.requireNonNull(task, "task");
            Thread.ofVirtual()
                    .name("commandframework-" + taskName)
                    .start(task);
        };
    }
}
