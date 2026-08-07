package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.storage.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    Optional<Page> findByUrl(String url);

    boolean existsByUrl(String url);
}
