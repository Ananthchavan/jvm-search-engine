package com.jvmservicengine.search.crawler.service;



import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.jsoup.JsoupWebClient;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import com.jvmservicengine.search.storage.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final CrawlerQueueRepository crawlerQueueRepository;
    private final PageRepository pageRepository;
    private final JsoupWebClient jsoupWebClient;

    private static final int MAX_CRAWL_DEPTH = 3;

    public void addSeedAndStart(String seedUrl) {
        // Check if it is already crawled or if it's already in the queue
        if(!pageRepository.findByUrl(seedUrl).isEmpty() && !crawlerQueueRepository.findByUrl(seedUrl).isEmpty()) {
            CrawlerQueueItem seed = new CrawlerQueueItem();
            seed.setUrl(seedUrl);
            seed.setStatus(CrawlStatus.PENDING);
            seed.setCrawlDepth(0);
            seed.setPriority(100);
            seed.setLastCrawledAt(LocalDateTime.now());

            crawlerQueueRepository.save(seed);
            log.info("Seed URL added to crawler Queue: {}", seedUrl);
        }
        processQueue();
    }

    @Async("crawlerTaskExecutor")
    public void processQueue() {
        log.info("Crawler Worker [{}] waking Up...", Thread.currentThread().getName());

        while (true) {

            Optional<CrawlerQueueItem> nextItemOpt = crawlerQueueRepository
                    .findFirstByStatusOrderByPriorityDesc(CrawlStatus.PENDING);

            if (nextItemOpt.isEmpty()) {
                log.info("Crawler Queue is empty. Worker going back to standby.");
                break;
            }

            CrawlerQueueItem currentItem = nextItemOpt.get();

            currentItem.setStatus(CrawlStatus.PROCESSING);
            crawlerQueueRepository.save(currentItem);

            try {

                Optional<Document> docOpt = jsoupWebClient.fetchDocument(currentItem.getUrl());

                if( docOpt.isPresent()) {
                    Document doc = docOpt.get();

                    Page page = new Page();
                    page.setUrl(currentItem.getUrl());
                    page.setTitle(doc.title());
                    page.setContentHash(doc.html());
                    page.setCreatedAt(LocalDateTime.now());
                    pageRepository.save(page);

                    if(currentItem.getCrawlDepth() < MAX_CRAWL_DEPTH) {
                        extractAndQueueLinks(doc, currentItem.getCrawlDepth() + 1);
                    }

                    currentItem.setStatus(CrawlStatus.DONE);
                    log.info("Successfully crawled and saved: {}", currentItem.getUrl());

                } else {
                    currentItem.setStatus(CrawlStatus.FAILED);
                }
            } catch (Exception e) {
                log.error("Unexpected error crawling URL: {}", currentItem.getUrl(), e);
                currentItem.setStatus(CrawlStatus.FAILED);
            } finally {
                currentItem.setLastCrawledAt(LocalDateTime.now());
                crawlerQueueRepository.save(currentItem);
            }
        }
    }

    private void extractAndQueueLinks(Document doc, int nextDepth) {
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String nextUrl = link.absUrl("href");

            if(isValidUrl(nextUrl) && !pageRepository.existsByUrl(nextUrl) && !crawlerQueueRepository.existsByUrl(nextUrl)) {
                CrawlerQueueItem newItem = new CrawlerQueueItem();
                newItem.setUrl(nextUrl);
                newItem.setStatus(CrawlStatus.PENDING);
                newItem.setCrawlDepth(nextDepth);
                newItem.setPriority(50);
                newItem.setLastCrawledAt(LocalDateTime.now());

                crawlerQueueRepository.save(newItem);
            }
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

}
