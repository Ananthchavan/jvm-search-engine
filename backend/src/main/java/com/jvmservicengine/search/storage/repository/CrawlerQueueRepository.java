package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlerQueueRepository extends JpaRepository<CrawlerQueueItem, Long> {

    boolean existsByUrl(String url);

    /**
     * Atomically selects the highest-priority PENDING item locking it with FOR UPDATE SKIP LOCKED
     */
    @Query(value = """
            SELECT *
              FROM crawler_queue
             WHERE status = 'PENDING'
             ORDER BY priority DESC, crawl_depth ASC, id ASC
             LIMIT 1
               FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<CrawlerQueueItem> findNextPendingForUpdate();

    
    @Modifying
    @Transactional
    @Query("UPDATE CrawlerQueueItem q SET q.status = :pending WHERE q.status = :processing")
    int resetStuckProcessingItems(@Param("pending") CrawlStatus pending,
                                   @Param("processing") CrawlStatus processing);

    List<CrawlerQueueItem> findTop10ByStatusOrderByPriorityDescCrawlDepthAsc(CrawlStatus status);

    @Query("SELECT q.url FROM CrawlerQueueItem q")
    List<String> findAllUrls();

    long countByStatus(CrawlStatus status);

    List<CrawlerQueueItem> findTop50ByStatusOrderByLastCrawledAtDesc(CrawlStatus status);
}
