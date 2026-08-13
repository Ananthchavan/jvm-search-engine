package com.jvmservicengine.search.indexing.dictionary;

public interface Dictionary {

    Long getOrAddTerm(String term);

    String getTerm(Long termId);

    Long getTermId(String term);

    int getSize();
}
