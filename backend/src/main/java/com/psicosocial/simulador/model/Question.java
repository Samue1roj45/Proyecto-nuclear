package com.psicosocial.simulador.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseStudy caseStudy;

    @Column(nullable = false, length = 1000)
    private String text;

    private int orderIndex;

    @Column(length = 1000)
    private String sceneImageUrl;

    @Column(length = 200)
    private String sceneTitle;

    @Column(length = 300)
    private String sceneSubtitle;

    @Column(length = 500)
    private String sceneHint;

    @Column(length = 100)
    private String npcLabel;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<AnswerOption> options = new ArrayList<>();
}
