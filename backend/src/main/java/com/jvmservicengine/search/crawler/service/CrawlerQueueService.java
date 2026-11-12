package com.jvmservicengine.search.crawler.service;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.crawler.validation.UrlSafetyValidator;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import com.jvmservicengine.search.storage.repository.PageRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
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
    private final UrlSafetyValidator urlSafetyValidator;

    private final List<String> blockedDomains;

    private final boolean allowExternalLinks;

    private final int maxPagesPerCrawl;

    private final java.util.concurrent.atomic.AtomicInteger pagesQueuedCount =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public CrawlerQueueService(
            CrawlerQueueRepository crawlerQueueRepository,
            PageRepository pageRepository,
            UrlSafetyValidator urlSafetyValidator,
            @Value("${crawler.blocked-domains:oracle.com,twitter.com,facebook.com,linkedin.com,youtube.com,instagram.com,t.co}")
            List<String> blockedDomains,
            @Value("${crawler.allow-external-links:false}") boolean allowExternalLinks,
            @Value("${crawler.max-pages-per-crawl:500}") int maxPagesPerCrawl) {
        this.crawlerQueueRepository = crawlerQueueRepository;
        this.pageRepository = pageRepository;
        this.urlSafetyValidator = urlSafetyValidator;
        this.blockedDomains = blockedDomains;
        this.allowExternalLinks = allowExternalLinks;
        this.maxPagesPerCrawl = maxPagesPerCrawl;
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

    public void addUrlToQueue(String url, int depth, int priority, String originHost) {
        // 1. Page cap
        if (pagesQueuedCount.get() >= maxPagesPerCrawl) {
            log.debug("Page cap ({}) reached, skipping: {}", maxPagesPerCrawl, url);
            return;
        }

        // 2. Basic URL validity and blocklist
        if (!isValidUrl(url)) return;

        // 3. SSRF safety check
        if (!urlSafetyValidator.isSafe(url)) {
            log.debug("URL failed safety check, skipping: {}", url);
            return;
        }

        // 4. Domain scoping
        if (!allowExternalLinks && originHost != null) {
            try {
                String host = new URL(url).getHost().toLowerCase();
                String origin = originHost.toLowerCase();
                if (!host.equals(origin) && !host.endsWith("." + origin)) {
                    log.debug("Out-of-scope host '{}' (origin: '{}'), skipping: {}", host, origin, url);
                    return;
                }
            } catch (Exception e) {
                return;
            }
        }

        // 5. In-memory deduplication (add returns false if already present)
        if (!seenUrls.add(url)) return;

        // 6. DB-level deduplication
        if (pageRepository.existsByUrl(url) || crawlerQueueRepository.existsByUrl(url)) {
            log.debug("URL already known in DB, skipping: {}", url);
            return;
        }

        // 7. Persist
        try {
            CrawlerQueueItem newItem = new CrawlerQueueItem();
            newItem.setUrl(url);
            newItem.setStatus(CrawlStatus.PENDING);
            newItem.setCrawlDepth(depth);
            newItem.setPriority(priority);
            newItem.setCrawlOriginHost(originHost);
            newItem.setLastCrawledAt(LocalDateTime.now());

            crawlerQueueRepository.save(newItem);
            pagesQueuedCount.incrementAndGet();
            log.debug("Added URL to frontier (origin={}): {}", originHost, url);
        } catch (Exception e) {
            seenUrls.remove(url);
            log.warn("Failed to persist URL to queue, removed from seen set: {} — {}", url, e.getMessage());
        }
    }

    /**
     * Atomically claims the next PENDING item by marking it PROCESSING in one query.
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
            String host = new URL(url).getHost().toLowerCase();
            for (String blocked : blockedDomains) {
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
