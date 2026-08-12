package com.jvmservicengine.search.processing.stemming;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SimpleEnglishStemmer implements Stemmer{


    @Override
    public List<String> stem(List<String> tokens) {
        if(tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        return tokens.stream()
                .map(this::stemWord)
                .collect(Collectors.toList());
    }

    private String stemWord(String word) {

        if(word == null || word.length() <= 3) {
            return word;
        }

        String stemmed = word;

        // handles plurals, 3rd person singulars
        if (stemmed.endsWith("ies") && stemmed.length() > 4) {
            return stemmed.substring(0, stemmed.length() - 3) + "y"; // e.g., "ponies" -> "pony"
        }
        if (stemmed.endsWith("es") && stemmed.length() > 4) {
            stemmed = stemmed.substring(0, stemmed.length() - 2); // e.g., "boxes" -> "box"
        } else if (stemmed.endsWith("s") && !stemmed.endsWith("ss") && stemmed.length() > 3) {
            stemmed = stemmed.substring(0, stemmed.length() - 1); // e.g., "cats" -> "cat" (but not "boss")
        }

        // handles verbal(--ed) forms and gerunds(--ing)
        if (stemmed.endsWith("ing") && stemmed.length() > 5) {
            return stemmed.substring(0, stemmed.length() - 3); // e.g., "walking" -> "walk"
        }
        if (stemmed.endsWith("ed") && stemmed.length() > 4) {
            return stemmed.substring(0, stemmed.length() - 2); // e.g., "walked" -> "walk"
        }

        // handles adverbs(--ly)
        if (stemmed.endsWith("ly") && stemmed.length() > 4) {
            return stemmed.substring(0, stemmed.length() - 2); // e.g., "quickly" -> "quick"
        }

        return stemmed;
    }
}
