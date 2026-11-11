package com.jvmservicengine.search.crawler.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {

    private static final long DEFAULT_DELAY_MS = 2000;
    private static final long MAX_DELAY_MS = 30_000;

    private final ConcurrentHashMap<String, Long> nextAllowedAt = new ConcurrentHashMap<>();

    public boolean enforcePoliteness(String domain, long crawlDelayMs) {
        long effectiveDelay = Math.min(Math.max(DEFAULT_DELAY_MS, crawlDelayMs), MAX_DELAY_MS);

        long now = System.currentTimeMillis();

        // Atomically claim the next available slot — no other thread can grab
        // the same instant because compute() is exclusive per key.
        long sleepUntil = nextAllowedAt.compute(domain, (k, prev) -> {
            long earliest = (prev == null) ? now : Math.max(now, prev);
            return earliest + effectiveDelay;
        });

        long waitMs = sleepUntil - System.currentTimeMillis();
        if (waitMs > 0) {
            log.debug("Rate limiting: sleeping {} ms for domain {}", waitMs, domain);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                // Restore the interrupt flag and tell the caller to stop.
                Thread.currentThread().interrupt();
                log.warn("Rate limiter interrupted while waiting for domain {}", domain);
                return false;
            }
        }

        return true;
    }
}
