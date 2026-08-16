package com.jvmservicengine.search.searches.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultItem {
    private String url;
    private String title;
    private String snippet;
    private double relevanceScore;
}
