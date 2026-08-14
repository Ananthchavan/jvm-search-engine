package com.jvmservicengine.search.indexing.dictionary;

public interface Dictionary {

    Long getOrAddTerm(String term);

    String getTerm(Long termId);

    Iterable<Long> getAllTermIds();

    Long getTermId(String term);

    int getSize();
}
