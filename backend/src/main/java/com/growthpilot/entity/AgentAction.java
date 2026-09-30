package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 1000)
    private String goal;

    @Column(name = "reasoning_summary", length = 2000)
    private String reasoningSummary;

    @Column(name = "tool_used")
    private String toolUsed;

    @Column(name = "parameters", length = 2000)
    private String parameters;

    @Column(length = 2000)
    private String result;

    /** PENDING_APPROVAL, APPROVED, REJECTED, EXECUTED, FAILED */
    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
