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
public class ProductIntelligenceDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private int stock;
    private int totalSales;
    private BigDecimal totalRevenue;
    private int views;
    private int cartAdditions;
    private double conversionRate; // (sales / views) * 100
    private double cartConversionRate; // (sales / cartAdditions) * 100
    private String performanceBadge; // TRENDING, HIGH_CONVERTING, LOW_PERFORMING, STABLE
    private String aiInsight;
    private List<RelatedProductDto> crossSellCandidates;
    private List<RelatedProductDto> bundleOpportunities;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedProductDto {
        private Long id;
        private String name;
        private String category;
        private BigDecimal price;
        private double affinityScore;
        private String reason;
    }
}
