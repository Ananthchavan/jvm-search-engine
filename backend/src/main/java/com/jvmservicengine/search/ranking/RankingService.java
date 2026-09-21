package com.jvmservicengine.search.ranking;

import com.jvmservicengine.search.indexing.tfidf.TfIdfCalculator;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RankingService {

    private final TfIdfCalculator tfIdfCalculator;
    private final SiteStatsRepository siteStatsRepository;
    private final PageRepository pageRepository;

    /**
     * Ranks the given pages by cumulative TF-IDF score using pre-fetched postings.
     *
     * <p>Total document count is resolved internally: first from the latest
     * {@code SiteStats} snapshot, then falling back to a live
     * {@code pageRepository.count()} if SiteStats has not yet been populated.
     *
     * @param validPages     pages that have already passed query filtering (NOT exclusions etc.)
     * @param postingsByPage postings grouped by page, fetched by the caller
     * @return mutable map of page → cumulative TF-IDF score (unsorted)
     */
    public Map<Page, Double> rankPages(List<Page> validPages,
                                       Map<Page, List<Posting>> postingsByPage) {

        long totalPages = siteStatsRepository.findTopByOrderByIdDesc()
                .map(SiteStats::getIndexedPages)
                .orElse(0L);

        // Fall back to live count if SiteStats has not been populated yet
        if (totalPages == 0) {
            totalPages = pageRepository.count();
        }

        Map<Page, Double> pageScores = new HashMap<>();

        if (totalPages == 0) {
            log.warn("[RANKING] Ranking attempted but no pages are indexed yet");
            return pageScores;
        }

        for (Page page : validPages) {
            double totalScore = 0.0;
            List<Posting> pagePostings = postingsByPage.get(page);

            for (Posting posting : pagePostings) {
                double tf  = tfIdfCalculator.calculateTf(posting.getTermFrequency());
                double idf = tfIdfCalculator.calculateIdf(totalPages, posting.getTerm().getDocumentFrequency());
                totalScore += tfIdfCalculator.calculateScore(tf, idf);
            }

            pageScores.put(page, totalScore);
        }

        return pageScores;
    }

    /** Value type for callers that need a fully sorted, ranked result list. */
    public record RankedResult(Page page, double score) {}
}
