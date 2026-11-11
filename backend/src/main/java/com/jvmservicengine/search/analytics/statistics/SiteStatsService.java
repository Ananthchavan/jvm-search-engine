package com.jvmservicengine.search.analytics.statistics;

import com.jvmservicengine.search.api.dto.response.SiteStatsDto;
import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.PostingRepository;
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
    private final PostingRepository postingRepository;

    @Transactional(readOnly = true)
    public SiteStatsDto getLatestStatsDto() {
        SiteStats s = siteStatsRepository.findTopByOrderByIdDesc().orElseGet(SiteStats::new);
        return new SiteStatsDto(
                s.getTotalPages(), s.getIndexedPages(), s.getTotalTerms(),
                s.getTotalPostings(), s.getStartedAt(), s.getLastCrawl(), s.getCompletedAt());
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
        stats.setTotalPostings(postingRepository.count());
        stats.setLastCrawl(LocalDateTime.now());

        siteStatsRepository.save(stats);
    }
}
