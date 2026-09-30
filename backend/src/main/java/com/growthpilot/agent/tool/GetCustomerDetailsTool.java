package com.growthpilot.agent.tool;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.service.CustomerService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCustomerDetailsTool implements AgentTool {

    private final CustomerService customerService;

    public GetCustomerDetailsTool(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public String getName() {
        return "getCustomerDetails";
    }

    @Override
    public String getDescription() {
        return "Retrieve complete intelligence, intent factors, and order history for a customer ID.";
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
            return AgentToolResult.ok(intel, "Retrieved full customer intelligence for " + intel.getName());
        } catch (Exception e) {
            return AgentToolResult.fail("Customer with id " + customerId + " not found.");
        }
    }
}
