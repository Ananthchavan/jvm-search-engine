package com.jvmservicengine.search.processing.tokenizer;

import com.jvmservicengine.search.processing.stopwords.StopWordFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WhitespaceTokenizer implements Tokenizer{

    private final StopWordFilter stopWordFilter;

    @Override
    public List<String> tokenize(String normalizedText) {
        if(normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizedText.split(" "))
                .filter(word -> !word.isBlank())
                .filter(word -> !stopWordFilter.iStopWord(word))
                .collect(Collectors.toList());
    }
}
