package com.growthpilot.service;

import com.growthpilot.dto.CustomerAuthDtos;
import com.growthpilot.entity.Business;
import com.growthpilot.entity.Customer;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.security.JwtUtil;
import com.growthpilot.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CustomerAuthService {
    private final CustomerRepository customers;
    private final BusinessRepository businesses;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final SecurityUtils security;

    public CustomerAuthService(CustomerRepository customers, BusinessRepository businesses,
                               PasswordEncoder encoder, JwtUtil jwt, SecurityUtils security) {
        this.customers = customers; this.businesses = businesses; this.encoder = encoder;
        this.jwt = jwt; this.security = security;
    }

    @Transactional
    public CustomerAuthDtos.AuthResponse register(CustomerAuthDtos.RegisterRequest request) {
        Business business = businesses.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No storefront business configured"));
        if (customers.findByEmailIgnoreCaseAndBusinessId(request.email().trim(), business.getId()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        Customer customer = Customer.builder().business(business).name(request.name().trim())
                .email(request.email().trim().toLowerCase()).phone(request.phone())
                .customerId("CUS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .passwordHash(encoder.encode(request.password())).createdAt(LocalDateTime.now())
                .totalOrders(0).totalSpend(java.math.BigDecimal.ZERO).engagementScore(0)
                .purchaseIntentScore(0).churnRisk("LOW").segment("New Customer").build();
        return response(customers.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerAuthDtos.AuthResponse login(CustomerAuthDtos.LoginRequest request) {
        Business business = businesses.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No storefront business configured"));
        Customer customer = customers.findByEmailIgnoreCaseAndBusinessId(request.email().trim(), business.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (customer.getPasswordHash() == null || !encoder.matches(request.password(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (customer.getCustomerId() == null || customer.getCustomerId().isBlank()) {
            customer.setCustomerId("CUS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            customers.save(customer);
        }
        return response(customer);
    }

    @Transactional(readOnly = true)
    public CustomerAuthDtos.Profile profile() {
        Customer c = security.getCurrentCustomer();
        return new CustomerAuthDtos.Profile(c.getCustomerId(), c.getId(), c.getName(), c.getEmail(), c.getPhone());
    }

    @Transactional
    public CustomerAuthDtos.Profile update(CustomerAuthDtos.ProfileUpdate request) {
        Customer c = security.getCurrentCustomer();
        c.setName(request.name().trim()); c.setPhone(request.phone());
        customers.save(c);
        return profile();
    }

    private CustomerAuthDtos.AuthResponse response(Customer c) {
        return new CustomerAuthDtos.AuthResponse(jwt.generateCustomerToken(c.getCustomerId()),
                c.getCustomerId(), c.getId(), c.getName(), c.getEmail());
    }
}
