package com.jvmservicengine.search.api.controller;


import com.jvmservicengine.search.api.dto.request.CrawlRequest;
import com.jvmservicengine.search.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CrawlController {

    private final CrawlerService crawlerService;

    @PostMapping
    public ResponseEntity<String> startCrawl(@RequestBody CrawlRequest request){

        crawlerService.addSeedAndStart(request.seedUrl());

        return ResponseEntity.accepted().body("Crawl successfully started for: " + request.seedUrl());
    }
}
