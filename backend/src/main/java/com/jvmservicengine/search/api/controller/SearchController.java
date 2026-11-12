package com.jvmservicengine.search.api.controller;

import com.jvmservicengine.search.searches.dto.SearchResponse;
import com.jvmservicengine.search.searches.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private static final int USER_AGENT_MAX_LENGTH = 256;

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q")
            @NotBlank(message = "Search query must not be blank")
            @Size(max = 200, message = "Search query must not exceed 200 characters")
            String query,

            @RequestParam(name = "page", defaultValue = "1")
            @Min(value = 1, message = "page must be at least 1")
            @Max(value = 1000, message = "page must not exceed 1000")
            int page,

            HttpServletRequest request) {

        String remoteAddr = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > USER_AGENT_MAX_LENGTH) {
            userAgent = userAgent.substring(0, USER_AGENT_MAX_LENGTH);
        }

        SearchResponse response = searchService.search(query, page, remoteAddr, userAgent);
        return ResponseEntity.ok(response);
    }
}