package com.jvmservicengine.search.api.dto.response;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for search history entries.
 * Replaces direct {@link com.jvmservicengine.search.storage.entity.SearchHistory} exposure.
 */
public record SearchHistoryDto(
        Long id,
        String query,
        Integer resultCount,
        Integer latencyMs,
        LocalDateTime searchedAt
) {}

