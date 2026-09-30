package com.growthpilot.controller;

import com.growthpilot.dto.CartRecoveryActionRequest;
import com.growthpilot.dto.CartRecoveryDto;
import com.growthpilot.entity.AgentAction;
import com.growthpilot.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Cart Abandonment Recovery", description = "Detection of abandoned carts, recovery probability scoring, incentive calculation, and AI recovery dispatch")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/abandoned")
    @Operation(summary = "Get paginated list of abandoned carts with recovery probabilities and values")
    public ResponseEntity<Page<CartRecoveryDto>> getAbandonedCarts(Pageable pageable) {
        return ResponseEntity.ok(cartService.getAbandonedCarts(pageable));
    }

    @GetMapping("/abandoned/list")
    @Operation(summary = "Get all abandoned carts list")
    public ResponseEntity<List<CartRecoveryDto>> getAbandonedCartsList() {
        return ResponseEntity.ok(cartService.getAbandonedCartsList());
    }

    @GetMapping("/{id}/recovery-plan")
    @Operation(summary = "Get detailed recovery plan and AI generated message for a specific abandoned cart")
    public ResponseEntity<CartRecoveryDto> getCartRecoveryPlan(@PathVariable Long id) {
        return ResponseEntity.ok(cartService.getCartRecoveryPlan(id));
    }

    @PostMapping("/{id}/recover")
    @Operation(summary = "Trigger or propose an AI recovery action for an abandoned cart")
    public ResponseEntity<AgentAction> triggerRecovery(
            @PathVariable Long id,
            @RequestBody CartRecoveryActionRequest request) {
        return ResponseEntity.ok(cartService.triggerRecoveryAction(id, request));
    }
}
