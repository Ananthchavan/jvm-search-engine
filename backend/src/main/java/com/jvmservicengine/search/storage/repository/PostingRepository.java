package com.jvmservicengine.search.storage.repository;


import com.jvmservicengine.search.storage.entity.Posting;
import com.jvmservicengine.search.storage.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostingRepository extends JpaRepository<Posting, Long> {

    List<Posting> findByTerm(Term term);

    List<Posting> findByTerm_TermIn(Collection<String> terms);
}
