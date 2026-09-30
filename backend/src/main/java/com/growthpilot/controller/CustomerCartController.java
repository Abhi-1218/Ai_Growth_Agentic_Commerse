package com.growthpilot.controller;

import com.growthpilot.dto.CustomerCartDto;
import com.growthpilot.service.CartService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/cart")
public class CustomerCartController {
    private final CartService service;
    public CustomerCartController(CartService service) { this.service = service; }
    @GetMapping public CustomerCartDto get(@RequestParam(required = false) String sessionId) { return service.getCustomerCart(sessionId); }
    @PutMapping("/{productId}")
    public CustomerCartDto set(@PathVariable Long productId,
                               @RequestParam(required = false) Integer quantity,
                               @Valid @RequestBody(required = false) Quantity body,
                               @RequestParam(required = false) String sessionId) {
        int requested = quantity != null ? quantity : body == null ? 0 : body.quantity();
        return service.setCustomerItem(productId, requested, sessionId);
    }
    @DeleteMapping("/{productId}") public CustomerCartDto remove(@PathVariable Long productId,
                                                                   @RequestParam(required = false) String sessionId) {
        return service.removeCustomerItem(productId, sessionId);
    }
    public record Quantity(@Min(1) @Max(99) int quantity) {}
}
