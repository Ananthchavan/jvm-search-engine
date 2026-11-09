package com.jvmservicengine.search.searches.query;

import com.jvmservicengine.search.processing.service.TextProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class QueryParser {

    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    // Exclusion requires '-' at start-of-string or after whitespace, so hyphens
    // inside words like "spring-boot" are NOT treated as exclusions.
    private static final Pattern EXCLUSION_PATTERN = Pattern.compile("(?:^|(?<=\\s))-(\\w+)");

    private final TextProcessingService textProcessingService;

    public ParsedQuery parse(String rawQuery) {
        ParsedQuery parsedQuery = new ParsedQuery();
        if(rawQuery == null || rawQuery.isEmpty()) {
            return parsedQuery;
        }

        parsedQuery.setOriginalQuery(rawQuery.trim());
        String remainingQuery = rawQuery;

        // Extraction of exact phrases
        Matcher phraseMatcher = PHRASE_PATTERN.matcher(remainingQuery);
        while(phraseMatcher.find()) {
            String phrase = phraseMatcher.group(1);
            List<String> processedPhrase = textProcessingService.processToTokens(phrase);
            if(!processedPhrase.isEmpty()) {
                parsedQuery.getExactPhrases().add(processedPhrase);
            }

            remainingQuery = remainingQuery.replace(phraseMatcher.group(0), "");
        }

        // Extraction of excluded terms
        Matcher exclusionMatcher = EXCLUSION_PATTERN.matcher(remainingQuery);
        while(exclusionMatcher.find()) {
            String excludedTerm = exclusionMatcher.group(1);
            List<String> processedExclusion = textProcessingService.processToTokens(excludedTerm);
            if(!processedExclusion.isEmpty()) {
                parsedQuery.getExcludedTerms().add(processedExclusion.get(0));
            }
            remainingQuery = remainingQuery.replace(exclusionMatcher.group(0), "");
        }

        // Extraction of standard Terms(left over terms)
        List<String> standardTerms = textProcessingService.processToTokens(remainingQuery);
        parsedQuery.getStandardTerms().addAll(standardTerms);

        return parsedQuery;
    }
}
