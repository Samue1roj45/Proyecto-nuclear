package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
