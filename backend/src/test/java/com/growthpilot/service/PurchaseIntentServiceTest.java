package com.growthpilot.service;

import com.growthpilot.dto.IntentFactorDto;
import com.growthpilot.entity.Business;
import com.growthpilot.entity.Cart;
import com.growthpilot.entity.Customer;
import com.growthpilot.repository.CartRepository;
import com.growthpilot.repository.CustomerEventRepository;
import com.growthpilot.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseIntentServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEventRepository eventRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private PurchaseIntentService purchaseIntentService;

    private Customer testCustomer;
    private Business business;

    @BeforeEach
    void setUp() {
        business = Business.builder().id(1L).name("TechMart").build();
        testCustomer = Customer.builder()
                .id(10L)
                .name("Rahul Sharma")
                .email("rahul@example.com")
                .preferredCategory("Electronics")
                .business(business)
                .totalOrders(12)
                .totalSpend(BigDecimal.valueOf(45000))
                .averageOrderValue(BigDecimal.valueOf(3750))
                .lastOrderDate(LocalDateTime.now().minusDays(3))
                .engagementScore(85)
                .purchaseIntentScore(75)
                .build();
    }

    @Test
    void testCalculateIntentForCustomer_HighIntent() {
        Cart cart = Cart.builder()
                .id(1L)
                .customer(testCustomer)
                .business(business)
                .status("ABANDONED")
                .build();

        when(cartRepository.findAllByBusinessIdAndStatus(1L, "ABANDONED"))
                .thenReturn(List.of(cart));

        PurchaseIntentService.IntentCalculationResult result =
                purchaseIntentService.calculateIntentForCustomer(testCustomer);

        assertNotNull(result);
        assertTrue(result.getScore() >= 75, "Score should be high for recent order, repeat buyer and abandoned cart");
        assertEquals("High Intent", result.getStatus());
        assertEquals(4, result.getFactors().size());
    }

    @Test
    void testExplainIntentFactors_Structure() {
        when(cartRepository.findAllByBusinessIdAndStatus(1L, "ABANDONED"))
                .thenReturn(List.of());

        PurchaseIntentService.IntentCalculationResult result =
                purchaseIntentService.calculateIntentForCustomer(testCustomer);

        List<IntentFactorDto> factors = result.getFactors();
        assertNotNull(factors);
        assertEquals(4, factors.size());
        assertTrue(factors.stream().anyMatch(f -> f.getSignalName().contains("Recency")));
        assertTrue(factors.stream().anyMatch(f -> f.getSignalName().contains("Spending Power")));
    }
}
