package com.growthpilot.service;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Product;
import com.growthpilot.entity.Recommendation;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.repository.ProductRepository;
import com.growthpilot.repository.RecommendationRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RecommendationRepository recommendationRepository;
    private final SecurityUtils securityUtils;

    public RecommendationService(CustomerRepository customerRepository,
                                  ProductRepository productRepository,
                                  RecommendationRepository recommendationRepository,
                                  SecurityUtils securityUtils) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.recommendationRepository = recommendationRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<CustomerIntelligenceDto.RecommendationDto> getRecommendationsForCustomer(Long customerId) {
        List<Recommendation> existing = recommendationRepository.findAllByCustomerIdOrderByScoreDesc(customerId);
        if (existing.isEmpty()) {
            return generateAndSaveRecommendations(customerId);
        }
        return existing.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<CustomerIntelligenceDto.RecommendationDto> generateAndSaveRecommendations(Long customerId) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Customer customer = customerRepository.findByIdAndBusinessId(customerId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        recommendationRepository.deleteAllByCustomerId(customerId);

        List<Product> allProducts = productRepository.findAllByBusinessId(businessId);
        List<Recommendation> recs = new ArrayList<>();

        for (Product product : allProducts) {
            double score = computeScore(customer, product);
            if (score > 0.35) {
                String reason = buildReason(customer, product, score);
                Recommendation rec = Recommendation.builder()
                        .customer(customer)
                        .product(product)
                        .score(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                        .reason(reason)
                        .createdAt(LocalDateTime.now())
                        .build();
                recs.add(recommendationRepository.save(rec));
            }
        }

        recs.sort(Comparator.comparing(Recommendation::getScore).reversed());
        return recs.stream().limit(5).map(this::toDto).collect(Collectors.toList());
    }

    private CustomerIntelligenceDto.RecommendationDto toDto(Recommendation r) {
        Product p = r.getProduct();
        return CustomerIntelligenceDto.RecommendationDto.builder()
                .id(r.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productCategory(p.getCategory())
                .productPrice(p.getPrice())
                .score(r.getScore())
                .reason(r.getReason())
                .build();
    }

    private double computeScore(Customer customer, Product product) {
        double score = 0.0;

        // 1. Category affinity (+0.40)
        if (product.getCategory() != null && product.getCategory().equalsIgnoreCase(customer.getPreferredCategory())) {
            score += 0.40;
        }

        // 2. Product popularity & sales velocity (+0.25)
        if (product.getTotalSales() != null && product.getTotalSales() > 0) {
            score += Math.min(0.25, product.getTotalSales() / 500.0);
        }

        // 3. Price elasticity & budget alignment (+0.20)
        if (customer.getAverageOrderValue() != null && product.getPrice() != null && customer.getAverageOrderValue().doubleValue() > 0) {
            double ratio = product.getPrice().doubleValue() / customer.getAverageOrderValue().doubleValue();
            if (ratio >= 0.25 && ratio <= 1.75) {
                score += 0.20;
            }
        }

        // 4. High intent boost (+0.10)
        if (customer.getPurchaseIntentScore() != null && customer.getPurchaseIntentScore() >= 70) {
            score += 0.10;
        }

        return Math.min(0.99, score);
    }

    private String buildReason(Customer customer, Product product, double score) {
        List<String> reasons = new ArrayList<>();
        String firstName = customer.getName() != null ? customer.getName().split(" ")[0] : "Customer";

        if (product.getCategory() != null && product.getCategory().equalsIgnoreCase(customer.getPreferredCategory())) {
            reasons.add("Matches " + firstName + "'s preferred category (" + product.getCategory() + ")");
        }
        if (product.getTotalSales() != null && product.getTotalSales() > 50) {
            reasons.add("Top-selling item with " + product.getTotalSales() + " purchases");
        }
        if (customer.getPurchaseIntentScore() != null && customer.getPurchaseIntentScore() >= 70) {
            reasons.add("Customer exhibits high buying intent (" + customer.getPurchaseIntentScore() + "/100)");
        }
        if (reasons.isEmpty()) {
            reasons.add("Trending product with strong cross-sell affinity in similar segments");
        }
        return String.join("; ", reasons);
    }
}
