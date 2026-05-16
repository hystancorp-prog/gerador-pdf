package com.hystan.demo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private static class Counter {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * Fixed-window rate limiter, keyed by (ip + endpoint).
     * Thread-safe per-key via synchronized on the Counter object.
     */
    public boolean isAllowed(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStart >= windowMs) {
                c.windowStart = now;
                c.count.set(1);
                return true;
            }
            return c.count.incrementAndGet() <= maxRequests;
        }
    }

    // Prevent unbounded map growth — remove entries idle for more than 2 windows
    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(e -> now - e.getValue().windowStart > 120_000);
    }
}
