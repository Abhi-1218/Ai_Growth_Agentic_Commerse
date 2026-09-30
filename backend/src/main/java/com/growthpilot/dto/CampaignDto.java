package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDto {
    private Long id;
    private String name;
    private String targetSegment;
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, COMPLETED, REJECTED
    private String offerDetails;
    private String generatedMessage;
    private BigDecimal estimatedImpact;
    private Boolean createdByAgent;
    private LocalDateTime createdAt;
}
