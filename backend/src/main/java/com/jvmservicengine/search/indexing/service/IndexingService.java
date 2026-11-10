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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    /** Must match the column definition on the terms table (length = 255). */
    private static final int MAX_TERM_LENGTH = 255;

    /** Number of terms processed per transaction chunk. */
    private static final int CHUNK_SIZE = 1000;

    private final InMemoryInvertedIndex inMemoryIndex;
    private final Dictionary dictionary;
    private final TermRepository termRepository;
    private final PostingRepository postingRepository;
    private final PageRepository pageRepository;
    private final SiteStatsService siteStatsService;
    private final TextProcessingService textProcessingService;

    /** Guard against concurrent flushes (scheduler + manual POST can overlap). */
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    // Startup recovery

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

    // Swap-and-drain flush


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

    // Bulk, chunked persistence
    protected void persistSnapshot(DictionarySnapshot dictSnapshot,
                                   ConcurrentHashMap<Long, PostingList> indexSnapshot) {

        // Collect valid (word → PostingList) pairs, skip oversized terms
        List<Map.Entry<String, PostingList>> validEntries = new ArrayList<>();

        for (var entry : dictSnapshot.idToTermMap().entrySet()) {
            String word = entry.getValue();

            if (word.length() > MAX_TERM_LENGTH) {
                log.warn("Skipping term that exceeds DB column limit (length={}): {}…",
                        word.length(), word.substring(0, Math.min(word.length(), 20)));
                continue;
            }

            PostingList postingList = indexSnapshot.get(entry.getKey());
            if (postingList == null) {
                continue; // term had no postings
            }

            validEntries.add(Map.entry(word, postingList));
        }

        // Process in CHUNK_SIZE-term chunks
        int savedTerms = 0;
        int skippedTerms = 0;
        int totalChunks = (int) Math.ceil((double) validEntries.size() / CHUNK_SIZE);
        log.info("Persisting {} valid terms in {} chunk(s) of up to {}.",
                validEntries.size(), totalChunks, CHUNK_SIZE);

        for (int i = 0; i < validEntries.size(); i += CHUNK_SIZE) {
            List<Map.Entry<String, PostingList>> chunk =
                    validEntries.subList(i, Math.min(i + CHUNK_SIZE, validEntries.size()));
            try {
                int saved = persistChunk(chunk);
                savedTerms += saved;
            } catch (Exception ex) {
                log.error("Chunk {}/{} failed — skipping {} terms.",
                        (i / CHUNK_SIZE) + 1, totalChunks, chunk.size(), ex);
                skippedTerms += chunk.size();
            }
        }

        log.info("Flush complete: {} terms persisted, {} skipped.", savedTerms, skippedTerms);
    }

    @Transactional
    protected int persistChunk(List<Map.Entry<String, PostingList>> chunk) {

        List<String> words = chunk.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Term> existingTermsByWord = termRepository.findByTermIn(words).stream()
                .collect(Collectors.toMap(Term::getTerm, t -> t));

        List<Term> newTerms = new ArrayList<>();
        for (String word : words) {
            if (!existingTermsByWord.containsKey(word)) {
                Term t = new Term();
                t.setTerm(word);
                newTerms.add(t);
            }
        }
        if (!newTerms.isEmpty()) {
            termRepository.saveAll(newTerms);
            newTerms.forEach(t -> existingTermsByWord.put(t.getTerm(), t));
        }

        Collection<Term> termEntities = existingTermsByWord.values();
        Map<Long, Map<Long, Posting>> postingsByTermId = new HashMap<>();
        for (Posting p : postingRepository.findByTermIn(termEntities)) {
            postingsByTermId
                    .computeIfAbsent(p.getTerm().getId(), k -> new HashMap<>())
                    .put(p.getPage().getId(), p);
        }

        List<Posting> postingsToSave = new ArrayList<>();
        Map<Term, Integer> docFreqIncrements = new HashMap<>();

        for (Map.Entry<String, PostingList> entry : chunk) {
            String word = entry.getKey();
            PostingList postingList = entry.getValue();
            Term termEntity = existingTermsByWord.get(word);

            Map<Long, Posting> existingPostingsByPage =
                    postingsByTermId.getOrDefault(termEntity.getId(), Collections.emptyMap());

            int newPagesCount = 0;
            for (IndexPosting memPosting : postingList.getPostings()) {
                Long pageId = memPosting.getDocumentId();
                Posting dbPosting = existingPostingsByPage.get(pageId);

                if (dbPosting != null) {
                    dbPosting.setTermFrequency(memPosting.getFrequency());
                    dbPosting.setPositions(memPosting.getPositions().toString());
                    postingsToSave.add(dbPosting);
                } else {
                    dbPosting = new Posting();
                    dbPosting.setTerm(termEntity);
                    dbPosting.setPage(pageRepository.getReferenceById(pageId));
                    dbPosting.setTermFrequency(memPosting.getFrequency());
                    dbPosting.setPositions(memPosting.getPositions().toString());
                    postingsToSave.add(dbPosting);
                    newPagesCount++;
                }
            }

            if (newPagesCount > 0) {
                docFreqIncrements.merge(termEntity, newPagesCount, Integer::sum);
            }
        }

        postingRepository.saveAll(postingsToSave);

        if (!docFreqIncrements.isEmpty()) {
            docFreqIncrements.forEach((term, increment) ->
                    term.setDocumentFrequency(term.getDocumentFrequency() + increment));
            termRepository.saveAll(docFreqIncrements.keySet());
        }

        return chunk.size();
    }
}
