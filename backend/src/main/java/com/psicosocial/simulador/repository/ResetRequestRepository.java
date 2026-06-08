package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.CaseStudy;
import com.psicosocial.simulador.model.ResetRequest;
import com.psicosocial.simulador.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResetRequestRepository extends JpaRepository<ResetRequest, Long> {
    List<ResetRequest> findByApprovedFalse();
    Optional<ResetRequest> findByUserAndCaseStudyAndApprovedFalse(User user, CaseStudy caseStudy);
}
