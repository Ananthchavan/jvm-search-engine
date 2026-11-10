package com.jvmservicengine.search.indexing.dictionary;

import java.util.concurrent.ConcurrentHashMap;

public interface Dictionary {

    Long getOrAddTerm(String term);

    String getTerm(Long termId);

    Iterable<Long> getAllTermIds();

    Long getTermId(String term);

    int getSize();

    /**
    Atomically replaces the internal maps with fresh empty ones and returns the
     */
    DictionarySnapshot swapAndDrain();

    /**
    Merges a previously drained snapshot back into the live dictionary.
     */
    void mergeFrom(DictionarySnapshot snapshot);

    /**
    An immutable snapshot of the dictionary state at the time of drain.
     */
    record DictionarySnapshot(
            ConcurrentHashMap<String, Long> termToIdMap,
            ConcurrentHashMap<Long, String> idToTermMap
    ) {}
}
