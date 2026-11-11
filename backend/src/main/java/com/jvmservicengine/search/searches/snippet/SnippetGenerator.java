package com.jvmservicengine.search.searches.snippet;

import com.jvmservicengine.search.searches.query.ParsedQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SnippetGenerator {

    private static final int SNIPPET_RADIUS_BEFORE = 60;
    private static final int SNIPPET_RADIUS_AFTER = 140;
    private static final int MAX_DEFAULT_LENGTH = 200;

    public String generateSnippet(String text, ParsedQuery query) {
        if (text == null || text.isBlank()) {
            return "";
        }

        List<String> termsToHighlight = new ArrayList<>(query.getStandardTerms());
        for (List<String> phrase : query.getExactPhrases()) {
            termsToHighlight.addAll(phrase);
        }

        if (termsToHighlight.isEmpty()) {
            return htmlEscape(truncateText(text, MAX_DEFAULT_LENGTH));
        }

        String lowerText = text.toLowerCase();
        int bestMatchIndex = -1;

        for (String term : termsToHighlight) {
            int index = lowerText.indexOf(term);
            if (index != -1) {
                if (bestMatchIndex == -1 || index < bestMatchIndex) {
                    bestMatchIndex = index;
                }
            }
        }

        if (bestMatchIndex == -1) {
            return htmlEscape(truncateText(text, MAX_DEFAULT_LENGTH));
        }

        int start = Math.max(0, bestMatchIndex - SNIPPET_RADIUS_BEFORE);
        int end = Math.min(text.length(), bestMatchIndex + SNIPPET_RADIUS_AFTER);

        String snippet = text.substring(start, end);

        if (start > 0) {
            snippet = "..." + snippet.substring(snippet.indexOf(' ') + 1);
        }

        if (end < text.length()) {
            int lastSpace = snippet.lastIndexOf(' ');
            if (lastSpace != -1) {
                snippet = snippet.substring(0, lastSpace);
            }
            snippet = snippet + "...";
        }
        snippet = htmlEscape(snippet);
        snippet = highlightTerms(snippet, termsToHighlight);

        return snippet;
    }

    static String htmlEscape(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public String escapeTitle(String title) {
        return htmlEscape(title);
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String highlightTerms(String snippet, List<String> terms) {
        String highlighted = snippet;
        for (String term : terms) {
            // Pattern.quote is safe here; we match against already-escaped plain text.
            highlighted = highlighted.replaceAll(
                    "(?i)(" + Pattern.quote(term) + ")",
                    "<b>$1</b>"
            );
        }
        return highlighted;
    }
}
