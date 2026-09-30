package com.growthpilot.controller;

import com.growthpilot.dto.AgentActionDto;
import com.growthpilot.dto.GrowthOpportunityDto;
import com.growthpilot.dto.OpportunityExecuteRequest;
import com.growthpilot.service.GrowthOpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "AI Growth Opportunities", description = "Autonomous detection and ranking of growth opportunities across cart recovery, intent conversion, churn prevention, and product bundling")
public class GrowthOpportunityController {

    private final GrowthOpportunityService opportunityService;

    public GrowthOpportunityController(GrowthOpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    @Operation(summary = "Get list of active AI-identified growth opportunities with estimated revenue impact")
    public ResponseEntity<List<GrowthOpportunityDto>> getOpportunities() {
        return ResponseEntity.ok(opportunityService.getOpportunitiesDto());
    }

    @PostMapping("/generate")
    @Operation(summary = "Trigger fresh scanning and re-generation of growth opportunities")
    public ResponseEntity<List<GrowthOpportunityDto>> generateOpportunities() {
        return ResponseEntity.ok(opportunityService.generateOpportunitiesDto());
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute an opportunity via the AI Agent (proposes or executes growth action)")
    public ResponseEntity<AgentActionDto> executeOpportunity(
            @PathVariable Long id,
            @RequestBody(required = false) OpportunityExecuteRequest request) {
        return ResponseEntity.ok(opportunityService.executeOpportunity(id, request));
    }
}
