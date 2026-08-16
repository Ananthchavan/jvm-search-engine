package com.jvmservicengine.search.processing.frequency;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StandardFrequencyCalculator implements FrequencyCalculator{

    @Override
    public Map<String, Integer> calculateFrequencies(List<String> tokens) {
        if(tokens == null || tokens.isEmpty()) {
            return  Map.of();
        }

        return tokens.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.summingInt(e -> 1)
                ));
    }
}
