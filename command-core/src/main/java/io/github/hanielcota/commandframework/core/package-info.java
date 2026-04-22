/**
 * Platform-agnostic command core.
 *
 * <p>Dispatch is synchronous in the MVP. Shared route, cooldown, debounce and
 * throttle state use thread-safe structures so platform adapters may call the
 * dispatcher from their normal command thread without extra synchronization.</p>
 */
@NullMarked
package io.github.hanielcota.commandframework.core;

import org.jspecify.annotations.NullMarked;
