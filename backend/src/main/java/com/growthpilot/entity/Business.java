package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "razorpay_key_id")
    private String razorpayKeyId;

    @Column(name = "razorpay_key_secret")
    private String razorpayKeySecret;

    @Column(name = "razorpay_webhook_secret")
    private String razorpayWebhookSecret;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_status")
    private String syncStatus;
}
