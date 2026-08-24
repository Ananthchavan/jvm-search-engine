package com.jvmservicengine.search.api.controller;

import com.jvmservicengine.search.searches.dto.SearchResponse;
import com.jvmservicengine.search.searches.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "1") int page) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        int validPage = Math.max(1, page);
        SearchResponse response = searchService.search(query, validPage);

        return ResponseEntity.ok(response);
    }
}