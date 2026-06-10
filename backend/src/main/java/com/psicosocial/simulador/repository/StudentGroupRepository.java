package com.psicosocial.simulador.repository;

import com.psicosocial.simulador.model.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    @Query("SELECT DISTINCT g FROM StudentGroup g LEFT JOIN FETCH g.members ORDER BY g.name ASC")
    List<StudentGroup> findAllWithMembers();

    @Query("SELECT DISTINCT g FROM StudentGroup g LEFT JOIN FETCH g.members WHERE g.id = :id")
    Optional<StudentGroup> findByIdWithMembers(Long id);

    List<StudentGroup> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT DISTINCT g FROM StudentGroup g JOIN g.members m WHERE m.id = :userId")
    List<StudentGroup> findAllByMemberId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT g FROM StudentGroup g LEFT JOIN FETCH g.members LEFT JOIN FETCH g.assignedCases WHERE g.id = :id")
    Optional<StudentGroup> findByIdWithMembersAndCases(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM StudentGroup g LEFT JOIN FETCH g.members LEFT JOIN FETCH g.assignedCases ORDER BY g.name ASC")
    List<StudentGroup> findAllWithMembersAndCases();

    @Query("SELECT DISTINCT g FROM StudentGroup g JOIN g.assignedCases c WHERE c.id = :caseId")
    List<StudentGroup> findAllByAssignedCaseId(@Param("caseId") Long caseId);
}
