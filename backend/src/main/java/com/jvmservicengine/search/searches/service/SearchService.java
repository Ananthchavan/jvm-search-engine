package com.jvmservicengine.search.searches.service;

import com.jvmservicengine.search.indexing.tfidf.TfIdfCalculator;
import com.jvmservicengine.search.searches.dto.SearchResponse;
import com.jvmservicengine.search.searches.dto.SearchResultItem;
import com.jvmservicengine.search.searches.query.ParsedQuery;
import com.jvmservicengine.search.searches.query.QueryParser;
import com.jvmservicengine.search.searches.snippet.SnippetGenerator;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.SiteStats;
import com.jvmservicengine.search.storage.repository.PostingRepository;
import com.jvmservicengine.search.storage.repository.SiteStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final QueryParser queryParser;
    private final SnippetGenerator snippetGenerator;
    private final PostingRepository postingRepository;

    private final TfIdfCalculator tfIdfCalculator;
    private final SiteStatsRepository siteStatsRepository;

    private static final int PAGE_SIZE = 10;

    public SearchResponse search(String rawQuery, int pageNumber) {
        long startTime = System.currentTimeMillis();

        // parse the Query
        ParsedQuery query = queryParser.parse(rawQuery);

        Set<String> allTermsToFetch = new HashSet<>(query.getStandardTerms());
        query.getExactPhrases().forEach(allTermsToFetch::addAll);

        if (allTermsToFetch.isEmpty()) {
            return emptyResponse(rawQuery, startTime);
        }

        // fetch Postings from DB
        List<Posting> postings = postingRepository.findByTerm_TermIn(allTermsToFetch);

        // group Postings by Page
        Map<Page, List<Posting>> postingsByPage = postings.stream()
                .collect(Collectors.groupingBy(Posting::getPage));

        // filter Pages (Handle Boolean NOT Exclusions)
        List<Page> validPages = filterPages(postingsByPage, query);

        // rank Pages using Phase 7 TF-IDF (TODO IMPLEMENTED ✅)
        Map<Page, Double> rankedPages = calculateTfIdfScores(validPages, postingsByPage);

        // sort by Score Descending
        List<Map.Entry<Page, Double>> sortedResults = rankedPages.entrySet().stream()
                .sorted(Map.Entry.<Page, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // pagination
        int totalResults = sortedResults.size();
        int startIndex = (pageNumber - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalResults);

        List<SearchResultItem> resultItems = new ArrayList<>();
        if (startIndex < totalResults) {
            List<Map.Entry<Page, Double>> paginatedEntries = sortedResults.subList(startIndex, endIndex);

            // generate Snippets and Build DTOs
            for (Map.Entry<Page, Double> entry : paginatedEntries) {
                Page page = entry.getKey();
                Double score = entry.getValue();

                String snippet = snippetGenerator.generateSnippet(page.getContentPreview(), query);

                resultItems.add(SearchResultItem.builder()
                        .title(page.getTitle())
                        .url(page.getUrl())
                        .snippet(snippet)
                        .relevanceScore(score)
                        .build());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;

        return SearchResponse.builder()
                .originalQuery(rawQuery)
                .totalResults(totalResults)
                .currentPage(pageNumber)
                .totalPages((int) Math.ceil((double) totalResults / PAGE_SIZE))
                .executionTimeMs(executionTime)
                .results(resultItems)
                .build();
    }

    private Map<Page, Double> calculateTfIdfScores(List<Page> validPages, Map<Page, List<Posting>> postingsByPage) {
        // Fetch total indexed pages to calculate Inverse Document Frequency (IDF)
        long totalPages = siteStatsRepository.findTopByOrderByIdDesc()
                .map(SiteStats::getIndexedPages)
                .orElse(0L);

        Map<Page, Double> pageScores = new HashMap<>();

        if (totalPages == 0) {
            return pageScores;
        }

        // calculate scores for every valid page
        for (Page page : validPages) {
            double totalScore = 0.0;
            List<Posting> pagePostings = postingsByPage.get(page);

            for (Posting posting : pagePostings) {
                double tf = tfIdfCalculator.calculateTf(posting.getTermFrequency());
                double idf = tfIdfCalculator.calculateIdf(totalPages, posting.getTerm().getDocumentFrequency());
                totalScore += tfIdfCalculator.calculateScore(tf, idf);
            }

            pageScores.put(page, totalScore);
        }

        return pageScores;
    }

    private List<Page> filterPages(Map<Page, List<Posting>> postingsByPage, ParsedQuery query) {
        List<Page> validPages = new ArrayList<>();

        for (Map.Entry<Page, List<Posting>> entry : postingsByPage.entrySet()) {
            Page page = entry.getKey();

            boolean containsExcluded = query.getExcludedTerms().stream()
                    .anyMatch(term -> page.getContentPreview().toLowerCase().contains(term));

            if (containsExcluded) continue;

            validPages.add(page);
        }
        return validPages;
    }

    private SearchResponse emptyResponse(String query, long startTime) {
        return SearchResponse.builder()
                .originalQuery(query)
                .totalResults(0)
                .currentPage(1)
                .totalPages(0)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .results(Collections.emptyList())
                .build();
    }
}