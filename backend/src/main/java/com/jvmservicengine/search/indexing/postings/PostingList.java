package com.jvmservicengine.search.indexing.postings;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PostingList {

    private final Long termId;
    private final ConcurrentHashMap<Long, IndexPosting> postingsMap;


    public PostingList(Long termId, ConcurrentHashMap<Long, IndexPosting> postingMap) {
        this.termId = termId;
        this.postingsMap = new ConcurrentHashMap<>();
    }

    public void addOccurrence(Long documentId, int position) {
        IndexPosting posting = postingsMap.computeIfAbsent(documentId, IndexPosting::new);

        // Granular locking here, we only lock specific document's posting
        // this ensures thread safety
        synchronized (posting) {
            posting.addPosition(position);
        }
    }

    public Long getTermId() {
        return termId;
    }

    public Collection<IndexPosting> getPostings() {
        return postingsMap.values();
    }
}
