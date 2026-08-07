package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.storage.entity.SiteStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteStatsRepository extends JpaRepository<SiteStats, Long> {

    Optional<SiteStats> findTopByOrderByIdDesc();
}
