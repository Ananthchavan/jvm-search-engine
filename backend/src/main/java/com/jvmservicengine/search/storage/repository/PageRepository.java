package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.storage.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    boolean existsByUrl(String url);

    Optional<Page> findByUrl(String url);

    @Query("SELECT p.url FROM Page p")
    List<String> findAllUrls();
}
