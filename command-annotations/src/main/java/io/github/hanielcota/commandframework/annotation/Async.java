package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command route for asynchronous execution.
 *
 * <p>Requires that the {@link io.github.hanielcota.commandframework.core.CommandDispatcher}
 * is configured with an {@link java.util.concurrent.Executor}.</p>
 *
 * <p><strong>Thread-safety warning for Paper/Bukkit:</strong> When a route is marked
 * as async, the entire dispatch pipeline (guards, parsing, executor) runs on the
 * provided executor — <em>not</em> on the server main thread. On Paper/Bukkit, most
 * API calls ({@code Player}, {@code World}, {@code Entity}, {@code Inventory},
 * etc.) must run on the main thread. Command executors that use these APIs must
 * schedule work back to the main thread explicitly. The Paper adapter's
 * {@code sendMessage} handles thread-safety automatically when a Plugin reference
 * is provided. On Velocity, the proxy API is generally safe to call from any
 * thread.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
}
