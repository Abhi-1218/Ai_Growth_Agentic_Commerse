package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "growth_opportunities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String title;

    /** RECOVER_CART, PREVENT_CHURN, UPSELL, CROSS_SELL, REACTIVATION, HIGH_INTENT */
    @Column(nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_customer_id")
    private Customer targetCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

    @Column(name = "estimated_impact")
    private BigDecimal estimatedImpact;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "recommended_action", length = 1000)
    private String recommendedAction;

    @Column(length = 1000)
    private String reason;

    /** PENDING, ACTIONED, DISMISSED */
    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
