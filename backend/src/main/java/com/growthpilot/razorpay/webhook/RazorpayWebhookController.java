package com.growthpilot.razorpay.webhook;

import com.growthpilot.entity.WebhookEvent;
import com.growthpilot.razorpay.service.RazorpaySyncService;
import com.growthpilot.service.CheckoutService;
import com.growthpilot.repository.BusinessRepository;
import com.growthpilot.repository.WebhookEventRepository;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {

    private final WebhookEventRepository webhookEventRepository;
    private final BusinessRepository businessRepository;
    private final RazorpaySyncService syncService;
    private final CheckoutService checkoutService;

    // Ideally, we'd lookup the business by the webhook account_id, but for single-tenant or default setups:
    @org.springframework.beans.factory.annotation.Value("${razorpay.webhook.secret:}")
    private String defaultWebhookSecret;

    public RazorpayWebhookController(WebhookEventRepository webhookEventRepository,
                                     BusinessRepository businessRepository,
                                     RazorpaySyncService syncService,
                                     CheckoutService checkoutService) {
        this.webhookEventRepository = webhookEventRepository;
        this.businessRepository = businessRepository;
        this.syncService = syncService;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String payload) {
        
        try {
            JSONObject json = new JSONObject(payload);
            
            Optional<com.growthpilot.entity.Business> businessOpt = businessRepository.findAll().stream()
                    .filter(b -> b.getRazorpayWebhookSecret() != null && !b.getRazorpayWebhookSecret().isBlank())
                    .filter(b -> {
                        try { return Utils.verifyWebhookSignature(payload, signature, b.getRazorpayWebhookSecret()); }
                        catch (Exception ignored) { return false; }
                    }).findFirst();
            if (businessOpt.isEmpty() && defaultWebhookSecret != null && !defaultWebhookSecret.isBlank()
                    && Utils.verifyWebhookSignature(payload, signature, defaultWebhookSecret)) {
                businessOpt = businessRepository.findAll().stream().findFirst();
            }
            if (businessOpt.isEmpty()) {
                log.warn("Invalid Razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            // Prevent Duplicates
            String eventId = json.has("id") ? json.getString("id") : "UNKNOWN_" + System.currentTimeMillis();
            if (webhookEventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook event ignored: {}", eventId);
                return ResponseEntity.ok("Duplicate ignored");
            }

            String eventType = json.has("event") ? json.getString("event") : "unknown";
            
            // Save Event
            WebhookEvent event = WebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .processedAt(LocalDateTime.now())
                    .status("PROCESSING")
                    .build();
            webhookEventRepository.save(event);

            // Process based on event
            try {
                processEvent(eventType, json, businessOpt.get());
                event.setStatus("PROCESSED");
                webhookEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to process webhook event {}", eventId, e);
                event.setStatus("FAILED");
                webhookEventRepository.save(event);
                // Return 200 anyway so Razorpay doesn't infinitely retry unless it's a critical infrastructure failure
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void processEvent(String eventType, JSONObject payload, com.growthpilot.entity.Business business) {
        if (eventType.startsWith("payment.")) {
            JSONObject entity = payload.optJSONObject("payload") == null ? null
                    : payload.optJSONObject("payload").optJSONObject("payment") == null ? null
                    : payload.optJSONObject("payload").optJSONObject("payment").optJSONObject("entity");
            if (entity != null) {
                checkoutService.applyWebhookPayment(entity.optString("id", null), entity.optString("order_id", null),
                        entity.optString("status", "unknown"), entity.optBoolean("captured", false), business);
                return;
            }
        }
        // Find the business context. In a real app, Razorpay payload contains the account_id.
        // For GrowthPilot, we will trigger a full sync for the first configured business, 
        // or a specific business if we had the account mapping.
        if (business.getRazorpayKeyId() != null) {
            // A lightweight approach: trigger incremental sync via the sync service
            // rather than manually parsing the entity JSON from the webhook payload.
            syncService.syncAll(business.getId());
        }
    }
}
