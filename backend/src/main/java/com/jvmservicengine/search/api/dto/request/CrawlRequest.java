package com.jvmservicengine.search.api.dto.request;

import com.jvmservicengine.search.crawler.validation.SafeUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrawlRequest(
        @NotBlank(message = "seedUrl must not be blank")
        @Size(max = 2048, message = "seedUrl must not exceed 2048 characters")
        @SafeUrl
        String seedUrl
) {}
