package com.jvmservicengine.search.api.dto;

public record SearchResultDTO(
    String title,
    String url,
    String snippet,
    double relevanceScore
) {}
