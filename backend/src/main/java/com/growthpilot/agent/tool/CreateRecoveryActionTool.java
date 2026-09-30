package com.growthpilot.agent.tool;

import com.growthpilot.dto.CartRecoveryActionRequest;
import com.growthpilot.service.CartService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateRecoveryActionTool implements AgentTool {

    private final CartService cartService;

    public CreateRecoveryActionTool(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public String getName() {
        return "createRecoveryAction";
    }

    @Override
    public String getDescription() {
        return "Dispatch a personalized cart recovery notification and discount incentive to an abandoned cart owner.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"cartId\":{\"type\":\"integer\"},\"discountPercent\":{\"type\":\"integer\"},\"message\":{\"type\":\"string\"}},\"required\":[\"cartId\"]}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return true;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        Long cartId = ((Number) parameters.get("cartId")).longValue();
        int discount = parameters.containsKey("discountPercent")
                ? ((Number) parameters.get("discountPercent")).intValue()
                : 10;
        String msg = (String) parameters.get("message");

        CartRecoveryActionRequest req = CartRecoveryActionRequest.builder()
                .discountPercent(discount)
                .message(msg)
                .executeDirectly(true)
                .build();

        cartService.triggerRecoveryAction(cartId, req);

        return AgentToolResult.ok(Map.of(
                "cartId", cartId,
                "discountPercent", discount,
                "status", "RECOVERY_DISPATCHED"
        ), "Successfully dispatched cart recovery incentive for Cart #" + cartId);
    }
}
