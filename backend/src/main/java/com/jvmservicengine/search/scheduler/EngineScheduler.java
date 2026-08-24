package com.jvmservicengine.search.scheduler;

import com.jvmservicengine.search.crawler.service.CrawlerService;
import com.jvmservicengine.search.indexing.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EngineScheduler {

    private final CrawlerService crawlerService;
    private final IndexingService indexingService;

    @Scheduled(fixedDelay = 300000)
    public void scheduleCrawling() {
        log.info("[SCHEDULER] Waking up to process crawler queue");

        try {
            crawlerService.processQueue();
        } catch (Exception e) {
            log.error("Scheduled crawler task failed : {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void scheduleIndexFlushing() {
        log.info("[SCHEDULER] Waking up for periodic index flush");

        try {
            indexingService.flushToDatabase();
        } catch (Exception e) {
            log.error("Scheduled indexing flush failed: {}", e.getMessage());
        }
    }
}
