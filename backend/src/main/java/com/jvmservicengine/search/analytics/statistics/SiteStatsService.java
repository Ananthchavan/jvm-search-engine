package com.jvmservicengine.search.analytics.statistics;

import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteStatsService {

    private final SiteStatsRepository siteStatsRepository;

    @Transactional(readOnly = true)
    public SiteStats getLatestStats() {
        return siteStatsRepository.findTopByOrderByIdDesc().orElseGet(SiteStats::new);
    }
}
