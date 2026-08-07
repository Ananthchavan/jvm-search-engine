package com.jvmservicengine.search.storage.repository;

import com.jvmservicengine.search.storage.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByTerm(String term);
}
