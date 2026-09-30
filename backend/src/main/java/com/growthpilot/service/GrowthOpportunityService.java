package com.growthpilot.service;

import com.growthpilot.dto.AgentActionDto;
import com.growthpilot.dto.GrowthOpportunityDto;
import com.growthpilot.dto.OpportunityExecuteRequest;
import com.growthpilot.entity.*;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.*;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrowthOpportunityService {

    private final GrowthOpportunityRepository opportunityRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AgentActionService agentActionService;
    private final SecurityUtils securityUtils;

    public GrowthOpportunityService(GrowthOpportunityRepository opportunityRepository,
                                    CustomerRepository customerRepository,
                                    CartRepository cartRepository,
                                    ProductRepository productRepository,
                                    AgentActionService agentActionService,
                                    SecurityUtils securityUtils) {
        this.opportunityRepository = opportunityRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.agentActionService = agentActionService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<GrowthOpportunityDto> getOpportunitiesDto() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        List<GrowthOpportunity> list = opportunityRepository.findAllByBusinessIdAndStatus(businessId, "PENDING");
        if (list.isEmpty()) {
            return generateOpportunitiesDto();
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<GrowthOpportunityDto> generateOpportunitiesDto() {
        Business business = securityUtils.getCurrentUser().getBusiness();
        Long businessId = business.getId();

        // Clear old PENDING opportunities
        List<GrowthOpportunity> existing = opportunityRepository.findAllByBusinessIdAndStatus(businessId, "PENDING");
        opportunityRepository.deleteAll(existing);

        List<GrowthOpportunity> generated = new ArrayList<>();

        // 1. Recover abandoned carts
        List<Cart> abandonedCarts = cartRepository.findAbandonedCartsByBusinessId(businessId);
        for (Cart cart : abandonedCarts.subList(0, Math.min(abandonedCarts.size(), 4))) {
            BigDecimal cartValue = cart.getItems().stream()
                    .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            GrowthOpportunity opp = opportunityRepository.save(GrowthOpportunity.builder()
                    .business(business)
                    .title("Recover Abandoned Cart - " + cart.getCustomer().getName())
                    .type("RECOVER_CART")
                    .targetCustomer(cart.getCustomer())
                    .estimatedImpact(cartValue)
                    .confidenceScore(82)
                    .recommendedAction("Send a personalized 10% discount code with free shipping offer to recover cart worth ₹" + cartValue.intValue())
                    .reason("Customer abandoned " + cart.getItems().size() + " item(s) worth ₹" + cartValue.intValue() + ". High recovery probability.")
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build());
            generated.add(opp);
        }

        // 2. High-intent customers not converted
        List<Customer> highIntent = customerRepository.findHighIntentCustomers(businessId);
        for (Customer c : highIntent.subList(0, Math.min(highIntent.size(), 4))) {
            BigDecimal impact = c.getAverageOrderValue() != null ? c.getAverageOrderValue() : BigDecimal.valueOf(4500);
            GrowthOpportunity opp = opportunityRepository.save(GrowthOpportunity.builder()
                    .business(business)
                    .title("Convert High-Intent Buyer - " + c.getName())
                    .type("HIGH_INTENT")
                    .targetCustomer(c)
                    .estimatedImpact(impact)
                    .confidenceScore(c.getPurchaseIntentScore() != null ? c.getPurchaseIntentScore() : 85)
                    .recommendedAction("Send targeted " + c.getPreferredCategory() + " recommendation offer with 10% coupon")
                    .reason("Purchase intent score: " + c.getPurchaseIntentScore() + "/100. Category affinity: " + c.getPreferredCategory())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build());
            generated.add(opp);
        }

        // 3. At-risk customers (churn prevention)
        List<Customer> atRisk = customerRepository.findAllByBusinessIdAndChurnRisk(businessId, "HIGH");
        for (Customer c : atRisk.subList(0, Math.min(atRisk.size(), 3))) {
            BigDecimal impact = c.getAverageOrderValue() != null ? c.getAverageOrderValue().multiply(BigDecimal.valueOf(3)) : BigDecimal.valueOf(12000);
            long days = c.getLastOrderDate() != null ? ChronoUnit.DAYS.between(c.getLastOrderDate(), LocalDateTime.now()) : 65;

            GrowthOpportunity opp = opportunityRepository.save(GrowthOpportunity.builder()
                    .business(business)
                    .title("Win Back At-Risk VIP - " + c.getName())
                    .type("PREVENT_CHURN")
                    .targetCustomer(c)
                    .estimatedImpact(impact)
                    .confidenceScore(76)
                    .recommendedAction("Launch win-back sequence with exclusive 15% off and free shipping for next 48 hours")
                    .reason("Customer has HIGH churn risk. Last order was " + days + " days ago.")
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build());
            generated.add(opp);
        }

        // 4. Top product upsell / bundles
        List<Product> topProducts = productRepository.findTopProductsByBusinessId(businessId, PageRequest.of(0, 3));
        for (Product p : topProducts) {
            BigDecimal impact = p.getPrice().multiply(BigDecimal.valueOf(8));
            GrowthOpportunity opp = opportunityRepository.save(GrowthOpportunity.builder()
                    .business(business)
                    .title("Launch Cross-Sell Bundle for " + p.getName())
                    .type("BUNDLE")
                    .targetProduct(p)
                    .estimatedImpact(impact)
                    .confidenceScore(88)
                    .recommendedAction("Create bundle campaign pairing " + p.getName() + " with complementary accessories at 10% discount")
                    .reason("Top-selling product with " + p.getTotalSales() + " units sold and high cart conversion affinity.")
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build());
            generated.add(opp);
        }

        return generated.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AgentActionDto executeOpportunity(Long opportunityId, OpportunityExecuteRequest request) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        GrowthOpportunity opp = opportunityRepository.findById(opportunityId)
                .filter(o -> o.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        String tool;
        String parameters;
        if ("RECOVER_CART".equalsIgnoreCase(opp.getType())) {
            tool = "createRecoveryAction";
            parameters = String.format("{\"customerId\":%d,\"discountPercent\":10,\"message\":\"Special cart recovery offer\"}",
                    opp.getTargetCustomer() != null ? opp.getTargetCustomer().getId() : 1);
        } else if ("HIGH_INTENT".equalsIgnoreCase(opp.getType()) || "PREVENT_CHURN".equalsIgnoreCase(opp.getType())) {
            tool = "createOffer";
            parameters = String.format("{\"customerId\":%d,\"discountPercent\":%d,\"reason\":\"%s\"}",
                    opp.getTargetCustomer() != null ? opp.getTargetCustomer().getId() : 1,
                    "PREVENT_CHURN".equalsIgnoreCase(opp.getType()) ? 15 : 10,
                    opp.getRecommendedAction().replace("\"", "'"));
        } else {
            tool = "createCampaign";
            parameters = String.format("{\"name\":\"%s\",\"targetSegment\":\"Loyal\",\"estimatedImpact\":%.2f}",
                    opp.getTitle().replace("\"", "'"), opp.getEstimatedImpact() != null ? opp.getEstimatedImpact().doubleValue() : 15000.0);
        }

        AgentAction action = agentActionService.createAction(
                "Execute Opportunity: " + opp.getTitle(),
                tool,
                parameters,
                opp.getReason() + " Recommended Action: " + opp.getRecommendedAction()
        );

        if (request != null && request.isAutoApprove()) {
            action = agentActionService.approve(action.getId());
            opp.setStatus("EXECUTED");
        } else {
            opp.setStatus("APPROVED");
        }
        opportunityRepository.save(opp);

        return agentActionService.toDto(action);
    }

    private GrowthOpportunityDto toDto(GrowthOpportunity o) {
        return GrowthOpportunityDto.builder()
                .id(o.getId())
                .title(o.getTitle())
                .type(o.getType())
                .targetCustomerId(o.getTargetCustomer() != null ? o.getTargetCustomer().getId() : null)
                .targetCustomerName(o.getTargetCustomer() != null ? o.getTargetCustomer().getName() : null)
                .targetProductId(o.getTargetProduct() != null ? o.getTargetProduct().getId() : null)
                .targetProductName(o.getTargetProduct() != null ? o.getTargetProduct().getName() : null)
                .estimatedImpact(o.getEstimatedImpact())
                .confidenceScore(o.getConfidenceScore())
                .recommendedAction(o.getRecommendedAction())
                .reason(o.getReason())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
