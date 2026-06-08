package com.psicosocial.simulador.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseStudy caseStudy;

    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    private double totalScore;

    private double clinicalScore;

    private double ethicalScore;

    private double normativeScore;

    private int currentQuestionIndex;

    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Builder.Default
    private boolean archived = false;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AttemptAnswer> answers = new ArrayList<>();
}
