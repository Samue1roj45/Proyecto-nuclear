package com.psicosocial.simulador.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String category;

    private String level;

    @Column(length = 1000)
    private String imageUrl;

    private String contextQuote;

    private int estimatedMinutes;

    private double complexityStars;

    @Builder.Default
    private boolean timerEnabled = false;

    @ElementCollection
    @CollectionTable(name = "case_competencies", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "competency")
    @Builder.Default
    private List<String> competencies = new ArrayList<>();

    @OneToMany(mappedBy = "caseStudy", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();
}
