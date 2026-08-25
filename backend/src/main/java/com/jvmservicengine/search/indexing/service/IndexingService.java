package com.jvmservicengine.search.indexing.service;

import com.jvmservicengine.search.analytics.statistics.SiteStatsService;
import com.jvmservicengine.search.indexing.dictionary.Dictionary;
import com.jvmservicengine.search.indexing.invertedindex.InMemoryInvertedIndex;
import com.jvmservicengine.search.indexing.postings.IndexPosting;
import com.jvmservicengine.search.indexing.postings.PostingList;
import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.Term;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.PostingRepository;
import com.jvmservicengine.search.storage.repository.TermRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final InMemoryInvertedIndex inMemoryIndex;
    private final Dictionary dictionary;
    private final TermRepository termRepository;
    private final PostingRepository postingRepository;
    private final PageRepository pageRepository;
    private final SiteStatsService siteStatsService;

    @Transactional
    public void flushToDatabase() {
        log.info("Starting batch flush of Inverted Index to PostgreSQL");

        List<Posting> dbPostingToSave = new ArrayList<>();

        for(Long termId : dictionary.getAllTermIds()) {

            String word = dictionary.getTerm(termId);
            Term termEntity = termRepository.findByTerm(word)
                    .orElseGet(() -> {
                        Term newTerm = new Term();
                        newTerm.setTerm(word);
                        return termRepository.save(newTerm);
                    });

            PostingList postingList = inMemoryIndex.search(word);
            if(postingList != null) {
                List<Posting> existingPostings = postingRepository.findByTerm(termEntity);
                Map<Long, Posting> existingPostingsMap = existingPostings.stream()
                        .collect(Collectors.toMap(p -> p.getPage().getId(), p -> p));

                int newlyDiscoveredPagesCount = 0;

                for (IndexPosting memoryPosting : postingList.getPostings()) {
                    Long pageId = memoryPosting.getDocumentId();
                    Posting dbPosting = existingPostingsMap.get(pageId);

                    if (dbPosting != null) {
                        dbPosting.setTermFrequency(memoryPosting.getFrequency());
                        dbPosting.setPositions(memoryPosting.getPositions().toString());
                        dbPostingToSave.add(dbPosting);
                    } else {
                        dbPosting = new Posting();
                        dbPosting.setTerm(termEntity);
                        dbPosting.setPage(pageRepository.getReferenceById(pageId));
                        dbPosting.setTermFrequency(memoryPosting.getFrequency());
                        dbPosting.setPositions(memoryPosting.getPositions().toString());

                        dbPostingToSave.add(dbPosting);
                        newlyDiscoveredPagesCount++;
                    }
                }

                if (newlyDiscoveredPagesCount > 0) {
                    termEntity.setDocumentFrequency(termEntity.getDocumentFrequency() + newlyDiscoveredPagesCount);
                    termRepository.save(termEntity);
                }
            }
        }

        postingRepository.saveAll(dbPostingToSave);
        log.info("Successfully flushed {} postings to database", dbPostingToSave.size());

        inMemoryIndex.clearMemory();

        siteStatsService.recalculateStats();
        log.info("Site statistics recalculated");
    }
}