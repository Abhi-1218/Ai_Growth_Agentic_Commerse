package com.growthpilot.razorpay.controller;

import com.growthpilot.entity.Business;
import com.growthpilot.razorpay.client.RazorpayClientFactory;
import com.growthpilot.razorpay.service.RazorpaySyncService;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.repository.OrderRepository;
import com.growthpilot.repository.PaymentRepository;
import com.growthpilot.repository.RefundRepository;
import com.growthpilot.security.SecurityUtils;
import com.razorpay.RazorpayClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/razorpay")
public class RazorpayIntegrationController {

    private final RazorpaySyncService syncService;
    private final SecurityUtils securityUtils;
    private final BusinessRepository businessRepository;
    private final RazorpayClientFactory clientFactory;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public RazorpayIntegrationController(RazorpaySyncService syncService,
                                         SecurityUtils securityUtils,
                                         BusinessRepository businessRepository,
                                         RazorpayClientFactory clientFactory,
                                         CustomerRepository customerRepository,
                                         OrderRepository orderRepository,
                                         PaymentRepository paymentRepository,
                                         RefundRepository refundRepository) {
        this.syncService = syncService;
        this.securityUtils = securityUtils;
        this.businessRepository = businessRepository;
        this.clientFactory = clientFactory;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Business business = businessRepository.findById(businessId).orElseThrow();

        Map<String, Object> response = new HashMap<>();
        boolean isConfigured = business.getRazorpayKeyId() != null && !business.getRazorpayKeyId().isEmpty();
        response.put("configured", isConfigured);
        response.put("syncStatus", business.getSyncStatus());
        response.put("lastSyncAt", business.getLastSyncAt());
        
        long rzpCustomers = customerRepository.findAllByBusinessId(businessId).stream()
                .filter(c -> c.getRazorpayCustomerId() != null && !c.getRazorpayCustomerId().isEmpty())
                .count();
        long rzpOrders = orderRepository.findAllByBusinessId(businessId).stream()
                .filter(o -> o.getRazorpayOrderId() != null && !o.getRazorpayOrderId().isEmpty())
                .count();
        long totalPayments = paymentRepository.findAllByBusinessId(businessId).size();
        long totalRefunds = refundRepository.findAllByBusinessId(businessId).size();
        response.put("recordsSynchronized", rzpCustomers + rzpOrders + totalPayments + totalRefunds);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> payload) {
        String keyId = payload.get("keyId");
        String keySecret = payload.get("keySecret");
        
        Map<String, Object> response = new HashMap<>();
        try {
            RazorpayClient razorpay = clientFactory.getClient(keyId, keySecret);
            // Simple fetch to test auth
            razorpay.customers.fetchAll(new org.json.JSONObject().put("count", 1));
            response.put("success", true);
            response.put("message", "Connection successful!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Connection failed: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-credentials")
    public ResponseEntity<Map<String, Object>> saveCredentials(@RequestBody Map<String, String> payload) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        
        business.setRazorpayKeyId(payload.get("keyId"));
        business.setRazorpayKeySecret(payload.get("keySecret"));
        business.setRazorpayWebhookSecret(payload.get("webhookSecret"));
        businessRepository.save(business);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncNow() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        syncService.syncAll(businessId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Data synchronized successfully");
        return ResponseEntity.ok(response);
    }
}
