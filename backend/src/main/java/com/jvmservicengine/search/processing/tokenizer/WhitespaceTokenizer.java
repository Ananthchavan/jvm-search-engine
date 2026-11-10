package com.jvmservicengine.search.processing.tokenizer;

import com.jvmservicengine.search.processing.stopwords.StopWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhitespaceTokenizer implements Tokenizer {

    /** Tokens longer than this are almost certainly noise (base64, hashes, etc.) */
    private static final int MAX_TOKEN_LENGTH = 64;

    private final StopWordFilter stopWordFilter;

    @Override
    public List<String> tokenize(String normalizedText) {
        if(normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizedText.split(" "))
                .filter(word -> !word.isBlank())
                .filter(word -> {
                    if (word.length() > MAX_TOKEN_LENGTH) {
                        log.debug("Dropping oversized token (length={}): {}…", word.length(),
                                word.substring(0, Math.min(word.length(), 20)));
                        return false;
                    }
                    return true;
                })
                .filter(word -> !stopWordFilter.iStopWord(word))
                .collect(Collectors.toList());
    }
}
