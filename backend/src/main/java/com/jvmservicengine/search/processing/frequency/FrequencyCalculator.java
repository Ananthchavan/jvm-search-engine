package com.jvmservicengine.search.processing.frequency;

import java.util.List;
import java.util.Map;

public interface FrequencyCalculator {

    Map<String, Integer> calculateFrequencies(List<String> tokens);
}
