package com.growthpilot.service;

import com.growthpilot.dto.*;
import com.growthpilot.entity.Customer;
import com.growthpilot.entity.Order;
import com.growthpilot.entity.Product;
import com.growthpilot.repository.*;
import com.growthpilot.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final GrowthOpportunityRepository opportunityRepository;
    private final AgentActionRepository agentActionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SecurityUtils securityUtils;

    public DashboardService(CustomerRepository customerRepository, ProductRepository productRepository,
                             OrderRepository orderRepository, CartRepository cartRepository,
                             GrowthOpportunityRepository opportunityRepository,
                             AgentActionRepository agentActionRepository, 
                             PaymentRepository paymentRepository,
                             RefundRepository refundRepository,
                             SecurityUtils securityUtils) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.opportunityRepository = opportunityRepository;
        this.agentActionRepository = agentActionRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getExecutiveSummary() {
        Long businessId = securityUtils.getCurrentUser().getBusiness().getId();

        long totalCustomers = customerRepository.countByBusinessId(businessId);
        long totalOrders = orderRepository.countByBusinessId(businessId);
        long totalProducts = productRepository.countByBusinessId(businessId);
        // Base revenue from internal orders
        BigDecimal baseRevenue = orderRepository.sumTotalRevenueByBusinessId(businessId);
        if (baseRevenue == null) baseRevenue = BigDecimal.ZERO;

        // Add revenue from standalone captured payments (no internal order link)
        BigDecimal standaloneRevenue = paymentRepository.findAllByBusinessId(businessId).stream()
                .filter(p -> p.getCaptured() != null && p.getCaptured() && p.getOrder() == null)
                .map(com.growthpilot.entity.Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRevenue = baseRevenue.add(standaloneRevenue);

        // Deduct refunds
        BigDecimal totalRefundAmount = refundRepository.findAllByBusinessId(businessId).stream()
                .map(com.growthpilot.entity.Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalRevenue = totalRevenue.subtract(totalRefundAmount);

        // Add standalone payments to total orders count
        long standaloneOrders = paymentRepository.findAllByBusinessId(businessId).stream()
                .filter(p -> p.getCaptured() != null && p.getCaptured() && p.getOrder() == null)
                .count();
        totalOrders += standaloneOrders;

        long abandonedCarts = cartRepository.countByBusinessIdAndStatus(businessId, "ABANDONED");
        long highIntentCustomers = customerRepository.findHighIntentCustomers(businessId).size();
        long growthOpportunities = opportunityRepository.countByBusinessIdAndStatus(businessId, "PENDING");
        long pendingApprovals = agentActionRepository.countByBusinessIdAndStatus(businessId, "PENDING_APPROVAL");

        // Recoverable cart revenue
        BigDecimal recoverableCartRevenue = cartRepository.findAbandonedCartsByBusinessId(businessId).stream()
                .flatMap(c -> c.getItems().stream())
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue at risk (high churn customers' avg order value * 3)
        BigDecimal revenueAtRisk = customerRepository.findAllByBusinessIdAndChurnRisk(businessId, "HIGH").stream()
                .map(c -> c.getAverageOrderValue() != null ? c.getAverageOrderValue().multiply(BigDecimal.valueOf(3)) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Average order value
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Active & Repeat Customers
        List<Customer> allCustomers = customerRepository.findAllByBusinessId(businessId);
        long activeCustomers = allCustomers.stream()
                .filter(c -> c.getLastOrderDate() != null && c.getLastOrderDate().isAfter(LocalDateTime.now().minusDays(60)))
                .count();
        long repeatCustomers = allCustomers.stream()
                .filter(c -> c.getTotalOrders() > 1)
                .count();

        // 1. Revenue & Orders Trend (Last 6 months / weeks)
        List<Order> allOrders = orderRepository.findAllByBusinessId(businessId);
        Map<String, List<Order>> ordersByMonth = allOrders.stream()
                .filter(o -> o.getOrderDate() != null)
                .collect(Collectors.groupingBy(o -> o.getOrderDate().format(DateTimeFormatter.ofPattern("MMM yyyy"))));

        List<com.growthpilot.entity.Payment> standalonePayments = paymentRepository.findAllByBusinessId(businessId).stream()
                .filter(p -> p.getCaptured() != null && p.getCaptured() && p.getOrder() == null && p.getCreatedAt() != null)
                .collect(Collectors.toList());
        Map<String, List<com.growthpilot.entity.Payment>> paymentsByMonth = standalonePayments.stream()
                .collect(Collectors.groupingBy(p -> p.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy"))));

        // Create ordered monthly trend points
        List<TrendDataPoint> revenueTrend = new ArrayList<>();
        List<TrendDataPoint> ordersTrend = new ArrayList<>();

        // Generate default 6 month time series if orders are clustered
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthTarget = LocalDateTime.now().minusMonths(i);
            String monthKey = monthTarget.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            List<Order> monthOrders = ordersByMonth.getOrDefault(monthKey, Collections.emptyList());
            List<com.growthpilot.entity.Payment> monthPayments = paymentsByMonth.getOrDefault(monthKey, Collections.emptyList());

            BigDecimal monthRev = monthOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paymentsRev = monthPayments.stream()
                    .map(com.growthpilot.entity.Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalMonthRev = monthRev.add(paymentsRev);
            int totalMonthCount = monthOrders.size() + monthPayments.size();

            revenueTrend.add(new TrendDataPoint(monthKey, totalMonthRev, totalMonthCount));
            ordersTrend.add(new TrendDataPoint(monthKey, totalMonthRev, totalMonthCount));
        }

        // 2. Customer Segments Distribution
        String[] segmentNames = {"VIP", "Loyal", "New Customer", "High Intent", "At Risk", "Dormant", "Cart Abandoner"};
        List<SegmentDistributionDto> segments = new ArrayList<>();
        for (String seg : segmentNames) {
            long count = allCustomers.stream().filter(c -> seg.equalsIgnoreCase(c.getSegment())).count();
            double pct = totalCustomers > 0 ? ((double) count / totalCustomers) * 100.0 : 0.0;
            segments.add(new SegmentDistributionDto(seg, count, Math.round(pct * 10.0) / 10.0));
        }

        // 3. Top Products
        List<Product> topProds = productRepository.findTopProductsByBusinessId(businessId, PageRequest.of(0, 5));
        List<DashboardSummaryDto.ProductSummaryDto> topProductDtos = topProds.stream().map(p -> {
            int sales = p.getTotalSales() != null ? p.getTotalSales() : 0;
            int views = p.getViews() != null ? p.getViews() : 0;
            double conv = views > 0 ? ((double) sales / views) * 100.0 : 0.0;
            BigDecimal rev = p.getPrice().multiply(BigDecimal.valueOf(sales));
            return DashboardSummaryDto.ProductSummaryDto.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .category(p.getCategory())
                    .price(p.getPrice())
                    .totalSales(sales)
                    .totalRevenue(rev)
                    .views(views)
                    .cartAdditions(p.getCartAdditions() != null ? p.getCartAdditions() : 0)
                    .conversionRate(Math.round(conv * 100.0) / 100.0)
                    .build();
        }).collect(Collectors.toList());

        // 4. Conversion Funnel
        long totalVisitors = totalCustomers * 85;
        long productViews = topProds.stream().mapToInt(p -> p.getViews() != null ? p.getViews() : 0).sum() + totalVisitors / 2;
        long cartAdditions = topProds.stream().mapToInt(p -> p.getCartAdditions() != null ? p.getCartAdditions() : 0).sum() + (totalOrders * 2);
        long checkoutAttempts = totalOrders + abandonedCarts;
        long purchases = totalOrders;

        List<FunnelStageDto> funnel = List.of(
                new FunnelStageDto("Store Visitors", totalVisitors, 100.0),
                new FunnelStageDto("Product Views", productViews, Math.round(((double) productViews / totalVisitors) * 1000.0) / 10.0),
                new FunnelStageDto("Cart Additions", cartAdditions, Math.round(((double) cartAdditions / productViews) * 1000.0) / 10.0),
                new FunnelStageDto("Checkout Started", checkoutAttempts, Math.round(((double) checkoutAttempts / cartAdditions) * 1000.0) / 10.0),
                new FunnelStageDto("Purchased", purchases, Math.round(((double) purchases / checkoutAttempts) * 1000.0) / 10.0)
        );

        double overallConv = totalVisitors > 0 ? ((double) purchases / totalVisitors) * 100.0 : 3.2;
        double cartRecov = (checkoutAttempts > 0) ? ((double) (checkoutAttempts - abandonedCarts) / checkoutAttempts) * 100.0 : 42.5;

        return DashboardSummaryDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .averageOrderValue(avgOrderValue)
                .activeCustomers(activeCustomers)
                .repeatCustomers(repeatCustomers)
                .abandonedCarts(abandonedCarts)
                .recoverableCartRevenue(recoverableCartRevenue)
                .revenueAtRisk(revenueAtRisk)
                .highIntentCustomers(highIntentCustomers)
                .growthOpportunities(growthOpportunities)
                .pendingApprovals(pendingApprovals)
                .overallConversionRate(Math.round(overallConv * 100.0) / 100.0)
                .cartRecoveryRate(Math.round(cartRecov * 100.0) / 100.0)
                .revenueTrend(revenueTrend)
                .ordersTrend(ordersTrend)
                .customerSegments(segments)
                .topProducts(topProductDtos)
                .conversionFunnel(funnel)
                .failedPayments(paymentRepository.countByBusinessIdAndStatus(businessId, "failed"))
                .successfulPayments(paymentRepository.countByBusinessIdAndStatus(businessId, "captured"))
                .refunds(refundRepository.findAllByBusinessId(businessId).size())
                .totalRefundAmount(refundRepository.findAllByBusinessId(businessId).stream().map(com.growthpilot.entity.Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }
}
