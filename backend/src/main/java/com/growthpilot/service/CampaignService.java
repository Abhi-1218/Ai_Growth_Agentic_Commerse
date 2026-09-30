package com.growthpilot.service;

import com.growthpilot.dto.CampaignDto;
import com.growthpilot.dto.CreateCampaignRequest;
import com.growthpilot.entity.Campaign;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.CampaignRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final SecurityUtils securityUtils;

    public CampaignService(CampaignRepository campaignRepository, SecurityUtils securityUtils) {
        this.campaignRepository = campaignRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public Page<CampaignDto> getCampaigns(Pageable pageable) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        return campaignRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CampaignDto getCampaignById(Long id) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Campaign c = campaignRepository.findById(id)
                .filter(camp -> camp.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        return toDto(c);
    }

    @Transactional
    public CampaignDto createCampaign(CreateCampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .business(securityUtils.getCurrentUser().getBusiness())
                .name(request.getName())
                .targetSegment(request.getTargetSegment())
                .offerDetails(request.getOfferDetails())
                .generatedMessage(request.getGeneratedMessage())
                .estimatedImpact(request.getEstimatedImpact() != null ? request.getEstimatedImpact() : BigDecimal.valueOf(15000))
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdByAgent(false)
                .createdAt(LocalDateTime.now())
                .build();

        campaign = campaignRepository.save(campaign);
        return toDto(campaign);
    }

    @Transactional
    public CampaignDto updateStatus(Long id, String status) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Campaign campaign = campaignRepository.findById(id)
                .filter(c -> c.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        campaign.setStatus(status.toUpperCase());
        return toDto(campaignRepository.save(campaign));
    }

    public CampaignDto toDto(Campaign c) {
        return CampaignDto.builder()
                .id(c.getId())
                .name(c.getName())
                .targetSegment(c.getTargetSegment())
                .status(c.getStatus())
                .offerDetails(c.getOfferDetails())
                .generatedMessage(c.getGeneratedMessage())
                .estimatedImpact(c.getEstimatedImpact())
                .createdByAgent(c.getCreatedByAgent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
