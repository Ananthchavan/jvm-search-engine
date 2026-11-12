package com.jvmservicengine.search.api.controller;

import com.jvmservicengine.search.api.dto.request.CrawlRequest;
import com.jvmservicengine.search.api.dto.response.CrawlAcceptedResponse;
import com.jvmservicengine.search.api.dto.response.CrawlErrorDto;
import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.service.CrawlerService;
import com.jvmservicengine.search.indexing.service.IndexingService;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/crawl")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlerService crawlerService;
    private final IndexingService indexingService;
    private final CrawlerQueueRepository crawlerQueueRepository;

    @PostMapping
    public ResponseEntity<CrawlAcceptedResponse> startCrawl(@Valid @RequestBody CrawlRequest request) {
        crawlerService.addSeedAndStart(request.seedUrl());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CrawlAcceptedResponse("accepted", request.seedUrl()));
    }

    /** Manual trigger — flushes the in-memory index to DB immediately */
    @PostMapping("/flush-index")
    public ResponseEntity<String> flushIndex() {
        boolean started = indexingService.flushToDatabase();
        if (!started) {
            return ResponseEntity.status(409)
                    .body("A flush is already in progress. Please try again later.");
        }
        return ResponseEntity.ok("Index flushed to database successfully.");
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getQueueStats() {
        return ResponseEntity.ok(Map.of(
                "pending",    crawlerQueueRepository.countByStatus(CrawlStatus.PENDING),
                "processing", crawlerQueueRepository.countByStatus(CrawlStatus.PROCESSING),
                "completed",  crawlerQueueRepository.countByStatus(CrawlStatus.DONE),
                "failed",     crawlerQueueRepository.countByStatus(CrawlStatus.FAILED)
        ));
    }

    @GetMapping("/errors")
    public ResponseEntity<List<CrawlErrorDto>> getCrawlerErrors() {
        List<CrawlerQueueItem> items =
                crawlerQueueRepository.findTop50ByStatusOrderByLastCrawledAtDesc(CrawlStatus.FAILED);
        List<CrawlErrorDto> dtos = items.stream()
                .map(i -> new CrawlErrorDto(
                        i.getId(), i.getUrl(), i.getStatus(),
                        i.getCrawlDepth(), i.getErrorMessage(), i.getLastCrawledAt()))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
