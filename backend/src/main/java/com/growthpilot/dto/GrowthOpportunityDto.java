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
public class GrowthOpportunityDto {
    private Long id;
    private String title;
    private String type; // RECOVER_CART, HIGH_INTENT, PREVENT_CHURN, UPSELL, BUNDLE
    private Long targetCustomerId;
    private String targetCustomerName;
    private Long targetProductId;
    private String targetProductName;
    private BigDecimal estimatedImpact;
    private Integer confidenceScore; // 0 - 100%
    private String recommendedAction;
    private String reason;
    private String status; // PENDING, APPROVED, EXECUTED, DISMISSED
    private LocalDateTime createdAt;
}
