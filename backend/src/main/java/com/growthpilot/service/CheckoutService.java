package com.growthpilot.service;

import com.growthpilot.dto.CheckoutDtos;
import com.growthpilot.entity.*;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.razorpay.client.RazorpayClientFactory;
import com.growthpilot.repository.*;
import com.growthpilot.security.SecurityUtils;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class CheckoutService {
    private final SecurityUtils securityUtils;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClientFactory clientFactory;
    private final CartRepository cartRepository;
    private final CustomerEventRepository eventRepository;

    @Value("${razorpay.key.id:}")
    private String defaultKeyId;

    @Value("${razorpay.key.secret:}")
    private String defaultKeySecret;

    public CheckoutService(SecurityUtils securityUtils, BusinessRepository businessRepository,
                           CustomerRepository customerRepository, ProductRepository productRepository,
                           OrderRepository orderRepository, PaymentRepository paymentRepository,
                           RazorpayClientFactory clientFactory, CartRepository cartRepository,
                           CustomerEventRepository eventRepository) {
        this.securityUtils = securityUtils;
        this.businessRepository = businessRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.clientFactory = clientFactory;
        this.cartRepository = cartRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public CheckoutDtos.OrderResponse createOrder(CheckoutDtos.CreateOrderRequest request) {
        Customer authenticatedCustomer = securityUtils.getCurrentCustomerOptional().orElse(null);
        Long requestedBusinessId = authenticatedCustomer != null ? authenticatedCustomer.getBusiness().getId()
                : securityUtils.getCurrentUser().getBusiness().getId();
        Business business = businessRepository.findById(requestedBusinessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        Customer customer = authenticatedCustomer != null ? authenticatedCustomer : request.customerId() == null
                ? customerRepository.findAllByBusinessId(business.getId()).stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("A seeded customer is required for demo checkout"))
                : customerRepository.findByIdAndBusinessId(request.customerId(), business.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (authenticatedCustomer != null && !authenticatedCustomer.getBusiness().getId().equals(business.getId())) {
            throw new IllegalArgumentException("Customer is not part of this store");
        }
        Order order = Order.builder().business(business).customer(customer).status("PENDING")
                .currency("INR").orderDate(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO).build();
        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutDtos.Item input : request.items()) {
            Product product = productRepository.findByIdAndBusinessId(input.productId(), business.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + input.productId()));
            if (product.getStock() == null || product.getStock() < input.quantity()) {
                throw new IllegalArgumentException("Insufficient stock for " + product.getName());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(input.quantity())));
            order.getItems().add(OrderItem.builder().order(order).product(product)
                    .quantity(input.quantity()).priceAtPurchase(product.getPrice()).build());
        }
        if (total.signum() <= 0) throw new IllegalArgumentException("Cart total must be greater than zero");
        order.setTotalAmount(total);
        order.setReceipt("GP-" + System.currentTimeMillis());
        orderRepository.save(order);
        eventRepository.save(CustomerEvent.builder().customer(customer).eventType("CHECKOUT_STARTED")
                .timestamp(LocalDateTime.now()).sessionId(request.sessionId()).build());

        String keyId = business.getRazorpayKeyId() != null && !business.getRazorpayKeyId().isBlank()
                ? business.getRazorpayKeyId() : defaultKeyId;
        String secret = business.getRazorpayKeySecret() != null && !business.getRazorpayKeySecret().isBlank()
                ? business.getRazorpayKeySecret() : defaultKeySecret;
        if (keyId == null || keyId.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException("Razorpay TEST MODE credentials are not configured");
        }
        try {
            RazorpayClient client = clientFactory.getClient(keyId, secret);
            JSONObject options = new JSONObject().put("amount", total.movePointRight(2).intValueExact())
                    .put("currency", "INR").put("receipt", order.getReceipt());
            int amountPaise = options.getInt("amount");
            log.info("Creating Razorpay TEST order: businessId={}, internalOrderId={}, keyId={}, amountPaise={}, currency={}, receipt={}",
                    business.getId(), order.getId(), redactKeyId(keyId), amountPaise, options.getString("currency"), order.getReceipt());
            com.razorpay.Order razorpayOrder = client.orders.create(options);
            order.setRazorpayOrderId(razorpayOrder.get("id").toString());
            orderRepository.save(order);
            log.info("Razorpay TEST order created: internalOrderId={}, razorpayOrderId={}",
                    order.getId(), redactId(order.getRazorpayOrderId()));
            return new CheckoutDtos.OrderResponse(order.getId(), order.getRazorpayOrderId(), total,
                    "INR", keyId, order.getStatus());
        } catch (Exception e) {
            log.error("Razorpay TEST order creation failed: internalOrderId={}, keyId={}, amountPaise={}, currency=INR, error={}",
                    order.getId(), redactKeyId(keyId), total.movePointRight(2), sanitizeError(e));
            if (sanitizeError(e).toLowerCase().contains("authentication failed")) {
                throw new IllegalStateException("Razorpay Test Mode authentication failed. Check RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.", e);
            }
            throw new IllegalStateException("Unable to create Razorpay TEST MODE order", e);
        }
    }

    @Transactional
    public CheckoutDtos.OrderResponse verifyPayment(CheckoutDtos.VerifyPaymentRequest request) {
        Long requestedBusinessId = securityUtils.getCurrentCustomerOptional()
                .map(c -> c.getBusiness().getId()).orElseGet(() -> securityUtils.getCurrentUser().getBusiness().getId());
        Business business = businessRepository.findById(requestedBusinessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        Order order = securityUtils.getCurrentCustomerOptional().map(c -> orderRepository.findByIdAndCustomerId(request.orderId(), c.getId()))
                .orElseGet(() -> orderRepository.findByIdAndBusinessId(request.orderId(), business.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!request.razorpayOrderId().equals(order.getRazorpayOrderId())) throw new IllegalArgumentException("Order mismatch");
        String keyId = business.getRazorpayKeyId() != null && !business.getRazorpayKeyId().isBlank()
                ? business.getRazorpayKeyId() : defaultKeyId;
        try {
            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                return new CheckoutDtos.OrderResponse(order.getId(), order.getRazorpayOrderId(),
                        order.getTotalAmount(), order.getCurrency(), keyId, order.getStatus());
            }
            String secret = business.getRazorpayKeySecret() != null && !business.getRazorpayKeySecret().isBlank()
                    ? business.getRazorpayKeySecret() : defaultKeySecret;
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("Razorpay TEST MODE secret is not configured");
            }
            boolean valid = Utils.verifyPaymentSignature(new JSONObject()
                    .put("razorpay_order_id", request.razorpayOrderId())
                    .put("razorpay_payment_id", request.razorpayPaymentId())
                    .put("razorpay_signature", request.razorpaySignature()), secret);
            if (!valid) throw new IllegalArgumentException("Invalid payment signature");
            Payment payment = paymentRepository.findByRazorpayPaymentId(request.razorpayPaymentId())
                    .orElseGet(() -> Payment.builder().business(business).order(order)
                            .razorpayPaymentId(request.razorpayPaymentId()).amount(order.getTotalAmount())
                            .currency(order.getCurrency()).status("captured").captured(true)
                            .createdAt(LocalDateTime.now()).build());
            payment.setStatus("captured"); payment.setCaptured(true); paymentRepository.save(payment);
            order.getItems().forEach(item -> {
                Product p = item.getProduct();
                p.setStock(Math.max(0, (p.getStock() == null ? 0 : p.getStock()) - item.getQuantity()));
                p.setTotalSales((p.getTotalSales() == null ? 0 : p.getTotalSales()) + item.getQuantity());
                productRepository.save(p);
            });
            order.setStatus("PAID"); orderRepository.save(order);
            Customer customer = order.getCustomer();
            eventRepository.save(CustomerEvent.builder().customer(customer).eventType("PURCHASE_COMPLETED")
                    .timestamp(LocalDateTime.now()).sessionId(request.sessionId()).build());
            cartRepository.findByCustomerIdAndStatus(customer.getId(), "ACTIVE").ifPresent(cart -> {
                cart.setStatus("CONVERTED"); cart.setUpdatedAt(LocalDateTime.now()); cartRepository.save(cart);
            });
            log.info("Razorpay payment verified: internalOrderId={}, razorpayOrderId={}, paymentId={}, status=PAID",
                    order.getId(), redactId(request.razorpayOrderId()), redactId(request.razorpayPaymentId()));
            return new CheckoutDtos.OrderResponse(order.getId(), order.getRazorpayOrderId(),
                    order.getTotalAmount(), order.getCurrency(), keyId, order.getStatus());
        } catch (IllegalArgumentException e) {
            markPaymentFailedInternal(order, e.getMessage());
            throw e;
        } catch (Exception e) {
            markPaymentFailedInternal(order, e.getMessage());
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Payment verification failed", e);
        }
    }

    @Transactional
    public void markPaymentFailed(Long orderId, String reason) {
        markPaymentFailed(orderId, reason, null);
    }

    @Transactional
    public void markPaymentFailed(Long orderId, String reason, String sessionId) {
        Customer customer = securityUtils.getCurrentCustomer();
        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        markPaymentFailedInternal(order, reason, sessionId);
    }

    private void markPaymentFailedInternal(Order order, String reason) {
        markPaymentFailedInternal(order, reason, null);
    }

    private void markPaymentFailedInternal(Order order, String reason, String sessionId) {
        if ("PAYMENT_FAILED".equalsIgnoreCase(order.getStatus())) return;
        Customer customer = order.getCustomer();
        order.setStatus("PAYMENT_FAILED");
        orderRepository.save(order);
        cartRepository.findByCustomerIdAndStatus(customer.getId(), "ACTIVE").ifPresent(cart -> {
            cart.setStatus("ABANDONED"); cart.setUpdatedAt(LocalDateTime.now()); cartRepository.save(cart);
        });
        eventRepository.save(CustomerEvent.builder().customer(customer).eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now()).sessionId(sessionId).build());
        log.warn("Payment failed: orderId={}, reason={}", order.getId(), sanitizeError(new IllegalArgumentException(reason)));
    }

    @Transactional
    public void applyWebhookPayment(String paymentId, String orderId, String status, boolean captured, Business business) {
        if (paymentId == null) return;
        Order order = orderId == null ? null : orderRepository.findAllByBusinessId(business.getId()).stream()
                .filter(o -> orderId.equals(o.getRazorpayOrderId())).findFirst().orElse(null);
        Payment payment = paymentRepository.findByRazorpayPaymentId(paymentId).orElseGet(() ->
                Payment.builder().business(business).order(order).razorpayPaymentId(paymentId)
                        .amount(order == null ? BigDecimal.ZERO : order.getTotalAmount())
                        .currency(order == null ? "INR" : order.getCurrency()).createdAt(LocalDateTime.now()).build());
        payment.setStatus(status); payment.setCaptured(captured); paymentRepository.save(payment);
        if (order != null) {
            order.setStatus(captured ? "PAID" : "PAYMENT_FAILED"); orderRepository.save(order);
            eventRepository.save(CustomerEvent.builder().customer(order.getCustomer())
                    .eventType(captured ? "PURCHASE_COMPLETED" : "PAYMENT_FAILED")
                    .timestamp(LocalDateTime.now()).build());
        }
    }

    private String redactKeyId(String keyId) {
        if (keyId == null || keyId.length() < 8) return "[redacted]";
        return keyId.substring(0, 8) + "...";
    }

    private String redactId(String value) {
        if (value == null || value.length() < 8) return "[redacted]";
        return value.substring(0, 6) + "...";
    }

    private String sanitizeError(Exception error) {
        String message = error.getMessage();
        if (message == null) return error.getClass().getSimpleName();
        return message.replaceAll("(?i)(key_secret|password|secret|authorization)\\s*[:=]\\s*[^,\\s]+", "$1=[redacted]");
    }
}
