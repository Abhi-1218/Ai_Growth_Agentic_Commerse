package com.growthpilot.service;

import com.growthpilot.dto.CartRecoveryActionRequest;
import com.growthpilot.dto.CartRecoveryDto;
import com.growthpilot.entity.AgentAction;
import com.growthpilot.entity.Cart;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Product;
import com.growthpilot.entity.CartItem;
import com.growthpilot.entity.CustomerEvent;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.CartRepository;
import com.growthpilot.repository.ProductRepository;
import com.growthpilot.repository.CustomerEventRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import com.growthpilot.dto.CustomerCartDto;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final SecurityUtils securityUtils;
    private final AgentActionService agentActionService;
    private final ProductRepository productRepository;
    private final CustomerEventRepository eventRepository;

    public CartService(CartRepository cartRepository,
                       SecurityUtils securityUtils,
                       AgentActionService agentActionService,
                       ProductRepository productRepository,
                       CustomerEventRepository eventRepository) {
        this.cartRepository = cartRepository;
        this.securityUtils = securityUtils;
        this.agentActionService = agentActionService;
        this.productRepository = productRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Page<CartRecoveryDto> getAbandonedCarts(Pageable pageable) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Page<Cart> carts = cartRepository.findAllByBusinessId(businessId, pageable);
        List<CartRecoveryDto> dtos = carts.getContent().stream()
                .filter(c -> "ABANDONED".equalsIgnoreCase(c.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, carts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<CartRecoveryDto> getAbandonedCartsList() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        return cartRepository.findAbandonedCartsByBusinessId(businessId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CartRecoveryDto getCartRecoveryPlan(Long cartId) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Cart cart = cartRepository.findByIdAndBusinessId(cartId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));
        return toDto(cart);
    }

    @Transactional
    public AgentAction triggerRecoveryAction(Long cartId, CartRecoveryActionRequest request) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Cart cart = cartRepository.findByIdAndBusinessId(cartId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        CartRecoveryDto plan = toDto(cart);
        int discount = request.getDiscountPercent() != null ? request.getDiscountPercent() : 10;
        String message = request.getMessage() != null && !request.getMessage().isBlank()
                ? request.getMessage()
                : plan.getSuggestedMessage();

        String goal = "Recover abandoned cart #" + cartId + " for " + cart.getCustomer().getName();
        String tool = "createRecoveryAction";
        String parameters = String.format("{\"cartId\":%d,\"customerId\":%d,\"customerEmail\":\"%s\",\"cartValue\":%.2f,\"discountPercent\":%d,\"message\":\"%s\"}",
                cartId, cart.getCustomer().getId(), cart.getCustomer().getEmail(), plan.getTotalValue(), discount, message.replace("\"", "'"));
        String reasoning = String.format("Recovery probability is %d%% with cart value ₹%.2f. Sending targeted %d%% discount incentive.",
                plan.getRecoveryProbability(), plan.getTotalValue(), discount);

        AgentAction action = agentActionService.createAction(goal, tool, parameters, reasoning);

        if (request.isExecuteDirectly()) {
            return agentActionService.approve(action.getId());
        }
        return action;
    }

    public BigDecimal calculateCartValue(Cart cart) {
        if (cart.getItems() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public CustomerCartDto getCustomerCart() {
        return getCustomerCart(null);
    }

    @Transactional
    public CustomerCartDto getCustomerCart(String sessionId) {
        Customer customer = securityUtils.getCurrentCustomer();
        Cart cart = cartRepository.findByCustomerIdAndStatus(customer.getId(), "ACTIVE")
                .orElseGet(() -> Cart.builder().customer(customer).business(customer.getBusiness()).status("ACTIVE")
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        if (cart.getId() == null) cart = cartRepository.save(cart);
        eventRepository.save(CustomerEvent.builder().customer(customer).eventType("CART_VIEWED")
                .timestamp(LocalDateTime.now()).sessionId(sessionId).build());
        return customerCartDto(cart);
    }

    @Transactional
    public CustomerCartDto setCustomerItem(Long productId, int quantity) {
        return setCustomerItem(productId, quantity, null);
    }

    @Transactional
    public CustomerCartDto setCustomerItem(Long productId, int quantity, String sessionId) {
        if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("Quantity must be between 1 and 99");
        Customer customer = securityUtils.getCurrentCustomer();
        Product product = productRepository.findByIdAndBusinessId(productId, customer.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for " + product.getName());
        }
        Cart cart = cartRepository.findByCustomerIdAndStatus(customer.getId(), "ACTIVE")
                .orElseGet(() -> Cart.builder().customer(customer).business(customer.getBusiness()).status("ACTIVE")
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        CartItem item = cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst().orElse(null);
        if (item == null) cart.getItems().add(CartItem.builder().cart(cart).product(product).quantity(quantity).build());
        else item.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        cart = cartRepository.save(cart);
        eventRepository.save(CustomerEvent.builder().customer(customer).product(product).eventType("ADD_TO_CART")
                .timestamp(LocalDateTime.now()).sessionId(sessionId).build());
        return customerCartDto(cart);
    }

    @Transactional
    public CustomerCartDto removeCustomerItem(Long productId) {
        return removeCustomerItem(productId, null);
    }

    @Transactional
    public CustomerCartDto removeCustomerItem(Long productId, String sessionId) {
        Customer customer = securityUtils.getCurrentCustomer();
        Cart cart = cartRepository.findByCustomerIdAndStatus(customer.getId(), "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        cart.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(CustomerEvent.builder().customer(customer).eventType("CART_ITEM_REMOVED")
                .timestamp(LocalDateTime.now()).sessionId(sessionId).build());
        return customerCartDto(cartRepository.save(cart));
    }

    private CustomerCartDto customerCartDto(Cart cart) {
        List<CustomerCartDto.Item> items = cart.getItems().stream().map(i -> {
            BigDecimal subtotal = i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CustomerCartDto.Item(i.getProduct().getId(), i.getProduct().getName(), i.getProduct().getPrice(),
                    i.getQuantity(), subtotal);
        }).toList();
        return new CustomerCartDto(cart.getId(), cart.getStatus(), items,
                items.stream().map(CustomerCartDto.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public int calculateRecoveryProbability(Cart cart) {
        int score = 55;
        BigDecimal value = calculateCartValue(cart);
        if (value.compareTo(BigDecimal.valueOf(10000)) > 0) score += 20;
        else if (value.compareTo(BigDecimal.valueOf(3000)) > 0) score += 12;
        else if (value.compareTo(BigDecimal.valueOf(500)) < 0) score -= 10;

        Customer c = cart.getCustomer();
        if (c != null) {
            if ("LOW".equalsIgnoreCase(c.getChurnRisk())) score += 15;
            else if ("HIGH".equalsIgnoreCase(c.getChurnRisk())) score -= 12;

            if (c.getPurchaseIntentScore() != null && c.getPurchaseIntentScore() >= 75) score += 10;
        }

        return Math.min(95, Math.max(25, score));
    }

    private CartRecoveryDto toDto(Cart cart) {
        Customer c = cart.getCustomer();
        BigDecimal totalVal = calculateCartValue(cart);
        int recoveryProb = calculateRecoveryProbability(cart);

        int suggestedDiscount = recoveryProb >= 70 ? 10 : 15;
        String recommendedIncentive = suggestedDiscount + "% Instant Discount + Free Delivery";
        String firstName = c != null && c.getName() != null ? c.getName().split(" ")[0] : "Shopper";
        String suggestedMessage = String.format(
                "Hey %s! You left items worth ₹%d in your cart. Complete your order today using code RECOVER%d for %d%% off + Free Express Shipping!",
                firstName, totalVal.intValue(), suggestedDiscount, suggestedDiscount
        );

        List<CartRecoveryDto.CartItemDto> items = cart.getItems().stream().map(item -> CartRecoveryDto.CartItemDto.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .category(item.getProduct().getCategory())
                .price(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build()).collect(Collectors.toList());

        return CartRecoveryDto.builder()
                .cartId(cart.getId())
                .customerId(c != null ? c.getId() : null)
                .customerName(c != null ? c.getName() : "Guest")
                .customerEmail(c != null ? c.getEmail() : "")
                .customerSegment(c != null ? c.getSegment() : "Standard")
                .churnRisk(c != null ? c.getChurnRisk() : "LOW")
                .purchaseIntentScore(c != null && c.getPurchaseIntentScore() != null ? c.getPurchaseIntentScore() : 50)
                .items(items)
                .totalValue(totalVal)
                .recoveryProbability(recoveryProb)
                .recommendedIncentive(recommendedIncentive)
                .suggestedMessage(suggestedMessage)
                .status(cart.getStatus())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
