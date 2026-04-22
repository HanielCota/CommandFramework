package io.github.hanielcota.commandframework.core.cooldown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.MutableClock;
import io.github.hanielcota.commandframework.core.TestActor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class RouteCooldownStoreTest {

    @Test
    void deniesActorUntilCooldownExpires() {
        MutableClock clock = new MutableClock();
        RouteCooldownStore store = new RouteCooldownStore(clock);
        CommandRoute route = route();
        TestActor actor = new TestActor(ActorKind.PLAYER, "cooldown-test");
        assertTrue(store.claim(actor, route).isAllowed());
        assertFalse(store.claim(actor, route).isAllowed());
        clock.advance(Duration.ofSeconds(3));
        assertTrue(store.claim(actor, route).isAllowed());
    }

    @Test
    void allowsOnlyOneConcurrentCooldownClaim() throws Exception {
        RouteCooldownStore store = new RouteCooldownStore();
        CommandRoute route = route();
        int workers = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        TestActor sharedActor = new TestActor(ActorKind.PLAYER, "concurrent-actor");
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    if (store.claim(sharedActor, route).isAllowed()) {
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
        assertEquals(1, allowed.get());
    }

    private CommandRoute route() {
        return CommandRoute.builder("kit", (context, parameters) -> CommandResult.success())
                .cooldown(Duration.ofSeconds(3))
                .build();
    }
}
