package com.jvmservicengine.search.crawler.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {

    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    private static final long DELAY_MS = 2000;

    public void enforcePoliteness(String domain) {
        long now = System.currentTimeMillis();
        long lastAccess = lastAccessTime.getOrDefault(domain, 0L);
        long timeSinceLastAccess = now - lastAccess;

        if(timeSinceLastAccess < DELAY_MS) {
            long sleepTime = DELAY_MS - timeSinceLastAccess;
            try{
                log.debug("Rate limiting: Sleeping for {} ms for domain {}", sleepTime, domain);
                Thread.sleep(sleepTime);
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                log.error("Rate limiter interrupted", e);
            }
        }

        lastAccessTime.put(domain, System.currentTimeMillis());
    }
}
