package com.jvmservicengine.search.api.dto;


import java.util.Set;

public record ParsedPageData(
        String title,
        String bodyText,
        String metaDescription,
        Set<String> outgoingLinks
) {}
