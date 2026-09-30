package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentFactorDto {
    private String signalName;
    private int scoreContribution;
    private int maxScore;
    private String status; // POSITIVE, NEUTRAL, NEGATIVE
    private String explanation;
}
