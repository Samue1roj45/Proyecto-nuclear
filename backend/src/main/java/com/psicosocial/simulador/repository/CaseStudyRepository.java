package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.CaseStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseStudyRepository extends JpaRepository<CaseStudy, Long> {
    List<CaseStudy> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String title, String category);
}
