package com.growthpilot.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.growthpilot.entity.Refund;
import com.growthpilot.repository.RefundRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetRefundInsightsTool implements AgentTool {

    private final RefundRepository refundRepository;
    private final SecurityUtils securityUtils;

    public GetRefundInsightsTool(RefundRepository refundRepository, SecurityUtils securityUtils) {
        this.refundRepository = refundRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public String getName() {
        return "getRefundInsights";
    }

    @Override
    public String getDescription() {
        return "Retrieves a summary of recent refunds to identify high refund rates and business risks. No parameters required.";
    }

    @Override
    public String getParameterSchema() {
        return "{}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return false;
    }

    @Override
    public AgentToolResult execute(java.util.Map<String, Object> parameters, Long businessId, Long userId) {
        List<Refund> allRefunds = refundRepository.findAllByBusinessId(businessId);

        BigDecimal totalRefundValue = allRefunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String result = String.format("Total Refunds Processed: %d. Total Revenue Lost to Refunds: ₹%s. Analyze recent orders to find patterns in product quality or fulfillment issues.", 
                allRefunds.size(), totalRefundValue.toString());
                
        return AgentToolResult.ok(result, "Found " + allRefunds.size() + " refunds");
    }
}
