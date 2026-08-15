package com.jvmservicengine.search.ranking;

import com.jvmservicengine.search.indexing.tfidf.TfIdfCalculator;
import com.jvmservicengine.search.processing.service.TextProcessingService;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.entity.Term;
import com.jvmservicengine.search.storage.repository.PostingRepository;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import com.jvmservicengine.search.storage.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RankingService {

    private final TextProcessingService textProcessingService;
    private final SiteStatsRepository siteStatsRepository;
    private final TermRepository termRepository;
    private final PostingRepository postingRepository;
    private final TfIdfCalculator tfIdfCalculator;

    public List<RankedResult> search(String rawQuery) {

        Map<String, Integer> queryTerms = textProcessingService.process(rawQuery);
        if(queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        long totalPages = siteStatsRepository.findTopByOrderByIdDesc()
                .map(SiteStats::getIndexedPages)
                .orElse(0L);

        if(totalPages == 0) {
            log.warn("Search attempted but database is empty. No pages indexed");
            return Collections.emptyList();
        }

        Map<Page, Double> pageScores = new HashMap<>();

        for(String word : queryTerms.keySet()) {

            Optional<Term> termOpt = termRepository.findByTerm(word);
            if(termOpt.isEmpty()) {
                continue;
            }
            Term term = termOpt.get();

            double idf = tfIdfCalculator.calculateIdf(totalPages, term.getDocumentFrequency());

            List<Posting> postings = postingRepository.findByTerm(term);

            for(Posting posting : postings) {
                double tf = tfIdfCalculator.calculateTf(posting.getTermFrequency());
                double score = tfIdfCalculator.calculateScore(tf, idf);

                Page page = posting.getPage();

                pageScores.put(page, pageScores.getOrDefault(page, 0.0) + score);
            }
        }

        return pageScores.entrySet().stream()
                .sorted(Map.Entry.<Page, Double>comparingByValue().reversed())
                .map(entry -> new RankedResult(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public record RankedResult(Page page, double score) {}
}
