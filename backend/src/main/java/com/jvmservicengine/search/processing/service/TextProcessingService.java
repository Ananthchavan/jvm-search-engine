package com.jvmservicengine.search.processing.service;

import com.jvmservicengine.search.processing.frequency.FrequencyCalculator;
import com.jvmservicengine.search.processing.normalization.TextNormalizer;
import com.jvmservicengine.search.processing.stemming.Stemmer;
import com.jvmservicengine.search.processing.tokenizer.Tokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TextProcessingService {

    private final TextNormalizer textNormalizer;
    private final Tokenizer tokenizer;
    private final Stemmer stemmer;
    private final FrequencyCalculator frequencyCalculator;

    public Map<String, Integer> process(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("Received empty text for processing");
            return Map.of();
        }

        // pipeline
        String cleanText = textNormalizer.normalize(rawText);
        List<String> tokens = tokenizer.tokenize(cleanText);
        List<String> stemmedTokens = stemmer.stem(tokens);
        Map<String, Integer> wordFrequencies = frequencyCalculator.calculateFrequencies(stemmedTokens);

        log.debug("Processed text down to {} unique searchable terms.", wordFrequencies.size());

        return wordFrequencies;
    }
}
