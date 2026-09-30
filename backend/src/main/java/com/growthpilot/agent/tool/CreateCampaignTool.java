package com.growthpilot.agent.tool;

import com.growthpilot.entity.Campaign;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.CampaignRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CreateCampaignTool implements AgentTool {

    private final CampaignRepository campaignRepository;
    private final BusinessRepository businessRepository;

    public CreateCampaignTool(CampaignRepository campaignRepository, BusinessRepository businessRepository) {
        this.campaignRepository = campaignRepository;
        this.businessRepository = businessRepository;
    }

    @Override
    public String getName() {
        return "createCampaign";
    }

    @Override
    public String getDescription() {
        return "Create a targeted growth or retention marketing campaign for a customer segment.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"targetSegment\":{\"type\":\"string\"},\"offerDetails\":{\"type\":\"string\"},\"generatedMessage\":{\"type\":\"string\"},\"estimatedImpact\":{\"type\":\"number\"}},\"required\":[\"name\",\"targetSegment\"]}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return true;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        String name = (String) parameters.get("name");
        String segment = (String) parameters.get("targetSegment");
        String offer = (String) parameters.getOrDefault("offerDetails", "Standard promotion");
        String message = (String) parameters.getOrDefault("generatedMessage", "Special personalized offer for your favorites.");
        BigDecimal impact = parameters.containsKey("estimatedImpact")
                ? BigDecimal.valueOf(((Number) parameters.get("estimatedImpact")).doubleValue())
                : BigDecimal.valueOf(15000);

        Campaign campaign = Campaign.builder()
                .business(businessRepository.findById(businessId).orElse(null))
                .name(name)
                .targetSegment(segment)
                .offerDetails(offer)
                .generatedMessage(message)
                .estimatedImpact(impact)
                .status("ACTIVE")
                .createdByAgent(true)
                .createdAt(LocalDateTime.now())
                .build();

        campaign = campaignRepository.save(campaign);

        return AgentToolResult.ok(campaign, "Created and activated campaign '" + name + "' for segment " + segment + " with estimated impact ₹" + impact.intValue());
    }
}
