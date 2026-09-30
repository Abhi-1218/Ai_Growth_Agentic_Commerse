package com.growthpilot.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.growthpilot.entity.Payment;
import com.growthpilot.repository.PaymentRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetFailedPaymentsTool implements AgentTool {

    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;

    public GetFailedPaymentsTool(PaymentRepository paymentRepository, SecurityUtils securityUtils) {
        this.paymentRepository = paymentRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public String getName() {
        return "getFailedPayments";
    }

    @Override
    public String getDescription() {
        return "Retrieves a summary of failed high-value payments to identify recovery opportunities. No parameters required.";
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
        List<Payment> allFailed = paymentRepository.findAllByBusinessId(businessId).stream()
                .filter(p -> "failed".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalFailedValue = allFailed.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> highValueFailed = allFailed.stream()
                .filter(p -> p.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0)
                .collect(Collectors.toList());

        String result = String.format("Total Failed Payments: %d. Total Recoverable Value: ₹%s. High Value Failures (>₹1000): %d", 
                allFailed.size(), totalFailedValue.toString(), highValueFailed.size());
                
        return AgentToolResult.ok(result, "Found " + highValueFailed.size() + " high-value failed payments");
    }
}
