package com.jvmservicengine.search.api.dto.response;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for site statistics.
 * Replaces direct {@link com.jvmservicengine.search.storage.entity.SiteStats} exposure.
 */
public record SiteStatsDto(
        Long totalPages,
        Long indexedPages,
        Long totalTerms,
        Long totalPostings,
        LocalDateTime startedAt,
        LocalDateTime lastCrawl,
        LocalDateTime completedAt
) {}

