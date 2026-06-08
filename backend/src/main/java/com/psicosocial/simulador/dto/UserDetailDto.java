package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDetailDto {
    private UserDto user;
    private List<AttemptSummaryDto> attempts;
    private double averageClinical;
    private double averageEthical;
    private double averageNormative;
}
