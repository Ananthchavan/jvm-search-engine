package com.jvmservicengine.search.crawler.service;

import com.jvmservicengine.search.api.dto.ParsedPageData;
import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.jsoup.JsoupWebClient;
import com.jvmservicengine.search.crawler.robots.RobotsTxtService;
import com.jvmservicengine.search.indexing.invertedindex.InMemoryInvertedIndex;
import com.jvmservicengine.search.parser.service.HtmlParserService;
import com.jvmservicengine.search.processing.service.TextProcessingService;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final CrawlerQueueService queueService;

    private final PageRepository pageRepository;
    private final JsoupWebClient jsoupWebClient;
    private final RobotsTxtService robotsTxtService;
    private final RateLimiterService rateLimiterService;
    private final HtmlParserService htmlParserService;
    private final TextProcessingService textProcessingService;
    private final InMemoryInvertedIndex inMemoryInvertedIndex;

    private static final int MAX_CRAWL_DEPTH = 3;
    private static final int CONTENT_PREVIEW_MAX_LENGTH = 500;

    @Async("crawlerTaskExecutor")
    public void addSeedAndStart(String seedUrl) {
        log.info("Initializing crawl with seed URL: {}", seedUrl);
        queueService.addUrlToQueue(seedUrl, 0, 100);
        processQueue();
    }

    @Async("crawlerTaskExecutor")
    public void processQueue() {
        log.info("Crawler Worker [{}] waking Up...", Thread.currentThread().getName());

        while (true) {
            Optional<CrawlerQueueItem> nextItemOpt = queueService.getNextItem();

            if (nextItemOpt.isEmpty()) {
                log.info("Crawler Queue is empty. Worker going back to standby.");
                break;
            }

            CrawlerQueueItem currentItem = nextItemOpt.get();
            queueService.updateStatus(currentItem, CrawlStatus.PROCESSING);

            try {
                if (!robotsTxtService.isAllowed(currentItem.getUrl())) {
                    log.info("URL blocked by robots.txt, skipping: {}", currentItem.getUrl());
                    queueService.updateStatus(currentItem, CrawlStatus.FAILED);
                    continue;
                }

                String domain = new URL(currentItem.getUrl()).getHost();
                rateLimiterService.enforcePoliteness(domain);

                Optional<Document> docOpt = jsoupWebClient.fetchDocument(currentItem.getUrl());

                if (docOpt.isPresent()) {
                    Document doc = docOpt.get();
                    String rawHtml = doc.outerHtml();
                    ParsedPageData parsedData = htmlParserService.parse(rawHtml, currentItem.getUrl());

                    if (parsedData != null) {
                        try {
                            String bodyText = parsedData.bodyText();
                            String preview = bodyText != null && bodyText.length() > CONTENT_PREVIEW_MAX_LENGTH
                                    ? bodyText.substring(0, CONTENT_PREVIEW_MAX_LENGTH)
                                    : bodyText;

                            // --- UPSERT: update if URL already exists, insert if new ---
                            Optional<Page> existingPage = pageRepository.findByUrl(currentItem.getUrl());

                            Page page;
                            boolean isNewPage = existingPage.isEmpty();

                            if (isNewPage) {
                                page = new Page();
                                page.setUrl(currentItem.getUrl());
                                page.setCrawlDepth(currentItem.getCrawlDepth());
                                page.setCreatedAt(LocalDateTime.now());
                            } else {
                                page = existingPage.get();
                                log.info("Page already exists, updating content for: {}", page.getUrl());
                            }

                            page.setTitle(parsedData.title());
                            page.setContentHash(bodyText);
                            page.setContentPreview(preview);
                            pageRepository.save(page);

                            // Only index new pages to avoid duplicate postings in the inverted index
                            if (isNewPage && bodyText != null && !bodyText.isBlank()) {
                                Map<String, Integer> termFrequencies = textProcessingService.process(bodyText);
                                List<String> tokens = new java.util.ArrayList<>(termFrequencies.keySet());
                                inMemoryInvertedIndex.addDocument(page.getId(), tokens);
                                log.info("Indexed {} unique terms for page: {}", tokens.size(), page.getUrl());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to save/index URL: {} — {}", currentItem.getUrl(), e.getMessage());
                        }

                        if (currentItem.getCrawlDepth() < MAX_CRAWL_DEPTH) {
                            for (String nextUrl : parsedData.outgoingLinks()) {
                                queueService.addUrlToQueue(nextUrl, currentItem.getCrawlDepth() + 1, 50);
                            }
                        }

                    }

                    queueService.updateStatus(currentItem, CrawlStatus.DONE);
                    log.info("Successfully crawled, parsed, and saved: {}", currentItem.getUrl());

                } else {
                    queueService.updateStatus(currentItem, CrawlStatus.FAILED);
                }
            } catch (Exception e) {
                log.error("Unexpected error crawling URL: {}", currentItem.getUrl(), e);
                queueService.updateStatus(currentItem, CrawlStatus.FAILED);
            }
        }
    }
    
}