package com.growthpilot.dto;

import com.growthpilot.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    // Primary KPI Cards
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalCustomers;
    private long totalProducts;
    private BigDecimal averageOrderValue;
    private long activeCustomers;
    private long repeatCustomers;
    private long abandonedCarts;
    private BigDecimal recoverableCartRevenue;
    private BigDecimal revenueAtRisk;
    private long highIntentCustomers;
    private long growthOpportunities;
    private long pendingApprovals;
    private double overallConversionRate;
    private double cartRecoveryRate;
    
    // Razorpay Integration Metrics
    private long failedPayments;
    private long successfulPayments;
    private long refunds;
    private BigDecimal totalRefundAmount;

    // Charts & Breakdowns
    private List<TrendDataPoint> revenueTrend;
    private List<TrendDataPoint> ordersTrend;
    private List<SegmentDistributionDto> customerSegments;
    private List<ProductSummaryDto> topProducts;
    private List<FunnelStageDto> conversionFunnel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummaryDto {
        private Long id;
        private String name;
        private String category;
        private BigDecimal price;
        private int totalSales;
        private BigDecimal totalRevenue;
        private int views;
        private int cartAdditions;
        private double conversionRate;
    }
}
