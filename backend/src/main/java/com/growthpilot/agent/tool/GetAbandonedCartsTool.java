package com.growthpilot.agent.tool;

import com.growthpilot.dto.CartRecoveryDto;
import com.growthpilot.service.CartService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GetAbandonedCartsTool implements AgentTool {

    private final CartService cartService;

    public GetAbandonedCartsTool(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public String getName() {
        return "getAbandonedCarts";
    }

    @Override
    public String getDescription() {
        return "Retrieve high-value abandoned carts with recovery probabilities and suggested recovery offers.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"minRecoveryProbability\":{\"type\":\"integer\"}}}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return false;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        List<CartRecoveryDto> carts = cartService.getAbandonedCartsList();
        Integer minProb = parameters != null && parameters.containsKey("minRecoveryProbability")
                ? ((Number) parameters.get("minRecoveryProbability")).intValue()
                : null;

        if (minProb != null) {
            carts = carts.stream()
                    .filter(c -> c.getRecoveryProbability() >= minProb)
                    .collect(Collectors.toList());
        }

        return AgentToolResult.ok(carts, "Found " + carts.size() + " abandoned carts eligible for automated recovery.");
    }
}
