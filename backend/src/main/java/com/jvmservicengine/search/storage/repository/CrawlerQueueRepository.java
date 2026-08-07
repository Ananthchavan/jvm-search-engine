package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import com.jvmservicengine.search.storage.entity.CrawlerQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlerQueueRepository extends JpaRepository<CrawlerQueueItem, Long> {

    boolean existsByUrl(String url);

    Optional<CrawlerQueueItem> findByUrl(String url);

    List<CrawlerQueueItem> findTop10ByStatusOrderByPriorityDescCrawlDepthAsc(CrawlStatus status);
}
