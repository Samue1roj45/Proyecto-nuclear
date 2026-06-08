package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.Attempt;
import com.psicosocial.simulador.model.AttemptStatus;
import com.psicosocial.simulador.model.CaseStudy;
import com.psicosocial.simulador.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByUserOrderByStartedAtDesc(User user);
    List<Attempt> findAllByOrderByStartedAtDesc();
    List<Attempt> findByCaseStudy(CaseStudy caseStudy);
    List<Attempt> findByUserAndCaseStudyOrderByAttemptNumberDesc(User user, CaseStudy caseStudy);
    List<Attempt> findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(User user, CaseStudy caseStudy);
    List<Attempt> findByUserAndArchivedFalseOrderByStartedAtDesc(User user);
    Optional<Attempt> findByUserAndCaseStudyAndStatus(User user, CaseStudy caseStudy, AttemptStatus status);
    long countByCaseStudy(CaseStudy caseStudy);
    long countByStatus(AttemptStatus status);
    long countByUserAndStatus(User user, AttemptStatus status);

    @Query("SELECT AVG(a.totalScore) FROM Attempt a WHERE a.status IN ('PASSED', 'FAILED', 'BLOCKED')")
    Double averageTotalScore();

    @Query("SELECT AVG(a.clinicalScore) FROM Attempt a WHERE a.status IN ('PASSED', 'FAILED', 'BLOCKED')")
    Double averageClinicalScore();

    @Query("SELECT AVG(a.ethicalScore) FROM Attempt a WHERE a.status IN ('PASSED', 'FAILED', 'BLOCKED')")
    Double averageEthicalScore();

    @Query("SELECT AVG(a.normativeScore) FROM Attempt a WHERE a.status IN ('PASSED', 'FAILED', 'BLOCKED')")
    Double averageNormativeScore();

    @Query("SELECT COUNT(DISTINCT a.caseStudy.id) FROM Attempt a")
    long countDistinctCasesAttempted();

    @Query("SELECT AVG(a.totalScore) FROM Attempt a WHERE a.user = :user AND a.status <> 'IN_PROGRESS'")
    Double averageScoreByUser(com.psicosocial.simulador.model.User user);
}
