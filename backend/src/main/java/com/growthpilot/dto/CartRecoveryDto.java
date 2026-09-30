package com.growthpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRecoveryDto {
    private Long cartId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerSegment;
    private String churnRisk;
    private int purchaseIntentScore;
    private List<CartItemDto> items;
    private BigDecimal totalValue;
    private int recoveryProbability; // 0 - 100%
    private String recommendedIncentive; // e.g. "10% Discount + Free Shipping"
    private String suggestedMessage;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long productId;
        private String productName;
        private String category;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
