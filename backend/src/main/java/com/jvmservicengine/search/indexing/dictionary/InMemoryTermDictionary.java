package com.jvmservicengine.search.indexing.dictionary;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryTermDictionary implements Dictionary{

    // ensures thread-safe reads and writes
    private final ConcurrentHashMap<String, Long> termToMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> idToTermMap = new ConcurrentHashMap<>();

    // ensures thread-safe ID generation
    private final AtomicLong idSequence = new AtomicLong();

    @Override
    public Long getOrAddTerm(String term) {
        // this ensures that no two threads can create duplicate IDs for the same word
        return termToMap.computeIfAbsent(term, k -> {
            Long newId = idSequence.getAndIncrement();
            idToTermMap.put(newId, k);
            return newId;
        });
    }

    @Override
    public String getTerm(Long termId) {
        return idToTermMap.get(termId);
    }

    @Override
    public Long getTermId(String term) {
        return termToMap.get(term);
    }

    @Override
    public int getSize() {
        return termToMap.size();
    }

    @Override
    public Iterable<Long> getAllTermIds() {
        return termToIdMap.keySet();
    }
}
