package com.growthpilot.controller;

import com.growthpilot.dto.StorefrontEventRequest;
import com.growthpilot.entity.Product;
import com.growthpilot.repository.ProductRepository;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.security.SecurityUtils;
import com.growthpilot.service.StorefrontEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/store")
public class StorefrontController {
    private final ProductRepository products;
    private final SecurityUtils securityUtils;
    private final StorefrontEventService eventService;
    private final BusinessRepository businesses;
    public StorefrontController(ProductRepository products, SecurityUtils securityUtils, StorefrontEventService eventService,
                                BusinessRepository businesses) {
        this.products = products; this.securityUtils = securityUtils; this.eventService = eventService; this.businesses = businesses;
    }
    @GetMapping("/products")
    public List<Product> products(@RequestParam(required = false) String q, @RequestParam(required = false) String category) {
        Long businessId = currentBusinessId();
        if (q != null && !q.isBlank())
            return products.findByBusinessIdAndNameContainingIgnoreCaseOrBusinessIdAndDescriptionContainingIgnoreCase(businessId, q.trim(), businessId, q.trim());
        if (category != null && !category.isBlank()) return products.findByBusinessIdAndCategory(businessId, category.trim());
        return products.findAllByBusinessId(businessId);
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) {
        return products(q, null);
    }

    @GetMapping("/category/{category}")
    public List<Product> category(@PathVariable String category) {
        return products(null, category);
    }
    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        return products.findByIdAndBusinessId(id, currentBusinessId())
                .orElseThrow(() -> new com.growthpilot.exception.ResourceNotFoundException("Product not found"));
    }
    @PostMapping("/events")
    public ResponseEntity<Void> event(@Valid @RequestBody StorefrontEventRequest request) {
        eventService.record(request); return ResponseEntity.accepted().build();
    }

    private Long currentBusinessId() {
        if (securityUtils.getCurrentCustomerOptional().isPresent()) {
            return securityUtils.getCurrentCustomer().getBusiness().getId();
        }
        if (securityUtils.getCurrentUserOptional().isPresent()) {
            return securityUtils.getCurrentUser().getBusiness().getId();
        }
        return businesses.findAll().stream().findFirst()
                .orElseThrow(() -> new com.growthpilot.exception.ResourceNotFoundException("No storefront business configured"))
                .getId();
    }
}
