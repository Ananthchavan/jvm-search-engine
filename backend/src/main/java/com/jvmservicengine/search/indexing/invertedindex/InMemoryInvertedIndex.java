package com.jvmservicengine.search.indexing.invertedindex;

import com.jvmservicengine.search.indexing.dictionary.Dictionary;
import com.jvmservicengine.search.indexing.postings.PostingList;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryInvertedIndex {

    private final Dictionary dictionary;

    private final ConcurrentHashMap<Long, PostingList> index;

    public InMemoryInvertedIndex(Dictionary dictionary) {
        this.dictionary = dictionary;
        this.index = new ConcurrentHashMap<>();
    }

    /**
     * This function wires document to the index
     */
    public void addDocument(Long documentId, List<String> processedTokens) {

        for(int i = 0 ; i < processedTokens.size() ; i++) {
            String term = processedTokens.get(i);

            Long termId = dictionary.getOrAddTerm(term);

            PostingList postingList = index.computeIfAbsent(termId, PostingList::new);

            postingList.addOccurrence(documentId, i);
        }
    }

    public PostingList search(String term) {
        Long termId = dictionary.getTermId(term);
        if(termId == null) {
            return null;
        }
        return index.get(termId);
    }

    public int getuniqueTermCount() {
        return index.size();
    }

    public void clearMemory() {
        index.clear();
    }
}
