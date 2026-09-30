package com.growthpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CheckoutDtos {
    private CheckoutDtos() {}

    public record Item(@NotNull Long productId, @Min(1) int quantity) {}

    public record CreateOrderRequest(Long customerId, @NotEmpty List<@Valid Item> items, String sessionId) {
        public CreateOrderRequest(Long customerId, List<Item> items) { this(customerId, items, null); }
    }

    public record VerifyPaymentRequest(@NotNull Long orderId, @NotNull String razorpayOrderId,
                                       @NotNull String razorpayPaymentId, @NotNull String razorpaySignature,
                                       String sessionId) {
        public VerifyPaymentRequest(Long orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
            this(orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature, null);
        }
    }

    public record OrderResponse(Long orderId, String razorpayOrderId, BigDecimal amount, String currency,
                                 String keyId, String status) {}
}
