package com.growthpilot.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerCartDto(Long cartId, String status, List<Item> items, BigDecimal total) {
    public record Item(Long productId, String name, BigDecimal price, int quantity, BigDecimal subtotal) {}
}
