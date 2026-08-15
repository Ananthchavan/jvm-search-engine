package com.jvmservicengine.search.indexing.tfidf;

import org.springframework.stereotype.Component;

@Component
public class TfIdfCalculator {

    public double calculateTf(int termFrequency) {
        if(termFrequency <= 0) {
            return 0.0;
        }
        return 1.0 + Math.log10(termFrequency);
    }

    public double calculateIdf(long totalPages, int documentFrequency) {
        if(documentFrequency <= 0 || totalPages <= 0) {
            return 0.0;
        }

        double ratio = (double) totalPages / documentFrequency;

        if(ratio < 1.0) {
            return 0.0;
        }

        return Math.log10(ratio);
    }

    public double calculateScore(double tf, double idf) {
        return tf * idf;
    }
}
