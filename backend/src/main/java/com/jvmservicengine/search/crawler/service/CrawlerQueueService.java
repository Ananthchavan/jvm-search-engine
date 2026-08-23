package com.jvmservicengine.search.crawler.service;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import com.jvmservicengine.search.storage.repository.PageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerQueueService {

    private final CrawlerQueueRepository crawlerQueueRepository;
    private final PageRepository pageRepository;

    private final Set<String> seenUrls = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initSeenUrls() {
        log.info("Warming up URL deduplication cache from PostgreSQL...");
        seenUrls.addAll(pageRepository.findAllUrls());
        seenUrls.addAll(crawlerQueueRepository.findAllUrls());
        log.info("Deduplication cache initialized with {} known URLs.", seenUrls.size());
    }

    public void addUrlToQueue(String url, int depth, int priority) {
        if (!isValidUrl(url)) return;

        // Fast in-memory check first
        if (!seenUrls.add(url)) return;

        // Hard DB guard — catches URLs saved in previous server runs or the current crawl
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
            log.debug("URL already exists in database, skipping duplicate: {}", url);
        }
    }

    public Optional<CrawlerQueueItem> getNextItem() {
        return crawlerQueueRepository.findFirstByStatusOrderByPriorityDesc(CrawlStatus.PENDING);
    }

    public void updateStatus(CrawlerQueueItem item, CrawlStatus status) {
        item.setStatus(status);
        item.setLastCrawledAt(LocalDateTime.now());
        crawlerQueueRepository.save(item);
    }

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "oracle.com",
            "docs.oracle.com",
            "login.oracle.com",
            "support.oracle.com",
            "twitter.com",
            "facebook.com",
            "linkedin.com",
            "youtube.com",
            "instagram.com",
            "t.co"
    );

    private boolean isValidUrl(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return false;
        }
        try {
            String host = new java.net.URL(url).getHost().toLowerCase();
            boolean blocked = BLOCKED_DOMAINS.stream().anyMatch(host::contains);
            if (blocked) {
                log.debug("Blocked domain, skipping: {}", url);
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
