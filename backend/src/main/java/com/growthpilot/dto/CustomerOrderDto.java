package com.growthpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderDto(Long id, String status, BigDecimal totalAmount, String currency,
                               String razorpayOrderId, LocalDateTime orderDate, List<Item> items) {
    public record Item(Long productId, String name, int quantity, BigDecimal price) {}
}
