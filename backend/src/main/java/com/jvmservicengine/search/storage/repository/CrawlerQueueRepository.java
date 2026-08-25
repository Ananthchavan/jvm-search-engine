package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlerQueueRepository extends JpaRepository<CrawlerQueueItem, Long> {

    boolean existsByUrl(String url);

    Optional<CrawlerQueueItem> findFirstByStatusOrderByPriorityDesc(CrawlStatus status);

    List<CrawlerQueueItem> findTop10ByStatusOrderByPriorityDescCrawlDepthAsc(CrawlStatus status);

    @Query("SELECT q.url FROM CrawlerQueueItem q")
    List<String> findAllUrls();

    long countByStatus(CrawlStatus status);
    List<CrawlerQueueItem> findTop50ByStatusOrderByLastCrawledAtDesc(CrawlStatus status);


}
