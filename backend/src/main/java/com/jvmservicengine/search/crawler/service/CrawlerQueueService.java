package com.jvmservicengine.search.crawler.service;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import com.jvmservicengine.search.storage.repository.PageRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CrawlerQueueService {

    private final CrawlerQueueRepository crawlerQueueRepository;
    private final PageRepository pageRepository;

    /** Comma-separated list of root domains to block, injected from application.properties. */
    private final List<String> blockedDomains;

    public CrawlerQueueService(CrawlerQueueRepository crawlerQueueRepository,
                               PageRepository pageRepository,
                               @Value("${crawler.blocked-domains:oracle.com,twitter.com,facebook.com,linkedin.com,youtube.com,instagram.com,t.co}")
                               List<String> blockedDomains) {
        this.crawlerQueueRepository = crawlerQueueRepository;
        this.pageRepository = pageRepository;
        this.blockedDomains = blockedDomains;
    }

    private final Set<String> seenUrls = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initSeenUrls() {
        // Reset items stuck in PROCESSING from a previous JVM run
        int reset = crawlerQueueRepository.resetStuckProcessingItems(
                CrawlStatus.PENDING, CrawlStatus.PROCESSING);
        if (reset > 0) {
            log.info("[STARTUP] Reset {} stuck PROCESSING item(s) back to PENDING.", reset);
        }

        // Warm up the in-memory deduplication cache
        log.info("Warming up URL deduplication cache from PostgreSQL...");
        seenUrls.addAll(pageRepository.findAllUrls());
        seenUrls.addAll(crawlerQueueRepository.findAllUrls());
        log.info("Deduplication cache initialized with {} known URLs.", seenUrls.size());
    }

    public void addUrlToQueue(String url, int depth, int priority) {
        if (!isValidUrl(url)) return;

        if (!seenUrls.add(url)) return;

        if (pageRepository.existsByUrl(url) || crawlerQueueRepository.existsByUrl(url)) {
            log.debug("URL already known in DB, skipping: {}", url);
            return;
        }

        try {
            CrawlerQueueItem newItem = new CrawlerQueueItem();
            newItem.setUrl(url);
            newItem.setStatus(CrawlStatus.PENDING);
            newItem.setCrawlDepth(depth);
            newItem.setPriority(priority);
            newItem.setLastCrawledAt(LocalDateTime.now());

            crawlerQueueRepository.save(newItem);
            log.debug("Added URL to frontier: {}", url);
        } catch (Exception e) {
            seenUrls.remove(url);
            log.warn("Failed to persist URL to queue, removed from seen set: {} — {}", url, e.getMessage());
        }
    }

    /**
    Atomically claims the next PENDING item by marking it PROCESSING in one
     */
    @Transactional
    public Optional<CrawlerQueueItem> getNextItem() {
        return crawlerQueueRepository.claimNextPending();
    }

    public void updateStatus(CrawlerQueueItem item, CrawlStatus status) {
        item.setStatus(status);
        item.setLastCrawledAt(LocalDateTime.now());
        crawlerQueueRepository.save(item);
    }

    private boolean isValidUrl(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return false;
        }
        try {
            String host = new java.net.URL(url).getHost().toLowerCase();
            for (String blocked : blockedDomains) {
                // Match on domain boundaries only:
                //   host equals the blocked domain exactly, or
                //   host is a subdomain of it (ends with ".<blocked>")
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    log.debug("Blocked domain, skipping: {}", url);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
