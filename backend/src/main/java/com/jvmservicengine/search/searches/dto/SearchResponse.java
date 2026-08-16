package com.jvmservicengine.search.searches.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private String originalQuery;
    private int totalResults;
    private long executionTimeMs;
    private int currentPage;
    private int totalPages;
    private List<SearchResultItem> results;
}
