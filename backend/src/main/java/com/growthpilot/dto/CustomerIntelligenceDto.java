package com.growthpilot.dto;

import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Order;
import com.growthpilot.entity.Recommendation;
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
public class CustomerIntelligenceDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String segment;
    private String preferredCategory;
    private int totalOrders;
    private BigDecimal totalSpend;
    private BigDecimal averageOrderValue;
    private LocalDateTime lastOrderDate;
    private Long daysSinceLastOrder;

    // Intent Intelligence
    private int purchaseIntentScore;
    private String purchaseIntentStatus; // High Intent, Moderate Intent, Low Intent
    private List<IntentFactorDto> intentFactors;

    // Churn Intelligence
    private String churnRisk; // LOW, MEDIUM, HIGH
    private String churnReason;
    private String suggestedRetentionAction;

    // Engagement
    private int engagementScore;

    // Recommendations & History
    private List<RecommendationDto> recommendations;
    private List<CustomerOrderSummaryDto> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productCategory;
        private BigDecimal productPrice;
        private BigDecimal score;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderSummaryDto {
        private Long id;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime orderDate;
        private int itemCount;
    }
}
