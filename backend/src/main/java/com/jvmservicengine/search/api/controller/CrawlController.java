package com.jvmservicengine.search.api.controller;


import com.jvmservicengine.search.api.dto.request.CrawlRequest;
import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.service.CrawlerService;
import com.jvmservicengine.search.indexing.service.IndexingService;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CrawlController {

    private final CrawlerService crawlerService;
    private final IndexingService indexingService;
    private final CrawlerQueueRepository crawlerQueueRepository;

    @PostMapping
    public ResponseEntity<String> startCrawl(@RequestBody CrawlRequest request){
        crawlerService.addSeedAndStart(request.seedUrl());
        return ResponseEntity.accepted().body("Crawl successfully started for: " + request.seedUrl());
    }

    /** Manual trigger — flushes the in-memory index to DB immediately */
    @PostMapping("/flush-index")
    public ResponseEntity<String> flushIndex() {
        indexingService.flushToDatabase();
        return ResponseEntity.ok("Index flushed to database successfully.");
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getQueueStats() {
        return ResponseEntity.ok(Map.of(
                "pending", crawlerQueueRepository.countByStatus(CrawlStatus.PENDING),
                "processing", crawlerQueueRepository.countByStatus(CrawlStatus.PROCESSING),
                "completed", crawlerQueueRepository.countByStatus(CrawlStatus.DONE),
                "failed", crawlerQueueRepository.countByStatus(CrawlStatus.FAILED)
        ));
    }

    @GetMapping("/errors")
    public ResponseEntity<List<CrawlerQueueItem>> getCrawlerErrors() {
        return ResponseEntity.ok(crawlerQueueRepository.findTop50ByStatusOrderByLastCrawledAtDesc(CrawlStatus.FAILED));
    }
    
}
