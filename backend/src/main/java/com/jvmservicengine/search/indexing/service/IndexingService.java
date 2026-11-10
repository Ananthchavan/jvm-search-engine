package com.jvmservicengine.search.indexing.service;

import com.jvmservicengine.search.analytics.statistics.SiteStatsService;
import com.jvmservicengine.search.indexing.dictionary.Dictionary;
import com.jvmservicengine.search.indexing.dictionary.Dictionary.DictionarySnapshot;
import com.jvmservicengine.search.indexing.invertedindex.InMemoryInvertedIndex;
import com.jvmservicengine.search.indexing.postings.IndexPosting;
import com.jvmservicengine.search.indexing.postings.PostingList;
import com.jvmservicengine.search.processing.service.TextProcessingService;
import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.Term;
import com.jvmservicengine.search.storage.repository.PageRepository;
import com.jvmservicengine.search.storage.repository.PostingRepository;
import com.jvmservicengine.search.storage.repository.TermRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final TextProcessingService textProcessingService;

    /** Guard against concurrent flushes (scheduler + manual POST can overlap). */
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    @PostConstruct
    public void reindexOrphanedPages() {
        List<Page> orphanedPages = pageRepository.findPagesWithNoPostings();
        if (orphanedPages.isEmpty()) {
            log.info("[STARTUP] No orphaned pages found — all pages have postings.");
            return;
        }

        log.info("[STARTUP] Found {} orphaned page(s) with no postings — re-indexing into memory.",
                orphanedPages.size());

        for (Page page : orphanedPages) {
            String bodyText = page.getContentHash();
            if (bodyText == null || bodyText.isBlank()) {
                continue;
            }
            List<String> tokens = textProcessingService.processToTokens(bodyText);
            if (!tokens.isEmpty()) {
                inMemoryIndex.addDocument(page.getId(), tokens);
                log.debug("[STARTUP] Re-indexed {} tokens for page id={} url={}",
                        tokens.size(), page.getId(), page.getUrl());
            }
        }

        log.info("[STARTUP] Finished re-indexing {} orphaned page(s).", orphanedPages.size());
    }

    /**
    Atomically drains the live in-memory index and dictionary into a snapshot,
    then flushes that snapshot to PostgreSQL.  New postings arriving during
    the flush go into fresh in-memory structures and are not affected.
     */
    public boolean flushToDatabase() {
        if (!flushing.compareAndSet(false, true)) {
            log.warn("Flush already in progress — skipping this invocation.");
            return false;
        }

        DictionarySnapshot dictSnapshot = dictionary.swapAndDrain();
        ConcurrentHashMap<Long, PostingList> indexSnapshot = inMemoryIndex.swapAndDrain();

        int snapshotTermCount = dictSnapshot.termToIdMap().size();
        int snapshotPostingLists = indexSnapshot.size();

        if (snapshotPostingLists == 0) {
            log.info("In-memory index was empty — nothing to flush.");
            flushing.set(false);
            return true;
        }

        log.info("Starting swap-and-drain flush: {} terms, {} posting lists.",
                snapshotTermCount, snapshotPostingLists);

        try {
            persistSnapshot(dictSnapshot, indexSnapshot);
            siteStatsService.recalculateStats();
            log.info("Flush completed successfully. Site statistics recalculated.");
        } catch (Exception ex) {
            log.error("Flush FAILED — merging snapshot back into live index.", ex);
            inMemoryIndex.mergeFrom(indexSnapshot);
            dictionary.mergeFrom(dictSnapshot);
            log.info("Snapshot merged back — no data was lost.");
        } finally {
            flushing.set(false);
        }

        return true;
    }

    /**
    Persist a drained snapshot to PostgreSQL in a single transaction.
     */
    @Transactional
    protected void persistSnapshot(DictionarySnapshot dictSnapshot,
                                   ConcurrentHashMap<Long, PostingList> indexSnapshot) {

        List<Posting> dbPostingsToSave = new ArrayList<>();

        // Iterate only the terms present in the drained snapshot
        for (var entry : dictSnapshot.idToTermMap().entrySet()) {
            Long termId = entry.getKey();
            String word = entry.getValue();

            PostingList postingList = indexSnapshot.get(termId);
            if (postingList == null) {
                continue; // term was in dictionary but had no postings
            }

            Term termEntity = termRepository.findByTerm(word)
                    .orElseGet(() -> {
                        Term newTerm = new Term();
                        newTerm.setTerm(word);
                        return termRepository.save(newTerm);
                    });

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
                    dbPostingsToSave.add(dbPosting);
                } else {
                    dbPosting = new Posting();
                    dbPosting.setTerm(termEntity);
                    dbPosting.setPage(pageRepository.getReferenceById(pageId));
                    dbPosting.setTermFrequency(memoryPosting.getFrequency());
                    dbPosting.setPositions(memoryPosting.getPositions().toString());

                    dbPostingsToSave.add(dbPosting);
                    newlyDiscoveredPagesCount++;
                }
            }

            if (newlyDiscoveredPagesCount > 0) {
                termEntity.setDocumentFrequency(
                        termEntity.getDocumentFrequency() + newlyDiscoveredPagesCount);
                termRepository.save(termEntity);
            }
        }

        postingRepository.saveAll(dbPostingsToSave);
        log.info("Persisted {} postings to database.", dbPostingsToSave.size());
    }
}
