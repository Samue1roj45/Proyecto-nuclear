package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.CaseStudy;
import com.psicosocial.simulador.model.StudentGroup;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.model.UserRole;
import com.psicosocial.simulador.repository.CaseStudyRepository;
import com.psicosocial.simulador.repository.StudentGroupRepository;
import com.psicosocial.simulador.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CaseStudyRepository caseStudyRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-CO"));

    @Transactional(readOnly = true)
    public List<GroupDto> listGroups() {
        return groupRepository.findAllWithMembersAndCases().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDto getGroup(Long id) {
        return toDto(groupRepository.findByIdWithMembersAndCases(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado")));
    }

    @Transactional
    public GroupDto createGroup(CreateGroupRequest req) {
        String name = req.getName().trim();
        if (groupRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Ya existe un grupo con ese nombre");
        }

        StudentGroup group = StudentGroup.builder()
                .name(name)
                .description(req.getDescription() != null ? req.getDescription().trim() : "")
                .members(resolveStudents(req.getStudentIds()))
                .assignedCases(resolveCases(req.getCaseIds()))
                .createdAt(LocalDateTime.now())
                .build();
        groupRepository.save(group);
        return toDto(group);
    }

    @Transactional
    public GroupDto updateGroup(Long id, UpdateGroupRequest req) {
        StudentGroup group = findGroup(id);

        if (req.getName() != null && !req.getName().isBlank()) {
            String name = req.getName().trim();
            if (groupRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new RuntimeException("Ya existe un grupo con ese nombre");
            }
            group.setName(name);
        }
        if (req.getDescription() != null) {
            group.setDescription(req.getDescription().trim());
        }
        if (req.getStudentIds() != null) {
            group.setMembers(resolveStudents(req.getStudentIds()));
        }
        if (req.getCaseIds() != null) {
            group.setAssignedCases(resolveCases(req.getCaseIds()));
        }

        groupRepository.save(group);
        return toDto(group);
    }

    @Transactional
    public MessageResponse deleteGroup(Long id) {
        groupRepository.delete(findGroup(id));
        return MessageResponse.builder().message("Grupo eliminado").build();
    }

    private StudentGroup findGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
    }

    private Set<User> resolveStudents(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<User> members = new HashSet<>();
        for (Long studentId : studentIds.stream().distinct().toList()) {
            User user = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + studentId));
            if (user.getRole() != UserRole.STUDENT) {
                throw new RuntimeException("Solo se pueden agregar estudiantes al grupo");
            }
            members.add(user);
        }
        return members;
    }

    private Set<CaseStudy> resolveCases(List<Long> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<CaseStudy> cases = new HashSet<>();
        for (Long caseId : caseIds.stream().distinct().toList()) {
            cases.add(caseStudyRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("Caso no encontrado: " + caseId)));
        }
        return cases;
    }

    private GroupDto toDto(StudentGroup group) {
        List<GroupMemberDto> members = group.getMembers().stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> GroupMemberDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        List<Long> caseIds = group.getAssignedCases() != null
                ? group.getAssignedCases().stream().map(CaseStudy::getId).sorted().toList()
                : List.of();

        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(members.size())
                .members(members)
                .assignedCaseIds(caseIds)
                .createdAt(group.getCreatedAt().format(FORMATTER))
                .build();
    }
}
