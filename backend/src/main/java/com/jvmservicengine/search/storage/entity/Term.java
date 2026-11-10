package com.jvmservicengine.search.storage.entity;

import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "term_seq")
    @SequenceGenerator(name = "term_seq", sequenceName = "term_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String term;

    @Column(name = "document_frequency", nullable = false)
    private Integer documentFrequency = 0;

    @Column(name = "total_frequency", nullable = false)
    private Integer totalFrequency = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
