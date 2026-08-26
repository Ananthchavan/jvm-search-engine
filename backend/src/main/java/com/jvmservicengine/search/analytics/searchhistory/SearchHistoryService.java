package com.jvmservicengine.search.analytics.searchhistory;

import com.jvmservicengine.search.storage.entity.SearchHistory;
import com.jvmservicengine.search.storage.repository.SearchHistoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    // Deduplication guard: tracks the last time each query was logged.
    // Prevents React 18 StrictMode's double-invoke from creating duplicate entries.
    private final ConcurrentHashMap<String, Long> recentQueryTimestamps = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 2000;

    @Transactional(readOnly = true)
    public Page<SearchHistory> getRecentSearches(int page, int size) {
        int safeSize = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "searchedAt"));
        return searchHistoryRepository.findAll(pageRequest);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSearch(String query, int resultCount, long latencyMs, String ipAddress, String userAgent) {
        long now = System.currentTimeMillis();
        String dedupKey = query.toLowerCase().trim() + "|" + ipAddress;

        Long lastLogged = recentQueryTimestamps.get(dedupKey);
        if (lastLogged != null && (now - lastLogged) < DEDUP_WINDOW_MS) {
            log.debug("Duplicate search suppressed within {}ms window: '{}'", DEDUP_WINDOW_MS, query);
            return;
        }
        recentQueryTimestamps.put(dedupKey, now);

        // Evict stale entries to prevent unbounded map growth
        recentQueryTimestamps.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_WINDOW_MS * 10);

        SearchHistory history = new SearchHistory();
        history.setQuery(query);
        history.setResultCount(resultCount);
        history.setLatencyMs((int) latencyMs);
        history.setIpAddress(ipAddress);
        history.setUserAgent(userAgent);

        searchHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSearchMetrics() {
        long totalSearches = searchHistoryRepository.count();
        double avgLatency = searchHistoryRepository.getAverageLatency();
        double avgResults = searchHistoryRepository.getAverageResultCount();

        return Map.of(
                "totalSearches", totalSearches,
                "averageLatency", Math.round(avgLatency),
                "averageResults", Math.round(avgResults)
        );
    }
}
