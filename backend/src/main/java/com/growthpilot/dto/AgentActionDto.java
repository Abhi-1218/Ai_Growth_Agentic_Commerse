package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentActionDto {
    private Long id;
    private Long businessId;
    private String userEmail;
    private String goal;
    private String reasoningSummary;
    private String toolUsed;
    private String parameters;
    private String result;
    private String status; // PENDING_APPROVAL, APPROVED, REJECTED, EXECUTED, FAILED
    private LocalDateTime createdAt;
}
