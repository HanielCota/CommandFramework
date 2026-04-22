package io.github.hanielcota.commandframework.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public final class MutableClock extends Clock {

    private Instant now = Instant.EPOCH;

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(getZone())) {
            return this;
        }
        MutableClock copy = new MutableClock();
        copy.now = this.now;
        return copy;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
