package com.jvmservicengine.search.searches.query;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParsedQuery {

    private List<String> standardTerms = new ArrayList<>();

    private List<List<String>> exactPhrases = new ArrayList<>();

    private List<String> excludedTerms = new ArrayList<>();

    private String originalQuery;
}
