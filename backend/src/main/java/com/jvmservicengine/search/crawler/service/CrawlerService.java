package com.jvmservicengine.search.crawler.service;

import com.jvmservicengine.search.api.dto.ParsedPageData;
import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.jsoup.JsoupWebClient;
import com.jvmservicengine.search.crawler.robots.RobotsTxtService;
import com.jvmservicengine.search.parser.service.HtmlParserService;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
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

    private static final int MAX_CRAWL_DEPTH = 3;

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
                        Page page = new Page();
                        page.setUrl(currentItem.getUrl());
                        page.setTitle(parsedData.title());
                        page.setContentHash(parsedData.bodyText());
                        page.setCreatedAt(LocalDateTime.now());
                        pageRepository.save(page);

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