package com.growthpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StorefrontEventRequest(
        @NotBlank String eventType,
        Long customerId,
        Long productId,
        String query,
        @Positive Long orderId,
        String sessionId) {}
