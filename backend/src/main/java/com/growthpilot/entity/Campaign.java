package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_segment")
    private String targetSegment;

    /** DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, COMPLETED, REJECTED */
    @Column(nullable = false)
    private String status;

    @Column(name = "offer_details", length = 2000)
    private String offerDetails;

    @Column(name = "generated_message", length = 2000)
    private String generatedMessage;

    @Column(name = "estimated_impact")
    private BigDecimal estimatedImpact;

    @Column(name = "created_by_agent")
    private Boolean createdByAgent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
