package com.growthpilot.agent.tool;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.service.RecommendationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetRecommendationsTool implements AgentTool {

    private final RecommendationService recommendationService;

    public GetRecommendationsTool(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Override
    public String getName() {
        return "getRecommendations";
    }

    @Override
    public String getDescription() {
        return "Generate personalized AI product recommendations with scores and reasoning for a customer.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"customerId\":{\"type\":\"integer\"}},\"required\":[\"customerId\"]}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return false;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        if (parameters == null || !parameters.containsKey("customerId")) {
            return AgentToolResult.fail("Parameter 'customerId' is required.");
        }
        Long customerId = ((Number) parameters.get("customerId")).longValue();
        try {
            List<CustomerIntelligenceDto.RecommendationDto> recs = recommendationService.getRecommendationsForCustomer(customerId);
            return AgentToolResult.ok(recs, "Generated " + recs.size() + " product recommendations.");
        } catch (Exception e) {
            return AgentToolResult.fail("Failed to generate recommendations: " + e.getMessage());
        }
    }
}
