package com.growthpilot.agent.tool;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.service.CustomerService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CalculateChurnRiskTool implements AgentTool {

    private final CustomerService customerService;

    public CalculateChurnRiskTool(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public String getName() {
        return "calculateChurnRisk";
    }

    @Override
    public String getDescription() {
        return "Evaluate customer inactivity, churn risk level (LOW/MEDIUM/HIGH), and suggested retention strategy.";
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
                    "churnRisk", intel.getChurnRisk(),
                    "reason", intel.getChurnReason(),
                    "suggestedAction", intel.getSuggestedRetentionAction()
            );
            return AgentToolResult.ok(data, "Customer churn risk evaluated as " + intel.getChurnRisk());
        } catch (Exception e) {
            return AgentToolResult.fail("Error evaluating churn risk: " + e.getMessage());
        }
    }
}
