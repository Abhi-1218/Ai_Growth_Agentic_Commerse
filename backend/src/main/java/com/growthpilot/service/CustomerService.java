package com.growthpilot.service;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.dto.IntentFactorDto;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Order;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.repository.OrderRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PurchaseIntentService intentService;
    private final ChurnRiskService churnRiskService;
    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    public CustomerService(CustomerRepository customerRepository,
                           OrderRepository orderRepository,
                           PurchaseIntentService intentService,
                           ChurnRiskService churnRiskService,
                           RecommendationService recommendationService,
                           SecurityUtils securityUtils) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.intentService = intentService;
        this.churnRiskService = churnRiskService;
        this.recommendationService = recommendationService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAllCustomers(String query, String segment, String churnRisk, Pageable pageable) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        if (query != null && !query.isBlank()) {
            return customerRepository.searchCustomers(businessId, query.trim(), pageable);
        }
        if (segment != null && !segment.isBlank()) {
            List<Customer> list = customerRepository.findAllByBusinessIdAndSegment(businessId, segment.trim());
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }
        if (churnRisk != null && !churnRisk.isBlank()) {
            List<Customer> list = customerRepository.findAllByBusinessIdAndChurnRisk(businessId, churnRisk.trim().toUpperCase());
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }
        return customerRepository.findAllByBusinessId(businessId, pageable);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        return customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerIntelligenceDto getCustomerIntelligence(Long id) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // 1. Calculate intent and update score
        PurchaseIntentService.IntentCalculationResult intentResult = intentService.calculateIntentForCustomer(customer);
        customer.setPurchaseIntentScore(intentResult.getScore());

        // 2. Calculate churn risk and retention action
        ChurnRiskService.ChurnEvaluationResult churnResult = churnRiskService.evaluateCustomer(customer);
        customer.setChurnRisk(churnResult.getChurnRisk());
        customerRepository.save(customer);

        // 3. Fetch recommendations
        List<CustomerIntelligenceDto.RecommendationDto> recs = recommendationService.getRecommendationsForCustomer(id);

        // 4. Fetch recent orders
        List<Order> orders = orderRepository.findAllByCustomerId(id);
        List<CustomerIntelligenceDto.CustomerOrderSummaryDto> recentOrders = orders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(5)
                .map(o -> CustomerIntelligenceDto.CustomerOrderSummaryDto.builder()
                        .id(o.getId())
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus())
                        .orderDate(o.getOrderDate())
                        .itemCount(o.getItems() != null ? o.getItems().size() : 1)
                        .build())
                .collect(Collectors.toList());

        long daysSinceOrder = customer.getLastOrderDate() != null
                ? ChronoUnit.DAYS.between(customer.getLastOrderDate(), LocalDateTime.now())
                : 0;

        return CustomerIntelligenceDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .segment(customer.getSegment())
                .preferredCategory(customer.getPreferredCategory())
                .totalOrders(customer.getTotalOrders())
                .totalSpend(customer.getTotalSpend())
                .averageOrderValue(customer.getAverageOrderValue())
                .lastOrderDate(customer.getLastOrderDate())
                .daysSinceLastOrder(daysSinceOrder)
                .purchaseIntentScore(intentResult.getScore())
                .purchaseIntentStatus(intentResult.getStatus())
                .intentFactors(intentResult.getFactors())
                .churnRisk(churnResult.getChurnRisk())
                .churnReason(churnResult.getChurnReason())
                .suggestedRetentionAction(churnResult.getRetentionAction())
                .engagementScore(customer.getEngagementScore() != null ? customer.getEngagementScore() : 50)
                .recommendations(recs)
                .recentOrders(recentOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Customer> getHighIntentCustomers() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        return customerRepository.findHighIntentCustomers(businessId);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        customer.setBusiness(securityUtils.getCurrentUser().getBusiness());
        if (customer.getSegment() == null) customer.setSegment("New Customer");
        if (customer.getChurnRisk() == null) customer.setChurnRisk("LOW");
        if (customer.getCreatedAt() == null) customer.setCreatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer updated) {
        Customer existing = getCustomerById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setSegment(updated.getSegment());
        existing.setPreferredCategory(updated.getPreferredCategory());
        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer existing = getCustomerById(id);
        customerRepository.delete(existing);
    }
}
