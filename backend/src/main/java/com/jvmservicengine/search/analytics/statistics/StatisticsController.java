package com.jvmservicengine.search.analytics.statistics;

import com.jvmservicengine.search.storage.entity.SiteStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final SiteStatsService siteStatsService;

    @GetMapping
    public ResponseEntity<SiteStats> getStats() {
        return ResponseEntity.ok(siteStatsService.getLatestStats());
    }
}
