package com.growthpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String category;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "total_sales")
    private Integer totalSales;

    @Column(name = "views")
    private Integer views;

    @Column(name = "cart_additions")
    private Integer cartAdditions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @JsonIgnore
    private Business business;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
