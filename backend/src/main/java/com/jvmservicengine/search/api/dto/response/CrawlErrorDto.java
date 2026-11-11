package com.jvmservicengine.search.api.dto.response;

import com.jvmservicengine.search.common.enums.CrawlStatus;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for failed crawler-queue items.
 * Replaces direct {@link com.jvmservicengine.search.storage.entity.CrawlerQueueItem} exposure.
 */
public record CrawlErrorDto(
        Long id,
        String url,
        CrawlStatus status,
        Integer crawlDepth,
        String errorMessage,
        LocalDateTime lastCrawledAt
) {}

