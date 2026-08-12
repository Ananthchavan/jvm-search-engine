package com.jvmservicengine.search.processing.normalization;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class StandardTextNormalizer implements TextNormalizer{

    // matches anything that is not a lowercase letter, number or whitespaces
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[^a-z0-9\\s]");

    // matches one or more whitespaces
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @Override
    public String normalize(String rawText) {
        if(rawText == null || rawText.isBlank()) {
            return "";
        }

        // converts to lowercase letters
        String normalized = rawText.toLowerCase();

        // removes accents/diacritics (`e to e)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");

        // remove punctuations
        normalized = PUNCTUATION_PATTERN.matcher(normalized).replaceAll(" ");

        // normalizes white spaces
        normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();

        return normalized;
    }
}
