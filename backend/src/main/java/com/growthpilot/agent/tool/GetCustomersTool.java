package com.growthpilot.agent.tool;

import com.growthpilot.entity.Customer;
import com.growthpilot.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GetCustomersTool implements AgentTool {

    private final CustomerRepository customerRepository;

    public GetCustomersTool(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public String getName() {
        return "getCustomer";
    }

    @Override
    public String getDescription() {
        return "Retrieve a list of customers filtered by segment or high intent status.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"segment\":{\"type\":\"string\"},\"minIntentScore\":{\"type\":\"integer\"}}}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return false;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        List<Customer> customers;
        String segment = parameters != null ? (String) parameters.get("segment") : null;
        if (segment != null && !segment.isBlank()) {
            customers = customerRepository.findAllByBusinessIdAndSegment(businessId, segment);
        } else {
            customers = customerRepository.findAllByBusinessId(businessId);
        }

        Integer minScore = parameters != null && parameters.containsKey("minIntentScore")
                ? ((Number) parameters.get("minIntentScore")).intValue()
                : null;

        if (minScore != null) {
            customers = customers.stream()
                    .filter(c -> c.getPurchaseIntentScore() != null && c.getPurchaseIntentScore() >= minScore)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = customers.stream().limit(10).map(c -> Map.<String, Object>of(
                "id", c.getId(),
                "name", c.getName(),
                "email", c.getEmail(),
                "segment", c.getSegment() != null ? c.getSegment() : "Standard",
                "intentScore", c.getPurchaseIntentScore() != null ? c.getPurchaseIntentScore() : 0,
                "preferredCategory", c.getPreferredCategory() != null ? c.getPreferredCategory() : "General"
        )).collect(Collectors.toList());

        return AgentToolResult.ok(result, "Retrieved " + result.size() + " customers matching criteria.");
    }
}
