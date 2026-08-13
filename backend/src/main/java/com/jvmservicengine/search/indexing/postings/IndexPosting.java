package com.jvmservicengine.search.indexing.postings;


import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * this represents the occurence of a specific term inside a specific document
 */
public class IndexPosting {

    private final Long documentId;
    private int frequency;
    private final List<Integer> positions;

    public IndexPosting(Long documentId) {
        this.documentId = documentId;
        this.frequency = 0;
        this.positions = new ArrayList<>();
    }

    public void addPosition(int position) {
        this.positions.add(position);
        this.frequency++;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public int getFrequency() {
        return frequency;
    }

    public List<Integer> getPositions() {
        return positions;
    }

}
