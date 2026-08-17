package com.jvmservicengine.search.analytics.searchhistory;

import com.jvmservicengine.search.storage.entity.SearchHistory;
import com.jvmservicengine.search.storage.repository.SearchHistoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    @Transactional(readOnly = true)
    public Page<SearchHistory> getRecentSearches(int page, int size) {
        int safeSize = Math.min(size, 100);

        PageRequest pageRequest = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "searchedAt"));
        return searchHistoryRepository.findAll(pageRequest);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSearch(String query, int resultCount, long latencyMs, String ipAddress, String userAgent) {
        SearchHistory history = new SearchHistory();
        history.setQuery(query);
        history.setResultCount(resultCount);
        history.setLatencyMs((int) latencyMs);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);

        searchHistoryRepository.save(history);
    }
}
