package com.growthpilot.controller;

import com.growthpilot.dto.CheckoutDtos;
import com.growthpilot.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;
    public CheckoutController(CheckoutService checkoutService) { this.checkoutService = checkoutService; }

    @PostMapping("/orders")
    public ResponseEntity<CheckoutDtos.OrderResponse> createOrder(@Valid @RequestBody CheckoutDtos.CreateOrderRequest request) {
        return ResponseEntity.ok(checkoutService.createOrder(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<CheckoutDtos.OrderResponse> verify(@Valid @RequestBody CheckoutDtos.VerifyPaymentRequest request) {
        return ResponseEntity.ok(checkoutService.verifyPayment(request));
    }

    @PostMapping("/{orderId}/failure")
    public ResponseEntity<Void> failure(@PathVariable Long orderId, @RequestParam(required = false) String reason,
                                        @RequestParam(required = false) String sessionId) {
        checkoutService.markPaymentFailed(orderId, reason == null ? "Payment was cancelled or declined" : reason, sessionId);
        return ResponseEntity.noContent().build();
    }
}
