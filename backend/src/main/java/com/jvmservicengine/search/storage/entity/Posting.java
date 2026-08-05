package com.jvmservicengine.search.storage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "postings",
            uniqueConstraints = {
                // A specific word should have only one posting per-page
                @UniqueConstraint(columnNames = {"page_id", "term_id"})
            })
@Getter
@Setter
@NoArgsConstructor
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id" , nullable = false)
    private Term term;

    @Column(name = "term_frequency", nullable = false)
    private Integer termFrequency;

    @Column(columnDefinition = "TEXT")
    private String positions;

}
