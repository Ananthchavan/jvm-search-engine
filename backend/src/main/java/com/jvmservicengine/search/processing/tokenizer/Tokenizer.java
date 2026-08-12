package com.jvmservicengine.search.processing.tokenizer;

import java.util.List;

public interface Tokenizer {

    List<String> tokenize(String normalizedText);
}
