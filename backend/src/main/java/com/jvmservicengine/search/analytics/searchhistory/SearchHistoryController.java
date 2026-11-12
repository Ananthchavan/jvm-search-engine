package com.jvmservicengine.search.analytics.searchhistory;

import com.jvmservicengine.search.api.dto.response.SearchHistoryDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/history")
@RequiredArgsConstructor
@Validated
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<Page<SearchHistoryDto>> getHistory(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be >= 0")
            int page,

            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size
    ) {
        return ResponseEntity.ok(searchHistoryService.getRecentSearches(page, size));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSearchMetrics() {
        return ResponseEntity.ok(searchHistoryService.getSearchMetrics());
    }
}
