package com.growthpilot.razorpay.service;

import com.growthpilot.entity.*;
import com.growthpilot.razorpay.client.RazorpayClientFactory;
import com.growthpilot.repository.*;
import com.razorpay.Customer;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class RazorpaySyncService {

    // Helper methods to safely extract values from Razorpay JSON responses
    private String getString(com.razorpay.Entity obj, String key) {
        if (!obj.has(key) || obj.toJson().isNull(key)) return null;
        Object val = obj.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(com.razorpay.Entity obj, String key) {
        if (!obj.has(key) || obj.toJson().isNull(key)) return null;
        Object val = obj.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    private Boolean getBoolean(com.razorpay.Entity obj, String key) {
        if (!obj.has(key) || obj.toJson().isNull(key)) return null;
        Object val = obj.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return null;
    }

    private LocalDateTime getDate(com.razorpay.Entity obj, String key) {
        if (!obj.has(key) || obj.toJson().isNull(key)) return null;
        Object val = obj.get(key);
        if (val instanceof java.util.Date) {
            return ((java.util.Date) val).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (val instanceof Number) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(((Number) val).longValue()), ZoneId.systemDefault());
        }
        return LocalDateTime.now();
    }

    private final RazorpayClientFactory clientFactory;
    private final BusinessRepository businessRepository;
    private final com.growthpilot.repository.CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    @Value("${razorpay.key.id:}")
    private String defaultKeyId;

    @Value("${razorpay.key.secret:}")
    private String defaultKeySecret;

    public RazorpaySyncService(RazorpayClientFactory clientFactory,
                               BusinessRepository businessRepository,
                               com.growthpilot.repository.CustomerRepository customerRepository,
                               OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               RefundRepository refundRepository) {
        this.clientFactory = clientFactory;
        this.businessRepository = businessRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public void syncAll(Long businessId) {
        Business business = businessRepository.findById(businessId).orElseThrow(() -> new RuntimeException("Business not found"));
        String keyId = business.getRazorpayKeyId() != null ? business.getRazorpayKeyId() : defaultKeyId;
        String keySecret = business.getRazorpayKeySecret() != null ? business.getRazorpayKeySecret() : defaultKeySecret;

        if (keyId == null || keyId.isEmpty() || keySecret == null || keySecret.isEmpty()) {
            throw new RuntimeException("Razorpay credentials not configured for this business");
        }

        try {
            RazorpayClient razorpay = clientFactory.getClient(keyId, keySecret);
            syncCustomers(business, razorpay);
            syncOrders(business, razorpay);
            syncPayments(business, razorpay);
            syncRefunds(business, razorpay);

            business.setLastSyncAt(LocalDateTime.now());
            business.setSyncStatus("SUCCESS");
            businessRepository.save(business);
        } catch (Exception e) {
            log.error("Razorpay sync failed for business {}", businessId, e);
            business.setSyncStatus("FAILED");
            businessRepository.save(business);
            throw new RuntimeException("Sync failed: " + e.getMessage());
        }
    }

    private void syncCustomers(Business business, RazorpayClient razorpay) throws Exception {
        JSONObject options = new JSONObject();
        options.put("count", 100);
        List<Customer> rzpCustomers = razorpay.customers.fetchAll(options);
        
        for (Customer c : rzpCustomers) {
            String rzpId = getString(c, "id");
            String email = getString(c, "email");
            String name = getString(c, "name") != null ? getString(c, "name") : email;
            String contact = getString(c, "contact");
            
            // Check if customer exists by email or rzp ID
            com.growthpilot.entity.Customer existing = customerRepository.findAllByBusinessId(business.getId()).stream()
                    .filter(cust -> (cust.getRazorpayCustomerId() != null && cust.getRazorpayCustomerId().equals(rzpId)) 
                                 || cust.getEmail().equals(email))
                    .findFirst().orElse(null);

            if (existing == null) {
                existing = com.growthpilot.entity.Customer.builder()
                        .business(business)
                        .name(name)
                        .email(email)
                        .phone(contact)
                        .razorpayCustomerId(rzpId)
                        .createdAt(LocalDateTime.now())
                        .build();
                customerRepository.save(existing);
            } else if (existing.getRazorpayCustomerId() == null) {
                existing.setRazorpayCustomerId(rzpId);
                customerRepository.save(existing);
            }
        }
    }

    private void syncOrders(Business business, RazorpayClient razorpay) throws Exception {
        JSONObject options = new JSONObject();
        options.put("count", 100);
        List<Order> rzpOrders = razorpay.orders.fetchAll(options);

        for (Order o : rzpOrders) {
            String rzpId = getString(o, "id");
            Integer amount = getInt(o, "amount");
            String currency = getString(o, "currency");
            String status = getString(o, "status");
            String receipt = getString(o, "receipt");

            com.growthpilot.entity.Order existing = orderRepository.findAllByBusinessId(business.getId()).stream()
                    .filter(ord -> ord.getRazorpayOrderId() != null && ord.getRazorpayOrderId().equals(rzpId))
                    .findFirst().orElse(null);

            if (existing == null) {
                // If it doesn't match an existing internal order, we could create it,
                // but we need a customer. Razorpay orders don't always contain customer ID natively.
                // For this platform, we only sync Razorpay status for orders that were initiated by us,
                // or we try to map via receipt.
                // Simplification: if not found, we skip or link to a default "Guest" if needed.
                // For now, we only update existing matched by receipt if present.
                if (receipt != null) {
                    try {
                        Long orderId = Long.parseLong(receipt);
                        com.growthpilot.entity.Order byId = orderRepository.findById(orderId).orElse(null);
                        if (byId != null) {
                            byId.setRazorpayOrderId(rzpId);
                            byId.setStatus(status);
                            byId.setCurrency(currency);
                            orderRepository.save(byId);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                existing.setStatus(status);
                orderRepository.save(existing);
            }
        }
    }

    private void syncPayments(Business business, RazorpayClient razorpay) throws Exception {
        JSONObject options = new JSONObject();
        options.put("count", 100);
        List<Payment> rzpPayments = razorpay.payments.fetchAll(options);

        for (Payment p : rzpPayments) {
            String rzpId = getString(p, "id");
            String rzpOrderId = getString(p, "order_id");
            Integer amount = getInt(p, "amount");
            String currency = getString(p, "currency");
            String status = getString(p, "status");
            String method = getString(p, "method");
            Boolean captured = getBoolean(p, "captured");
            String errorReason = getString(p, "error_reason");
            LocalDateTime createdAt = getDate(p, "created_at");
            if (createdAt == null) createdAt = LocalDateTime.now();

            com.growthpilot.entity.Payment existing = paymentRepository.findByRazorpayPaymentId(rzpId).orElse(null);

            if (existing == null) {
                com.growthpilot.entity.Order order = null;
                if (rzpOrderId != null) {
                    order = orderRepository.findAllByBusinessId(business.getId()).stream()
                            .filter(ord -> ord.getRazorpayOrderId() != null && ord.getRazorpayOrderId().equals(rzpOrderId))
                            .findFirst().orElse(null);
                }

                existing = com.growthpilot.entity.Payment.builder()
                        .business(business)
                        .order(order)
                        .razorpayPaymentId(rzpId)
                        .amount(amount != null ? BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO)
                        .currency(currency)
                        .status(status)
                        .method(method)
                        .captured(captured)
                        .errorReason(errorReason)
                        .createdAt(createdAt)
                        .build();
                paymentRepository.save(existing);
            } else {
                existing.setStatus(status);
                existing.setCaptured(captured);
                existing.setErrorReason(errorReason);
                paymentRepository.save(existing);
            }
        }
    }

    private void syncRefunds(Business business, RazorpayClient razorpay) throws Exception {
        JSONObject options = new JSONObject();
        options.put("count", 100);
        List<Refund> rzpRefunds = razorpay.refunds.fetchAll(options);

        for (Refund r : rzpRefunds) {
            String rzpId = getString(r, "id");
            String rzpPaymentId = getString(r, "payment_id");
            Integer amount = getInt(r, "amount");
            String currency = getString(r, "currency");
            String status = getString(r, "status");
            LocalDateTime createdAt = getDate(r, "created_at");
            if (createdAt == null) createdAt = LocalDateTime.now();

            com.growthpilot.entity.Refund existing = refundRepository.findByRazorpayRefundId(rzpId).orElse(null);

            if (existing == null) {
                com.growthpilot.entity.Payment payment = null;
                com.growthpilot.entity.Order order = null;

                if (rzpPaymentId != null) {
                    payment = paymentRepository.findByRazorpayPaymentId(rzpPaymentId).orElse(null);
                    if (payment != null) {
                        order = payment.getOrder();
                    }
                }

                existing = com.growthpilot.entity.Refund.builder()
                        .business(business)
                        .payment(payment)
                        .order(order)
                        .razorpayRefundId(rzpId)
                        .amount(amount != null ? BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO)
                        .currency(currency)
                        .status(status)
                        .createdAt(createdAt)
                        .build();
                refundRepository.save(existing);
            } else {
                existing.setStatus(status);
                refundRepository.save(existing);
            }
        }
    }
}
