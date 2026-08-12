package com.jvmservicengine.search.processing.stopwords;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EnglishStopWordFilter implements StopWordFilter{

    // In production system, this will be loaded from a config file
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "from", "how", "i", "if", "in", "into", "is", "it", "no",
            "not", "of", "on", "or", "such", "that", "the", "their", "then",
            "there", "these", "they", "this", "to", "was", "will", "with"
    );

    @Override
    public boolean iStopWord(String word) {
        if(word == null) {
            return true;
        }

        return STOP_WORDS.contains(word);
    }
}
