package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "uk_customers_customer_id", columnNames = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "customer_id", unique = true)
    private String customerId;

    @Column(name = "password_hash")
    @JsonIgnore
    private String passwordHash;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_spend")
    private BigDecimal totalSpend;

    @Column(name = "average_order_value")
    private BigDecimal averageOrderValue;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Column(name = "preferred_category")
    private String preferredCategory;

    @Column(name = "engagement_score")
    private Integer engagementScore;

    @Column(name = "purchase_intent_score")
    private Integer purchaseIntentScore;

    @Column(name = "churn_risk")
    private String churnRisk;

    @Column(name = "segment")
    private String segment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "razorpay_customer_id")
    private String razorpayCustomerId;
}
