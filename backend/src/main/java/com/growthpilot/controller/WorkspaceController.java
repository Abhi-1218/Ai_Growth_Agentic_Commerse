package com.growthpilot.controller;

import com.growthpilot.dto.BusinessProfileDto;
import com.growthpilot.entity.Business;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace")
@Tag(name = "Workspace Profile", description = "Endpoints for managing store workspace profile and settings")
public class WorkspaceController {

    private final BusinessRepository businessRepository;
    private final SecurityUtils securityUtils;

    private final com.growthpilot.service.DataSeedService dataSeedService;

    public WorkspaceController(BusinessRepository businessRepository, SecurityUtils securityUtils, com.growthpilot.service.DataSeedService dataSeedService) {
        this.businessRepository = businessRepository;
        this.securityUtils = securityUtils;
        this.dataSeedService = dataSeedService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get workspace details for current user")
    public ResponseEntity<BusinessProfileDto> getProfile() {
        Business business = securityUtils.getCurrentUser().getBusiness();
        if (business == null) {
            throw new ResourceNotFoundException("No workspace found for current user");
        }
        return ResponseEntity.ok(BusinessProfileDto.builder()
                .id(business.getId())
                .name(business.getName())
                .description(business.getDescription())
                .createdAt(business.getCreatedAt())
                .build());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update workspace profile details")
    public ResponseEntity<BusinessProfileDto> updateProfile(@RequestBody BusinessProfileDto dto) {
        Business business = securityUtils.getCurrentUser().getBusiness();
        if (business == null) {
            throw new ResourceNotFoundException("No workspace found for current user");
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            business.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            business.setDescription(dto.getDescription());
        }
        business = businessRepository.save(business);
        return ResponseEntity.ok(BusinessProfileDto.builder()
                .id(business.getId())
                .name(business.getName())
                .description(business.getDescription())
                .createdAt(business.getCreatedAt())
                .build());
    }

    @PostMapping("/seed-demo")
    @Operation(summary = "Populate current workspace with demo catalog, customers, and opportunities for testing")
    public ResponseEntity<java.util.Map<String, Object>> seedDemoData() {
        var user = securityUtils.getCurrentUser();
        Business business = user.getBusiness();
        if (business == null) {
            throw new ResourceNotFoundException("No workspace found for current user");
        }
        dataSeedService.seedBusinessData(business, user);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Successfully generated demo catalog, customers, abandoned carts, and opportunities for " + business.getName()
        ));
    }
}
