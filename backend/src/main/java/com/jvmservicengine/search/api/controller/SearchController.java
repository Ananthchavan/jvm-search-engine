package com.jvmservicengine.search.api.controller;

import com.jvmservicengine.search.api.dto.SearchResultDTO;
import com.jvmservicengine.search.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<List<SearchResultDTO>> search(@RequestParam String query) {

        if(query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<RankingService.RankedResult> rawResults = rankingService.search(query);

        List<SearchResultDTO> response = rawResults.stream()
                .map(result -> new SearchResultDTO(
                        result.page().getTitle() != null ? result.page().getTitle() : result.page().getUrl(),
                        result.page().getUrl(),
                        result.page().getContentPreview(),
                        Math.round(result.score() * 100.0) / 100.0
                ))
                .limit(50)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
