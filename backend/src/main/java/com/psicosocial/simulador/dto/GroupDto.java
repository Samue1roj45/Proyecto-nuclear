package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private int memberCount;
    private List<GroupMemberDto> members;
    private List<Long> assignedCaseIds;
    private String createdAt;
}
