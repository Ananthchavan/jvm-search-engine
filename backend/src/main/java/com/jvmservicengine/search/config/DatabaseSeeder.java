package com.jvmservicengine.search.config;

import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.common.enums.PageStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.entity.Term;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import com.jvmservicengine.search.storage.repository.TermRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Bean
    CommandLineRunner initDatabase(
            PageRepository pageRepository,
            TermRepository termRepository,
            CrawlerQueueRepository crawlerQueueRepository,
            SiteStatsRepository siteStatsRepository) {

        return args -> {
            // 1. Only seed initial pages/terms if pages table is empty
            if (pageRepository.count() == 0) {
                log.info("Seeding database with test data...");

                // 1. Create a dummy Page
                Page springPage = new Page();
                springPage.setUrl("https://spring.io/projects/spring-boot");
                springPage.setTitle("Spring Boot");
                springPage.setContentPreview("Spring Boot makes it easy to create stand-alone applications.");
                springPage.setStatus(PageStatus.ACTIVE);
                springPage.setCrawlDepth(0);
                pageRepository.save(springPage);
                log.info("Saved Page: " + springPage.getTitle());

                // 2. Create some dummy Terms
                Term term1 = new Term();
                term1.setTerm("spring");
                term1.setDocumentFrequency(1);
                term1.setTotalFrequency(5);
                termRepository.save(term1);

                Term term2 = new Term();
                term2.setTerm("boot");
                term2.setDocumentFrequency(1);
                term2.setTotalFrequency(3);
                termRepository.save(term2);
                log.info("Saved Terms: spring, boot");

                // 3. Initialize Site Stats
                SiteStats stats = new SiteStats();
                stats.setTotalPages(1L);
                stats.setIndexedPages(1L);
                stats.setTotalTerms(2L);
                stats.setStartedAt(LocalDateTime.now());
                siteStatsRepository.save(stats);
                log.info("Initialized Site Stats");

                log.info("Database pages seeding completed successfully!");
            } else {
                log.info("Database already contains data. Skipping pages seed.");
            }

            // 2. Seed initial crawler queue if queue has no pending/processing items
            long pendingCount = crawlerQueueRepository.countByStatus(CrawlStatus.PENDING);
            long processingCount = crawlerQueueRepository.countByStatus(CrawlStatus.PROCESSING);
            String defaultSeed = "https://en.wikipedia.org/wiki/Java_(programming_language)";

            if (pendingCount == 0 && processingCount == 0 && !crawlerQueueRepository.existsByUrl(defaultSeed)) {
                CrawlerQueueItem queueItem = new CrawlerQueueItem();
                queueItem.setUrl(defaultSeed);
                queueItem.setStatus(CrawlStatus.PENDING);
                queueItem.setPriority(100);
                queueItem.setCrawlDepth(0);
                queueItem.setCrawlOriginHost("en.wikipedia.org");
                queueItem.setLastCrawledAt(LocalDateTime.now());
                crawlerQueueRepository.save(queueItem);
                log.info("Seeded initial queue item: {}", queueItem.getUrl());
            }
        };
    }
}