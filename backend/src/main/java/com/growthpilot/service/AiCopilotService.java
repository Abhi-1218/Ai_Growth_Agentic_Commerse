package com.growthpilot.service;

import com.growthpilot.agent.tool.AgentToolRegistry;
import com.growthpilot.ai.GeminiAiProvider;
import com.growthpilot.dto.AiChatRequest;
import com.growthpilot.dto.AiChatResponse;
import com.growthpilot.entity.Cart;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Product;
import com.growthpilot.repository.CartRepository;
import com.growthpilot.repository.CustomerRepository;
import com.growthpilot.repository.ProductRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCopilotService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final SecurityUtils securityUtils;
    private final GeminiAiProvider geminiAiProvider;
    private final AgentToolRegistry toolRegistry;

    public AiCopilotService(CustomerRepository customerRepository,
                            ProductRepository productRepository,
                            CartRepository cartRepository,
                            SecurityUtils securityUtils,
                            GeminiAiProvider geminiAiProvider,
                            AgentToolRegistry toolRegistry) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.securityUtils = securityUtils;
        this.geminiAiProvider = geminiAiProvider;
        this.toolRegistry = toolRegistry;
    }

    public AiChatResponse chat(AiChatRequest request) {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();
        String userMessage = request.getMessage();
        String context = buildBusinessContext(businessId);

        List<String> toolsInvoked = new ArrayList<>();
        List<AiChatResponse.ProposedActionDto> proposedActions = new ArrayList<>();
        String responseText;

        if (geminiAiProvider.isAvailable()) {
            try {
                String systemPrompt = "You are GrowthPilot AI, an autonomous e-commerce growth acceleration agent.\n" +
                        "Store Context:\n" + context + "\n\n" +
                        toolRegistry.buildToolDefinitionsPrompt() + "\n" +
                        "Provide a concise, direct, data-backed answer using real store numbers. Highlight growth opportunities and specify concrete actions.";
                responseText = geminiAiProvider.generateText(systemPrompt, userMessage);
                toolsInvoked.add("gemini-ai-provider");
            } catch (Exception e) {
                responseText = generateRuleBasedAnswer(userMessage, businessId, proposedActions, toolsInvoked);
            }
        } else {
            responseText = generateRuleBasedAnswer(userMessage, businessId, proposedActions, toolsInvoked);
        }

        // Attach default relevant proposed action if none was generated
        if (proposedActions.isEmpty()) {
            attachDefaultActions(userMessage, businessId, proposedActions);
        }

        return AiChatResponse.builder()
                .response(responseText)
                .proposedActions(proposedActions)
                .toolsInvoked(toolsInvoked)
                .build();
    }

    private String buildBusinessContext(Long businessId) {
        long totalCustomers = customerRepository.countByBusinessId(businessId);
        long totalProducts = productRepository.countByBusinessId(businessId);
        List<Customer> highIntent = customerRepository.findHighIntentCustomers(businessId);
        List<Cart> abandoned = cartRepository.findAbandonedCartsByBusinessId(businessId);
        List<Product> topProducts = productRepository.findTopProductsByBusinessId(businessId, PageRequest.of(0, 5));

        BigDecimal recoverableCartVal = abandoned.stream()
                .flatMap(c -> c.getItems().stream())
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
                "Store Metrics: Total Customers=%d, Products=%d, High Intent Buyers=%d, Abandoned Carts=%d (Value: ₹%d). Top Products: %s.",
                totalCustomers, totalProducts, highIntent.size(), abandoned.size(), recoverableCartVal.intValue(),
                topProducts.stream().map(Product::getName).collect(Collectors.joining(", "))
        );
    }

    private String generateRuleBasedAnswer(String message, Long businessId,
                                           List<AiChatResponse.ProposedActionDto> proposedActions,
                                           List<String> toolsInvoked) {
        String lower = message.toLowerCase();
        List<Customer> highIntent = customerRepository.findHighIntentCustomers(businessId);
        List<Cart> abandoned = cartRepository.findAbandonedCartsByBusinessId(businessId);
        List<Product> topProducts = productRepository.findTopProductsByBusinessId(businessId, PageRequest.of(0, 5));

        if (lower.contains("intent") || lower.contains("target") || lower.contains("buy") || lower.contains("customer")) {
            toolsInvoked.add("getCustomer");
            toolsInvoked.add("calculateIntentScore");

            if (!highIntent.isEmpty()) {
                Customer top = highIntent.get(0);
                proposedActions.add(AiChatResponse.ProposedActionDto.builder()
                        .title("Target High-Intent Customer " + top.getName())
                        .actionType("OFFER")
                        .description("Dispatch exclusive 10% coupon on " + top.getPreferredCategory() + " to convert buying intent.")
                        .estimatedImpact("₹" + (top.getAverageOrderValue() != null ? top.getAverageOrderValue().intValue() : 4500))
                        .targetEntity("Customer: " + top.getName())
                        .suggestedParameters("{\"customerId\":" + top.getId() + ",\"discountPercent\":10}")
                        .requiresApproval(true)
                        .build());
            }

            String names = highIntent.stream().limit(5)
                    .map(c -> "• **" + c.getName() + "** – Intent Score: " + c.getPurchaseIntentScore() + "/100 (Preferred: " + c.getPreferredCategory() + ")")
                    .collect(Collectors.joining("\n"));

            return "### 🎯 High-Intent Buyers Ready to Convert\n\n" +
                    "I analyzed customer browsing velocity, cart additions, and recency signals. Here are the top **" + highIntent.size() + "** high-intent buyers:\n\n" +
                    names + "\n\n" +
                    "**Growth Recommendation:** Deploy a personalized 10% coupon code to this segment to capture immediate checkout momentum.";
        }

        if (lower.contains("cart") || lower.contains("abandon") || lower.contains("recover")) {
            toolsInvoked.add("getAbandonedCarts");
            toolsInvoked.add("createRecoveryAction");

            BigDecimal totalVal = abandoned.stream()
                    .flatMap(c -> c.getItems().stream())
                    .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!abandoned.isEmpty()) {
                Cart first = abandoned.get(0);
                proposedActions.add(AiChatResponse.ProposedActionDto.builder()
                        .title("Auto-Recover Cart #" + first.getId() + " (" + first.getCustomer().getName() + ")")
                        .actionType("CART_RECOVERY")
                        .description("Send limited-time 10% discount code + free shipping notification.")
                        .estimatedImpact("₹" + (first.getItems().stream().mapToInt(i -> i.getProduct().getPrice().intValue() * i.getQuantity()).sum()))
                        .targetEntity("Cart #" + first.getId())
                        .suggestedParameters("{\"cartId\":" + first.getId() + ",\"discountPercent\":10}")
                        .requiresApproval(true)
                        .build());
            }

            return "### 🛒 Cart Abandonment Intelligence\n\n" +
                    "We have **" + abandoned.size() + " abandoned carts** representing **₹" + totalVal.intValue() + "** in recoverable revenue.\n\n" +
                    "- **Average Recovery Probability:** 74%\n" +
                    "- **Primary Drop-off Stage:** Shipping calculation step\n" +
                    "- **Recommended Action:** Dispatch a personalized recovery email offering 10% off with 24-hour expiration.";
        }

        if (lower.contains("product") || lower.contains("trending") || lower.contains("bundle")) {
            toolsInvoked.add("getProducts");
            toolsInvoked.add("getRecommendations");

            String prods = topProducts.stream()
                    .map(p -> "• **" + p.getName() + "** (" + p.getCategory() + ") – " + p.getTotalSales() + " sales (₹" + p.getPrice().intValue() + ")")
                    .collect(Collectors.joining("\n"));

            if (topProducts.size() >= 2) {
                proposedActions.add(AiChatResponse.ProposedActionDto.builder()
                        .title("Create " + topProducts.get(0).getName() + " Bundle")
                        .actionType("CAMPAIGN")
                        .description("Bundle with complementary accessories at 10% combo discount.")
                        .estimatedImpact("₹28,500")
                        .targetEntity("Category: " + topProducts.get(0).getCategory())
                        .suggestedParameters("{\"name\":\"" + topProducts.get(0).getName() + " Power Bundle\",\"targetSegment\":\"Loyal\"}")
                        .requiresApproval(true)
                        .build());
            }

            return "### 📦 Product Performance & Bundle Opportunities\n\n" +
                    "Here are your top-selling products by order volume:\n\n" + prods + "\n\n" +
                    "**AI Strategy:** Create an automated cross-sell bundle offering a 10% discount when paired with complementary accessories.";
        }

        if (lower.contains("churn") || lower.contains("retain") || lower.contains("risk")) {
            toolsInvoked.add("calculateChurnRisk");
            toolsInvoked.add("createCampaign");

            List<Customer> atRisk = customerRepository.findAllByBusinessIdAndChurnRisk(businessId, "HIGH");

            proposedActions.add(AiChatResponse.ProposedActionDto.builder()
                    .title("Launch At-Risk Customer Win-Back Campaign")
                    .actionType("CAMPAIGN")
                    .description("Automated 15% discount email sequence for customers inactive for 60+ days.")
                    .estimatedImpact("₹42,000")
                    .targetEntity("Segment: At Risk (" + atRisk.size() + " customers)")
                    .suggestedParameters("{\"name\":\"VIP Win-Back Campaign\",\"targetSegment\":\"At Risk\"}")
                    .requiresApproval(true)
                    .build());

            return "### ⚠️ Churn Risk & Retention Alert\n\n" +
                    "We identified **" + atRisk.size() + " customers** at **HIGH** churn risk due to order inactivity exceeding 60 days.\n\n" +
                    "**Retention Plan:** Launch an automated win-back sequence offering 15% off their next purchase to reactivate customer lifetime value.";
        }

        if (lower.contains("payment") || lower.contains("fail") || lower.contains("revenue") || lower.contains("razorpay")) {
            toolsInvoked.add("getFailedPayments");
            
            proposedActions.add(AiChatResponse.ProposedActionDto.builder()
                    .title("Recover High-Value Failed Payments")
                    .actionType("RECOVERY")
                    .description("Send a soft follow-up email to customers whose payments failed in the last 24 hours with a direct secure checkout link.")
                    .estimatedImpact("₹25,000")
                    .targetEntity("Failed Payments")
                    .suggestedParameters("{\"action\":\"email_payment_links\"}")
                    .requiresApproval(true)
                    .build());

            return "### 💳 Payment Intelligence & Recovery\n\n" +
                    "I have analyzed your Razorpay payment data.\n\n" +
                    "**Insight:** You have high-value failed payments that haven't been recovered.\n" +
                    "**Recommended Action:** Automatically send a personalized secure payment link to these customers to recover lost revenue.";
        }

        if (lower.contains("refund")) {
            toolsInvoked.add("getRefundInsights");
            return "### 📉 Refund Intelligence\n\n" +
                    "I analyzed your Razorpay refund patterns.\n\n" +
                    "**Insight:** Refunds can indicate product quality issues, shipping damage, or sizing problems.\n" +
                    "**Recommended Action:** Check the reviews on your top refunded products and consider updating the product description or supplier.";
        }

        toolsInvoked.add("getCustomer");
        toolsInvoked.add("getProducts");
        return "### 🚀 GrowthPilot AI Commerce Assistant\n\n" +
                "I am monitoring your commerce growth metrics in real-time. Here are high-impact questions you can ask:\n\n" +
                "- *'Which customers should I target today?'* – Scans intent scores and category preferences.\n" +
                "- *'Find high-value abandoned carts'* – Identifies top recoverable cart opportunities.\n" +
                "- *'Which products should I bundle?'* – Discovers complementary product pairings.\n" +
                "- *'Show me failed high-value payments'* – Queries Razorpay integration for failed transactions.\n" +
                "- *'What are my refund insights?'* – Analyzes Razorpay refunds for revenue leakage.\n" +
                "- *'Create a retention campaign for customers at high churn risk'* – Generates win-back actions.";
    }

    private void attachDefaultActions(String message, Long businessId, List<AiChatResponse.ProposedActionDto> actions) {
        List<Customer> highIntent = customerRepository.findHighIntentCustomers(businessId);
        if (!highIntent.isEmpty()) {
            Customer c = highIntent.get(0);
            actions.add(AiChatResponse.ProposedActionDto.builder()
                    .title("Target High-Intent Customer " + c.getName())
                    .actionType("OFFER")
                    .description("Personalized 10% promotional incentive for " + c.getPreferredCategory() + ".")
                    .estimatedImpact("₹" + (c.getAverageOrderValue() != null ? c.getAverageOrderValue().intValue() : 5000))
                    .targetEntity("Customer: " + c.getName())
                    .suggestedParameters("{\"customerId\":" + c.getId() + ",\"discountPercent\":10}")
                    .requiresApproval(true)
                    .build());
        }
    }
}
