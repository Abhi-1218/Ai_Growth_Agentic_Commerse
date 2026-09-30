package com.growthpilot.controller;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "AI Recommendations", description = "Hybrid recommendation engine providing personalized product suggestions with score and explanation")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get top AI product recommendations for a customer")
    public ResponseEntity<List<CustomerIntelligenceDto.RecommendationDto>> getRecommendations(@PathVariable Long customerId) {
        return ResponseEntity.ok(recommendationService.getRecommendationsForCustomer(customerId));
    }

    @PostMapping("/generate/{customerId}")
    @Operation(summary = "Force re-generation of AI product recommendations for a customer")
    public ResponseEntity<List<CustomerIntelligenceDto.RecommendationDto>> regenerateRecommendations(@PathVariable Long customerId) {
        return ResponseEntity.ok(recommendationService.generateAndSaveRecommendations(customerId));
    }
}
