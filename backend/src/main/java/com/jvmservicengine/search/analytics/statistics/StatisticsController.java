package com.jvmservicengine.search.analytics.statistics;

import com.jvmservicengine.search.api.dto.response.SiteStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final SiteStatsService siteStatsService;

    @GetMapping
    public ResponseEntity<SiteStatsDto> getStats() {
        return ResponseEntity.ok(siteStatsService.getLatestStatsDto());
    }
}
