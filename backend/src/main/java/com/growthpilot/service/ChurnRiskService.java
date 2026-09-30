package com.growthpilot.service;

import com.growthpilot.entity.Customer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ChurnRiskService {

    public static class ChurnEvaluationResult {
        private final String churnRisk; // LOW, MEDIUM, HIGH
        private final String churnReason;
        private final String retentionAction;

        public ChurnEvaluationResult(String churnRisk, String churnReason, String retentionAction) {
            this.churnRisk = churnRisk;
            this.churnReason = churnReason;
            this.retentionAction = retentionAction;
        }

        public String getChurnRisk() { return churnRisk; }
        public String getChurnReason() { return churnReason; }
        public String getRetentionAction() { return retentionAction; }
    }

    public ChurnEvaluationResult evaluateCustomer(Customer customer) {
        long daysSinceOrder = 0;
        if (customer.getLastOrderDate() != null) {
            daysSinceOrder = ChronoUnit.DAYS.between(customer.getLastOrderDate(), LocalDateTime.now());
        }

        int engagement = customer.getEngagementScore() != null ? customer.getEngagementScore() : 50;

        if (daysSinceOrder > 90 || "Dormant".equalsIgnoreCase(customer.getSegment())) {
            return new ChurnEvaluationResult(
                    "HIGH",
                    "Customer has been inactive for " + daysSinceOrder + " days and engagement has dropped to " + engagement + "/100",
                    "Deploy an automated 20% win-back discount campaign with free express shipping within 48 hours."
            );
        } else if (daysSinceOrder > 45 || "At Risk".equalsIgnoreCase(customer.getSegment()) || engagement < 35) {
            return new ChurnEvaluationResult(
                    "MEDIUM",
                    "Order interval exceeded average cycle (" + daysSinceOrder + " days since last purchase) with cooling engagement",
                    "Send a personalized product recommendation email featuring new arrivals in " + customer.getPreferredCategory() + " with 10% loyalty credit."
            );
        } else {
            return new ChurnEvaluationResult(
                    "LOW",
                    "Active customer with consistent purchase cadence and healthy engagement (" + engagement + "/100)",
                    "Maintain VIP status, reward repeat purchases with early access to upcoming product launches."
            );
        }
    }
}
