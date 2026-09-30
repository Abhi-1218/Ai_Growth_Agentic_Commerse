package com.growthpilot.controller;

import com.growthpilot.dto.CampaignDto;
import com.growthpilot.dto.CreateCampaignRequest;
import com.growthpilot.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns & Offers", description = "Targeted segment campaigns, promotional offers, and approval workflows")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(summary = "Get paginated list of campaigns with status and impact")
    public ResponseEntity<Page<CampaignDto>> getCampaigns(Pageable pageable) {
        return ResponseEntity.ok(campaignService.getCampaigns(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID")
    public ResponseEntity<CampaignDto> getCampaignById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new growth campaign")
    public ResponseEntity<CampaignDto> createCampaign(@RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update campaign status (DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, COMPLETED, REJECTED)")
    public ResponseEntity<CampaignDto> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(campaignService.updateStatus(id, status));
    }
}
