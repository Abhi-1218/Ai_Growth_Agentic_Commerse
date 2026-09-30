package com.growthpilot.agent.tool;

import com.growthpilot.entity.AgentAction;
import com.growthpilot.entity.Customer;
import com.growthpilot.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateOfferTool implements AgentTool {

    private final CustomerRepository customerRepository;

    public CreateOfferTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String getName() {
        return "createOffer";
    }

    @Override
    public String getDescription() {
        return "Generate a personalized promotional discount offer or coupon code for a specific target customer.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"customerId\":{\"type\":\"integer\"},\"discountPercent\":{\"type\":\"integer\"},\"code\":{\"type\":\"string\"},\"reason\":{\"type\":\"string\"}},\"required\":[\"customerId\",\"discountPercent\"]}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return true;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        Long customerId = ((Number) parameters.get("customerId")).longValue();
        int discount = ((Number) parameters.getOrDefault("discountPercent", 10)).intValue();
        String code = (String) parameters.getOrDefault("code", "GROWTH" + discount);
        String reason = (String) parameters.getOrDefault("reason", "Targeted customer growth incentive");

        Customer customer = customerRepository.findByIdAndBusinessId(customerId, businessId)
                .orElse(null);

        String customerName = customer != null ? customer.getName() : "Customer #" + customerId;
        String summary = String.format("Generated %d%% coupon code '%s' for %s. Rationale: %s", discount, code, customerName, reason);

        return AgentToolResult.ok(Map.of(
                "couponCode", code,
                "discountPercent", discount,
                "targetCustomerId", customerId,
                "targetCustomerName", customerName,
                "status", "DISPATCHED"
        ), summary);
    }
}
