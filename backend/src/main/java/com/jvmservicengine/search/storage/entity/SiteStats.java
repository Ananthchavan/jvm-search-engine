package com.jvmservicengine.search.storage.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_stats")
@Getter
@Setter
@NoArgsConstructor
public class SiteStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_pages", nullable = false)
    private Long totalPages = 0L;

    @Column(name = "indexed_pages", nullable = false)
    private Long indexedPages = 0L;

    @Column(name = "total_terms", nullable = false)
    private Long totalTerms = 0L;

    @Column(name = "total_postings", nullable = false)
    private Long totalPostings = 0L;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "last_crawl")
    private LocalDateTime lastCrawl;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

}
