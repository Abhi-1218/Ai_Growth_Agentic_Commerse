package com.growthpilot.controller;

import com.growthpilot.dto.ProductIntelligenceDto;
import com.growthpilot.entity.Product;
import com.growthpilot.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Intelligence", description = "Product catalog, sales conversion analytics, bundle opportunities, and inventory insights")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get all products with full intelligence, performance badges, and conversion rates")
    public ResponseEntity<Page<ProductIntelligenceDto>> getAllProducts(
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProductsIntelligence(category, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/{id}/intelligence")
    @Operation(summary = "Get detailed product intelligence, conversion analytics, and bundle pairings")
    public ResponseEntity<ProductIntelligenceDto> getProductIntelligence(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductIntelligence(id));
    }

    @GetMapping("/top")
    @Operation(summary = "Get top-selling products")
    public ResponseEntity<List<Product>> getTopProducts(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(productService.getTopProducts(limit));
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
