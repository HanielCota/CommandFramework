package io.github.hanielcota.commandframework.core.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.core.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class ActorMessageDebouncerTest {

    @Test
    void allowsFirstMessage() {
        ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(100));
        assertTrue(debouncer.shouldSend("actor1", "hello"));
    }

    @Test
    void suppressesRepeatedMessage() {
        ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(100));
        debouncer.shouldSend("actor1", "hello");
        assertFalse(debouncer.shouldSend("actor1", "hello"));
    }

    @Test
    void allowsDifferentMessage() {
        ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(100));
        debouncer.shouldSend("actor1", "hello");
        assertTrue(debouncer.shouldSend("actor1", "world"));
    }

    @Test
    void allowsSameMessageAfterWindow() {
        MutableClock clock = new MutableClock();
        ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(100), clock);
        debouncer.shouldSend("actor1", "hello");
        clock.advance(Duration.ofMillis(150));
        assertTrue(debouncer.shouldSend("actor1", "hello"));
    }

    @Test
    void differentActorsAreIndependent() {
        ActorMessageDebouncer debouncer = new ActorMessageDebouncer(Duration.ofMillis(100));
        debouncer.shouldSend("actor1", "hello");
        assertTrue(debouncer.shouldSend("actor2", "hello"));
    }
}
