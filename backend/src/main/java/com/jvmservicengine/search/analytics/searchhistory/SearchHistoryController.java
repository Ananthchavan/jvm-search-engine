package com.jvmservicengine.search.analytics.searchhistory;

import com.jvmservicengine.search.api.dto.response.SearchHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<Page<SearchHistoryDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(searchHistoryService.getRecentSearches(page, size));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSearchMetrics() {
        return ResponseEntity.ok(searchHistoryService.getSearchMetrics());
    }
}
