package com.growthpilot.service;

import com.growthpilot.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChurnRiskServiceTest {

    private ChurnRiskService churnRiskService;

    @BeforeEach
    void setUp() {
        churnRiskService = new ChurnRiskService();
    }

    @Test
    void testCalculateChurnRisk_LowRisk() {
        Customer customer = Customer.builder()
                .id(1L)
                .lastOrderDate(LocalDateTime.now().minusDays(10))
                .engagementScore(80)
                .preferredCategory("Electronics")
                .segment("VIP")
                .build();

        ChurnRiskService.ChurnEvaluationResult result = churnRiskService.evaluateCustomer(customer);
        assertEquals("LOW", result.getChurnRisk());
        assertNotNull(result.getRetentionAction());
    }

    @Test
    void testCalculateChurnRisk_HighRisk() {
        Customer customer = Customer.builder()
                .id(2L)
                .lastOrderDate(LocalDateTime.now().minusDays(95))
                .engagementScore(20)
                .preferredCategory("Beauty")
                .segment("Dormant")
                .build();

        ChurnRiskService.ChurnEvaluationResult result = churnRiskService.evaluateCustomer(customer);
        assertEquals("HIGH", result.getChurnRisk());
        assertTrue(result.getChurnReason().contains("inactive"));
        assertTrue(result.getRetentionAction().contains("win-back") || result.getRetentionAction().contains("discount"));
    }
}
