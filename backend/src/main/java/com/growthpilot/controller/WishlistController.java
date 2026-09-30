package com.growthpilot.controller;

import com.growthpilot.entity.Product;
import com.growthpilot.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer/wishlist")
public class WishlistController {
    private final WishlistService service;
    public WishlistController(WishlistService service) { this.service = service; }
    @GetMapping public List<Product> list() { return service.list(); }
    @PostMapping public List<Product> addBody(@RequestBody ProductId body) { return service.add(body.productId()); }
    @PostMapping("/{productId}") public List<Product> add(@PathVariable Long productId) { return service.add(productId); }
    @DeleteMapping("/{productId}") public ResponseEntity<List<Product>> remove(@PathVariable Long productId) {
        return ResponseEntity.ok(service.remove(productId));
    }
    public record ProductId(Long productId) {}
}
