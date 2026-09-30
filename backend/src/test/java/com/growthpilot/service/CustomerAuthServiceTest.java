package com.growthpilot.service;

import com.growthpilot.dto.CustomerAuthDtos;
import com.growthpilot.entity.Business;
import com.growthpilot.entity.Customer;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.security.JwtUtil;
import com.growthpilot.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {
    @Mock CustomerRepository customers;
    @Mock BusinessRepository businesses;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwt;
    @Mock SecurityUtils security;
    @InjectMocks CustomerAuthService service;

    @Test
    void registerStoresBCryptHashAndReturnsCustomerIdentity() {
        Business business = Business.builder().id(1L).name("TechMart").build();
        when(businesses.findAll()).thenReturn(List.of(business));
        when(customers.findByEmailIgnoreCaseAndBusinessId("buyer@example.com", 1L)).thenReturn(Optional.empty());
        when(encoder.encode("password123")).thenReturn("$2a$hash");
        when(jwt.generateCustomerToken(anyString())).thenReturn("customer-jwt");
        when(customers.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(7L);
            return c;
        });

        CustomerAuthDtos.AuthResponse response = service.register(
                new CustomerAuthDtos.RegisterRequest("Buyer", "buyer@example.com", "password123", null));

        assertEquals("customer-jwt", response.token());
        assertTrue(response.customerId().startsWith("CUS-"));
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customers).save(captor.capture());
        assertEquals("$2a$hash", captor.getValue().getPasswordHash());
        assertEquals("buyer@example.com", captor.getValue().getEmail());
    }
}
