package com.example.paperdemo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Toy in-memory economy. In a real plugin this would hit a database or cache.
 */
public final class EconomyService {

    private static final double STARTING_BALANCE = 100.0D;

    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    public double getBalance(UUID id) {
        return this.balances.computeIfAbsent(id, ignored -> STARTING_BALANCE);
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        return this.balances.compute(from, (ignored, current) -> {
            double now = current == null ? STARTING_BALANCE : current;
            if (now < amount) {
                return now;
            }
            this.balances.merge(to, amount, Double::sum);
            return now - amount;
        }) >= 0.0D;
    }
}
