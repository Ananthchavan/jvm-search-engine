package com.jvmservicengine.search.storage.entity;


import com.jvmservicengine.search.common.enums.CrawlStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawler_queue")
@Getter
@Setter
@NoArgsConstructor
public class CrawlerQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlStatus status = CrawlStatus.PENDING;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "crawl_depth", nullable = false)
    private Integer crawlDepth = 0;

    @UpdateTimestamp
    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

}
