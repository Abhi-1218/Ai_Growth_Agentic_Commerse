package com.growthpilot.agent.tool;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.service.CustomerService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CalculateIntentScoreTool implements AgentTool {

    private final CustomerService customerService;

    public CalculateIntentScoreTool(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public String getName() {
        return "calculateIntentScore";
    }

    @Override
    public String getDescription() {
        return "Calculate buying intent score (0-100) and identify contributing behavioral signals for a customer.";
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
            CustomerIntelligenceDto intel = customerService.getCustomerIntelligence(customerId);
            Map<String, Object> data = Map.of(
                    "customerId", customerId,
                    "customerName", intel.getName(),
                    "intentScore", intel.getPurchaseIntentScore(),
                    "status", intel.getPurchaseIntentStatus(),
                    "factors", intel.getIntentFactors()
            );
            return AgentToolResult.ok(data, "Calculated intent score " + intel.getPurchaseIntentScore() + "/100 (" + intel.getPurchaseIntentStatus() + ").");
        } catch (Exception e) {
            return AgentToolResult.fail("Error calculating intent score: " + e.getMessage());
        }
    }
}
