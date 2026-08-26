package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.storage.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop10ByOrderBySearchedAtDesc();

    @Query("SELECT COALESCE(AVG(s.latencyMs), 0.0) FROM SearchHistory s")
    Double getAverageLatency();

    @Query("SELECT COALESCE(AVG(s.resultCount), 0.0) FROM SearchHistory s")
    Double getAverageResultCount();
}
