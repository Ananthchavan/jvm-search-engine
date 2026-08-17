package com.jvmservicengine.search.analytics.statistics;

import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import com.jvmservicengine.search.storage.repository.TermRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SiteStatsService {

    private final SiteStatsRepository siteStatsRepository;
    private final PageRepository pageRepository;
    private final TermRepository termRepository;

    @Transactional(readOnly = true)
    public SiteStats getLatestStats() {
        return siteStatsRepository.findTopByOrderByIdDesc().orElseGet(SiteStats::new);
    }

    @Transactional
    public void recalculateStats() {
        SiteStats stats = siteStatsRepository.findTopByOrderByIdDesc()
                .orElseGet(() -> {
                    SiteStats newStats = new SiteStats();
                    newStats.setStartedAt(LocalDateTime.now());
                    return newStats;
                });

        stats.setTotalPages(pageRepository.count());
        stats.setIndexedPages(pageRepository.count());
        stats.setTotalTerms(termRepository.count());
        stats.setLastCrawl(LocalDateTime.now());

        siteStatsRepository.save(stats);
    }
}
