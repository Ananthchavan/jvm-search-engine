package com.jvmservicengine.search.analytics.searchhistory;

import com.jvmservicengine.search.storage.entity.SearchHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<Page<SearchHistory>> getHitory(
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
