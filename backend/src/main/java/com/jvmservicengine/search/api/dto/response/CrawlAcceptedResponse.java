package com.jvmservicengine.search.api.dto.response;

/**
 * Response body for POST /api/v1/crawl (202 Accepted).
 */
public record CrawlAcceptedResponse(String status, String seedUrl) {}

