package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRecoveryActionRequest {
    private String offerType; // DISCOUNT_10, DISCOUNT_15, FREE_SHIPPING, CUSTOM
    private Integer discountPercent;
    private String message;
    private boolean executeDirectly; // If true, executes without pending approval
}
