package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttemptDetailDto {
    private AttemptSummaryDto summary;
    private List<AttemptItemDto> items;
}
