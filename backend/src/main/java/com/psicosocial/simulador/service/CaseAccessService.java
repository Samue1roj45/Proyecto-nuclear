package com.psicosocial.simulador.service;

import com.psicosocial.simulador.model.CaseStudy;
import com.psicosocial.simulador.model.StudentGroup;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.model.UserRole;
import com.psicosocial.simulador.repository.StudentGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseAccessService {

    private final StudentGroupRepository groupRepository;

    public void ensureCaseVisible(CaseStudy caseStudy, User user) {
        if (user.getRole() == UserRole.ADMIN) return;
        if (!isCaseVisibleToStudent(caseStudy, user)) {
            throw new RuntimeException("Este caso no está asignado a tu grupo. Contacta a tu docente.");
        }
    }

    public boolean isCaseVisibleToStudent(CaseStudy caseStudy, User student) {
        if (student.getRole() == UserRole.ADMIN) return true;
        List<StudentGroup> groupsWithCase = groupRepository.findAllByAssignedCaseId(caseStudy.getId());
        if (groupsWithCase.isEmpty()) return true;
        List<StudentGroup> studentGroups = groupRepository.findAllByMemberId(student.getId());
        return studentGroups.stream()
                .anyMatch(sg -> groupsWithCase.stream().anyMatch(g -> g.getId().equals(sg.getId())));
    }
}
