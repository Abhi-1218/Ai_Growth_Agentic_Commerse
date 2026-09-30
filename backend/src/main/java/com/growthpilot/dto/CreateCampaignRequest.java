package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    private String name;
    private String targetSegment;
    private String offerDetails;
    private String generatedMessage;
    private BigDecimal estimatedImpact;
    private String status; // DRAFT or ACTIVE or PENDING_APPROVAL
}
