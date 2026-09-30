package com.growthpilot.service;

import com.growthpilot.dto.IntentFactorDto;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.CustomerEvent;
import com.growthpilot.repository.CartRepository;
import com.growthpilot.repository.CustomerEventRepository;
import com.growthpilot.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseIntentService {

    private final CustomerRepository customerRepository;
    private final CustomerEventRepository eventRepository;
    private final CartRepository cartRepository;

    public PurchaseIntentService(CustomerRepository customerRepository,
                                 CustomerEventRepository eventRepository,
                                 CartRepository cartRepository) {
        this.customerRepository = customerRepository;
        this.eventRepository = eventRepository;
        this.cartRepository = cartRepository;
    }

    public static class IntentCalculationResult {
        private final int score;
        private final String status;
        private final List<IntentFactorDto> factors;

        public IntentCalculationResult(int score, String status, List<IntentFactorDto> factors) {
            this.score = score;
            this.status = status;
            this.factors = factors;
        }

        public int getScore() { return score; }
        public String getStatus() { return status; }
        public List<IntentFactorDto> getFactors() { return factors; }
    }

    /**
     * Calculates transparent, explainable purchase intent for a single customer.
     */
    public IntentCalculationResult calculateIntentForCustomer(Customer customer) {
        List<IntentFactorDto> factors = new ArrayList<>();
        int totalScore = 0;

        // Signal 1: Recency & Browsing Activity (Max 30 pts)
        int recencyPoints = 0;
        String recencyExplanation;
        if (customer.getLastOrderDate() != null) {
            long daysSinceLastOrder = ChronoUnit.DAYS.between(customer.getLastOrderDate(), LocalDateTime.now());
            if (daysSinceLastOrder <= 7) {
                recencyPoints = 30;
                recencyExplanation = "Ordered in the last 7 days (" + daysSinceLastOrder + "d ago) – strong recency signal";
            } else if (daysSinceLastOrder <= 30) {
                recencyPoints = 22;
                recencyExplanation = "Active within the last month (" + daysSinceLastOrder + "d ago)";
            } else if (daysSinceLastOrder <= 60) {
                recencyPoints = 12;
                recencyExplanation = "Moderate recency (" + daysSinceLastOrder + "d ago)";
            } else {
                recencyPoints = 5;
                recencyExplanation = "Last purchase was over " + daysSinceLastOrder + " days ago";
            }
        } else {
            recencyPoints = 15;
            recencyExplanation = "New customer with active browsing session";
        }
        factors.add(IntentFactorDto.builder()
                .signalName("Browsing & Purchase Recency")
                .scoreContribution(recencyPoints)
                .maxScore(30)
                .status(recencyPoints >= 20 ? "POSITIVE" : (recencyPoints >= 10 ? "NEUTRAL" : "NEGATIVE"))
                .explanation(recencyExplanation)
                .build());
        totalScore += recencyPoints;

        // Signal 2: Cart Activity & Item Intent (Max 30 pts)
        int cartPoints = 0;
        String cartExplanation;
        long abandonedCount = cartRepository.findAllByBusinessIdAndStatus(customer.getBusiness().getId(), "ABANDONED")
                .stream().filter(c -> c.getCustomer().getId().equals(customer.getId())).count();
        if (abandonedCount > 0) {
            cartPoints = 28;
            cartExplanation = "Active items in cart awaiting checkout – immediate purchase candidate";
        } else if ("Cart Abandoner".equalsIgnoreCase(customer.getSegment()) || "High Intent".equalsIgnoreCase(customer.getSegment())) {
            cartPoints = 22;
            cartExplanation = "High-frequency cart additions in recent sessions";
        } else {
            cartPoints = 10;
            cartExplanation = "Standard cart engagement activity";
        }
        factors.add(IntentFactorDto.builder()
                .signalName("Cart & Checkout Intent")
                .scoreContribution(cartPoints)
                .maxScore(30)
                .status(cartPoints >= 20 ? "POSITIVE" : "NEUTRAL")
                .explanation(cartExplanation)
                .build());
        totalScore += cartPoints;

        // Signal 3: Historical Spending & Order Frequency (Max 25 pts)
        int spendPoints = 0;
        String spendExplanation;
        if (customer.getTotalOrders() >= 10) {
            spendPoints = 25;
            spendExplanation = "Frequent repeat buyer (" + customer.getTotalOrders() + " orders, ₹" + (customer.getTotalSpend() != null ? customer.getTotalSpend().intValue() : 0) + " total spend)";
        } else if (customer.getTotalOrders() >= 4) {
            spendPoints = 18;
            spendExplanation = "Consistent repeat customer (" + customer.getTotalOrders() + " orders)";
        } else if (customer.getTotalOrders() >= 1) {
            spendPoints = 12;
            spendExplanation = "Previous verified purchaser";
        } else {
            spendPoints = 5;
            spendExplanation = "No prior purchase history";
        }
        factors.add(IntentFactorDto.builder()
                .signalName("Spending Power & Order Frequency")
                .scoreContribution(spendPoints)
                .maxScore(25)
                .status(spendPoints >= 18 ? "POSITIVE" : (spendPoints >= 10 ? "NEUTRAL" : "NEGATIVE"))
                .explanation(spendExplanation)
                .build());
        totalScore += spendPoints;

        // Signal 4: Engagement Level & Category Affinity (Max 15 pts)
        int engagementPoints = 0;
        String engagementExplanation;
        int engScore = customer.getEngagementScore() != null ? customer.getEngagementScore() : 50;
        if (engScore >= 75) {
            engagementPoints = 15;
            engagementExplanation = "High engagement score (" + engScore + "/100) with clear " + customer.getPreferredCategory() + " preference";
        } else if (engScore >= 45) {
            engagementPoints = 10;
            engagementExplanation = "Moderate engagement (" + engScore + "/100)";
        } else {
            engagementPoints = 4;
            engagementExplanation = "Low engagement score (" + engScore + "/100)";
        }
        factors.add(IntentFactorDto.builder()
                .signalName("Engagement Score & Category Affinity")
                .scoreContribution(engagementPoints)
                .maxScore(15)
                .status(engagementPoints >= 12 ? "POSITIVE" : (engagementPoints >= 8 ? "NEUTRAL" : "NEGATIVE"))
                .explanation(engagementExplanation)
                .build());
        totalScore += engagementPoints;

        // Bound score between 0 and 100
        int finalScore = Math.min(100, Math.max(10, totalScore));
        String status = finalScore >= 75 ? "High Intent" : (finalScore >= 45 ? "Moderate Intent" : "Low Intent");

        return new IntentCalculationResult(finalScore, status, factors);
    }

    @Transactional
    public void calculatePurchaseIntentForBusiness(Long businessId) {
        List<Customer> customers = customerRepository.findAllByBusinessId(businessId);
        for (Customer c : customers) {
            IntentCalculationResult result = calculateIntentForCustomer(c);
            c.setPurchaseIntentScore(result.getScore());
            customerRepository.save(c);
        }
    }
}
