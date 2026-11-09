package com.jvmservicengine.search.processing.stemming;

import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Stemmer backed by Lucene's Porter stemmer algorithm.
 * Produces linguistically accurate stems:
 *   running/run  → run
 *   images/image → imag (standard Porter)
 *   string       → string  (not "str")
 *   spring       → spring  (not "spr")
 */
@Component
public class PorterEnglishStemmer implements Stemmer {

    @Override
    public List<String> stem(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            result.add(stemWord(token));
        }
        return result;
    }

    private String stemWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }

        try (StringReader reader = new StringReader(word);
             KeywordTokenizer keywordTokenizer = new KeywordTokenizer()) {

            keywordTokenizer.setReader(reader);
            TokenStream stream = new PorterStemFilter(keywordTokenizer);
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);

            stream.reset();
            if (stream.incrementToken()) {
                String stemmed = attr.toString();
                stream.end();
                return stemmed;
            }
            stream.end();
        } catch (IOException e) {
            // Fallback to the original word if stemming fails
            return word;
        }
        return word;
    }
}

