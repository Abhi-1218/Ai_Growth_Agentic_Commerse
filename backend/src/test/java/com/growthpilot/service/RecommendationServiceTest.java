package com.growthpilot.service;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.entity.Business;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Product;
import com.growthpilot.entity.Recommendation;
import com.growthpilot.entity.User;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.repository.ProductRepository;
import com.growthpilot.repository.RecommendationRepository;
import com.growthpilot.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RecommendationService recommendationService;

    private Business business;
    private User user;
    private Customer customer;
    private List<Product> products;

    @BeforeEach
    void setUp() {
        business = Business.builder().id(1L).name("TechMart").build();
        user = User.builder().id(1L).email("admin@techmart.in").business(business).build();

        customer = Customer.builder()
                .id(100L)
                .name("Anjali Verma")
                .preferredCategory("Electronics")
                .averageOrderValue(BigDecimal.valueOf(15000))
                .purchaseIntentScore(80)
                .business(business)
                .build();

        Product p1 = Product.builder()
                .id(1L)
                .name("Sony Headphones")
                .category("Electronics")
                .price(BigDecimal.valueOf(18000))
                .totalSales(150)
                .business(business)
                .build();

        Product p2 = Product.builder()
                .id(2L)
                .name("Nike Air Max")
                .category("Footwear")
                .price(BigDecimal.valueOf(10000))
                .totalSales(50)
                .business(business)
                .build();

        products = List.of(p1, p2);
    }

    @Test
    void testGenerateAndSaveRecommendations() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(customerRepository.findByIdAndBusinessId(100L, 1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByBusinessId(1L)).thenReturn(products);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<CustomerIntelligenceDto.RecommendationDto> recs = recommendationService.generateAndSaveRecommendations(100L);

        assertNotNull(recs);
        assertFalse(recs.isEmpty());
        // Electronics should rank higher than Footwear due to preferred category affinity
        assertEquals("Sony Headphones", recs.get(0).getProductName());
        assertTrue(recs.get(0).getScore().doubleValue() > 0.5);
    }
}
