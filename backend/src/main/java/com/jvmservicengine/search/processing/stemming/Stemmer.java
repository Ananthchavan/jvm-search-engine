package com.jvmservicengine.search.processing.stemming;

import java.util.List;

public interface Stemmer {

    List<String> stem(List<String> tokens);
}
