package com.jvmservicengine.search.analytics.service;

import com.jvmservicengine.search.storage.entity.SearchHistory;
import com.jvmservicengine.search.storage.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    @Async
    public void logSearch(String query, int resultCount, long latencyMs) {
        SearchHistory history = new SearchHistory();
        history.setQuery(query);
        history.setResultCount(resultCount);
        history.setLatencyMs((int) latencyMs);

        // hardcoded for now. will change in a prod app
        history.setIpAddress("127.0.0.1");
        history.setUserAgent("React Engine UI");
    }
}
