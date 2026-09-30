package com.growthpilot.service;

import com.growthpilot.dto.StorefrontEventRequest;
import com.growthpilot.entity.*;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.*;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class StorefrontEventService {
    private static final Set<String> ALLOWED = Set.of("SESSION_STARTED", "ACCOUNT_CREATED", "LOGIN_COMPLETED", "PRODUCT_VIEWED", "SEARCH_PERFORMED",
            "ADD_TO_CART", "CART_VIEWED", "CART_ITEM_REMOVED", "CHECKOUT_STARTED", "PAYMENT_STARTED", "PURCHASE_COMPLETED", "PAYMENT_FAILED");
    private final SecurityUtils securityUtils;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final CustomerEventRepository events;
    private final OrderRepository orders;

    public StorefrontEventService(SecurityUtils securityUtils, CustomerRepository customers,
                                  ProductRepository products, CustomerEventRepository events, OrderRepository orders) {
        this.securityUtils = securityUtils; this.customers = customers; this.products = products; this.events = events; this.orders = orders;
    }

    @Transactional
    public void record(StorefrontEventRequest request) {
        if (!ALLOWED.contains(request.eventType())) throw new IllegalArgumentException("Unsupported storefront event");
        Customer authenticatedCustomer = securityUtils.getCurrentCustomerOptional().orElse(null);
        Long businessId = authenticatedCustomer != null ? authenticatedCustomer.getBusiness().getId()
                : securityUtils.getCurrentUser().getBusiness().getId();
        Customer customer = authenticatedCustomer != null ? authenticatedCustomer : request.customerId() == null
                ? customers.findAllByBusinessId(businessId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Customer is required to record events"))
                : customers.findByIdAndBusinessId(request.customerId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Product product = null;
        if (request.productId() != null)
            product = products.findByIdAndBusinessId(request.productId(), businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (request.orderId() != null && orders.findByIdAndBusinessId(request.orderId(), businessId).isEmpty())
            throw new ResourceNotFoundException("Order not found");
        events.save(CustomerEvent.builder().customer(customer).eventType(request.eventType())
                .product(product).timestamp(LocalDateTime.now()).sessionId(request.sessionId()).build());
    }
}
