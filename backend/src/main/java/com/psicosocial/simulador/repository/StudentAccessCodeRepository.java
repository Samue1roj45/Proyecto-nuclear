package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.AccessCodeStatus;
import com.psicosocial.simulador.model.StudentAccessCode;
import com.psicosocial.simulador.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAccessCodeRepository extends JpaRepository<StudentAccessCode, Long> {
    Optional<StudentAccessCode> findFirstByStudentAndEstadoOrderByFechaGeneracionDesc(User student, AccessCodeStatus estado);

    Optional<StudentAccessCode> findBySessionToken(String sessionToken);

    Optional<StudentAccessCode> findByCodigoAndUtilizadoFalse(String codigo);

    Optional<StudentAccessCode> findFirstByStudentAndEstadoInAndUtilizadoFalseOrderByFechaGeneracionDesc(
            User student, List<AccessCodeStatus> estados);

    boolean existsByCodigo(String codigo);

    List<StudentAccessCode> findByEstadoOrderByFechaGeneracionDesc(AccessCodeStatus estado);

    List<StudentAccessCode> findAllByOrderByFechaGeneracionDesc();

    boolean existsByStudentAndEstado(User student, AccessCodeStatus estado);
}
