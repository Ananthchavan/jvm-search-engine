package com.jvmservicengine.search.searches.query;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryParser {

    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern EXCLUSION_PATTERN = Pattern.compile("-(\\w+)");

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
            List<String> processedPhrase = processText(phrase);
            if(!processedPhrase.isEmpty()) {
                parsedQuery.getExactPhrases().add(processedPhrase);
            }

            remainingQuery = remainingQuery.replace(phraseMatcher.group(0), "");
        }

        // Extraction of excluded terms
        Matcher exclusionMatcher = EXCLUSION_PATTERN.matcher(remainingQuery);
        while(exclusionMatcher.find()) {
            String excludedTerm = exclusionMatcher.group(1);
            List<String> processedExclusion = processText(excludedTerm);
            if(!processedExclusion.isEmpty()) {
                parsedQuery.getExcludedTerms().add(processedExclusion.get(0));
            }
            remainingQuery = remainingQuery.replace(exclusionMatcher.group(0), "");
        }

        // Extraction of standard Terms(left over terms)
        List<String> standardTerms = processText(remainingQuery);
        parsedQuery.getStandardTerms().addAll(standardTerms);

        return parsedQuery;
    }

    private List<String> processText(String text) {
        List<String> result = new ArrayList<>();
        if(text == null || text.isBlank()) {
            return result;
        }

        String words[] = text.toLowerCase().split("\\W+");
        for(String word : words) {
            if(!word.isBlank()) {
                result.add(word);
            }
        }

        return result;
    }
}
