package com.growthpilot.service;

import com.growthpilot.entity.*;
import com.growthpilot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.Ordered;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@org.springframework.core.annotation.Order(Ordered.LOWEST_PRECEDENCE)
public class DataSeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeedService.class);

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CustomerEventRepository customerEventRepository;
    private final CampaignRepository campaignRepository;
    private final AgentActionRepository agentActionRepository;
    private final GrowthOpportunityRepository opportunityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeedService(BusinessRepository businessRepository,
                           UserRepository userRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           OrderRepository orderRepository,
                           CartRepository cartRepository,
                           CustomerEventRepository customerEventRepository,
                           CampaignRepository campaignRepository,
                           AgentActionRepository agentActionRepository,
                           GrowthOpportunityRepository opportunityRepository,
                           PasswordEncoder passwordEncoder) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.customerEventRepository = customerEventRepository;
        this.campaignRepository = campaignRepository;
        this.agentActionRepository = agentActionRepository;
        this.opportunityRepository = opportunityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (businessRepository.count() > 0) {
            log.info("Database already seeded. Skipping.");
            return;
        }
        log.info("Seeding database with realistic e-commerce demo data...");

        Business business = businessRepository.save(Business.builder()
                .name("TechMart India")
                .description("Premier Electronics & Lifestyle D2C Commerce Platform")
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build());

        User admin = userRepository.save(User.builder()
                .email("admin@techmart.in")
                .password(passwordEncoder.encode("password123"))
                .role("ADMIN")
                .business(business)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build());

        userRepository.save(User.builder()
                .email("manager@techmart.in")
                .password(passwordEncoder.encode("password123"))
                .role("MANAGER")
                .business(business)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build());

        seedBusinessData(business, admin);
    }

    @Transactional
    public void seedBusinessData(Business business, User user) {
        log.info("Seeding e-commerce catalog and activity for business '{}'...", business.getName());

        // --- Products ---
        List<Product> products = createProducts(business);

        // --- Customers ---
        List<Customer> customers = createCustomers(business);

        // --- Orders ---
        createOrders(business, customers, products);

        // --- Abandoned Carts ---
        createAbandonedCarts(business, customers, products);

        // --- Customer Events ---
        createCustomerEvents(customers, products);

        // --- Campaigns ---
        createCampaigns(business);

        // --- Agent Actions (Audit Log) ---
        createAgentActions(business, user, customers);

        // --- Growth Opportunities ---
        createGrowthOpportunities(business, customers, products);

        log.info("Seeding complete. Business: '{}', Customers: {}, Products: {}",
                business.getName(), customers.size(), products.size());
    }

    private List<Product> createProducts(Business business) {
        String[][] productData = {
            {"iPhone 15 Pro", "Electronics", "129999"},
            {"Samsung Galaxy S24 Ultra", "Electronics", "119999"},
            {"Sony WH-1000XM5 Headphones", "Electronics", "24999"},
            {"Dell XPS 15 OLED Laptop", "Electronics", "189999"},
            {"Apple Watch Series 9 GPS", "Electronics", "41900"},
            {"JBL Flip 6 Waterproof Speaker", "Electronics", "8999"},
            {"Realme Buds Air 5 ANC", "Electronics", "3499"},
            {"Boat Bassheads 100 Earphones", "Electronics", "699"},
            {"Nikon Z6 III Mirrorless Camera", "Electronics", "229000"},
            {"Canon EOS R50 Content Creator Kit", "Electronics", "79990"},
            {"Nike Air Max 270 Sneakers", "Footwear", "10995"},
            {"Adidas Ultraboost Light", "Footwear", "14999"},
            {"Puma RS-X Retro Sneakers", "Footwear", "5999"},
            {"Woodland Leather Trekking Boots", "Footwear", "3999"},
            {"Levi's 511 Slim Fit Stretch Jeans", "Clothing", "3299"},
            {"US Polo Assn Solid Pique Polo", "Clothing", "1299"},
            {"Allen Solly Slim Formal Shirt", "Clothing", "1899"},
            {"Titan Octane Chronograph Watch", "Accessories", "8495"},
            {"Fossil Gen 6 Hybrid Smartwatch", "Electronics", "22995"},
            {"Wildcraft 35L Adventure Backpack", "Accessories", "2499"},
            {"Prestige Induction Cooktop 2000W", "Appliances", "3500"},
            {"Philips Digital Air Fryer HD9200", "Appliances", "8499"},
            {"Instant Pot Duo 7-in-1 Multi-Cooker", "Appliances", "12999"},
            {"Himalaya Purifying Neem Face Wash 150ml", "Beauty", "275"},
            {"Lakme 9 to 5 Primer + Matte Foundation", "Beauty", "525"},
            {"Biotique Bio Morning Nectar Moisturizer", "Beauty", "349"},
            {"Bombay Shaving Company Premium Kit", "Beauty", "1499"},
            {"Classmate Spiral Notebook 6-Pack", "Stationery", "450"},
            {"Parker Sonnet Fountain Pen Set", "Stationery", "2499"},
            {"LG 43\" 4K Ultra HD Smart LED TV", "Electronics", "34990"}
        };

        List<Product> products = new ArrayList<>();
        Random rng = new Random(42);
        for (String[] pd : productData) {
            Product p = productRepository.save(Product.builder()
                    .name(pd[0])
                    .category(pd[1])
                    .brand(pd[0].split(" ")[0])
                    .sku("TM-" + String.format("%03d", products.size() + 1))
                    .description(descriptionFor(pd[0], pd[1]))
                    .imageUrl(imageUrlFor(pd[0], pd[1]))
                    .metadata("{\"source\":\"TechMart verified catalog\",\"warranty\":\"1 year manufacturer warranty\",\"returns\":\"7-day easy returns\"}")
                    .price(new BigDecimal(pd[2]))
                    .stock(rng.nextInt(80) + 15)
                    .totalSales(rng.nextInt(320) + 12)
                    .views(rng.nextInt(2500) + 300)
                    .cartAdditions(rng.nextInt(500) + 50)
                    .business(business)
                    .createdAt(LocalDateTime.now().minusMonths(rng.nextInt(5) + 1))
                    .build());
            products.add(p);
        }
        return products;
    }

    private String descriptionFor(String name, String category) {
        return switch (category) {
            case "Electronics" -> name + " with dependable performance, premium materials and a manufacturer-backed warranty.";
            case "Footwear" -> name + " made for all-day comfort with durable construction and a secure fit.";
            case "Clothing" -> name + " crafted from comfortable materials with an easy everyday fit.";
            case "Accessories" -> name + " designed with practical details to keep up with everyday use.";
            case "Appliances" -> name + " built for efficient, reliable results in the modern home.";
            case "Beauty" -> name + " made with carefully selected ingredients for a simple daily routine.";
            default -> name + " selected by TechMart for quality, value and everyday usefulness.";
        };
    }

    private String imageUrlFor(String name, String category) {
        String photo = switch (category) {
            case "Footwear" -> "photo-1542291026-7eec264c27ff";
            case "Clothing" -> "photo-1521572163474-6864f9cf17ab";
            case "Accessories" -> "photo-1553062407-98eeb64c6a62";
            case "Appliances" -> "photo-1585515320310-259814833e62";
            case "Beauty" -> "photo-1598440947619-2c35fc9aa908";
            case "Stationery" -> "photo-1455390582262-044cdead277a";
            default -> name.toLowerCase().contains("headphone") || name.toLowerCase().contains("earphone")
                    ? "photo-1505740420928-5e560c06d30e"
                    : name.toLowerCase().contains("laptop") ? "photo-1496181133206-80ce9b88a853"
                    : name.toLowerCase().contains("watch") ? "photo-1523275335684-37898b6baf30"
                    : name.toLowerCase().contains("camera") ? "photo-1516035069371-29a1b244cc32"
                    : name.toLowerCase().contains("speaker") ? "photo-1608043152269-423dbba4e7e1"
                    : "photo-1511707171634-5f897ff02aa9";
        };
        return "https://images.unsplash.com/" + photo + "?auto=format&fit=crop&w=900&q=85";
    }

    private List<Customer> createCustomers(Business business) {
        String[][] data = {
            {"Rahul Sharma","rahul.sharma@gmail.com","Electronics","VIP"},
            {"Priya Mehta","priya.mehta@yahoo.com","Beauty","Loyal"},
            {"Amit Patel","amit.patel@outlook.com","Electronics","High Intent"},
            {"Sneha Kapoor","sneha.kapoor@gmail.com","Clothing","Cart Abandoner"},
            {"Vikram Singh","vikram.singh@hotmail.com","Electronics","VIP"},
            {"Anjali Verma","anjali.verma@gmail.com","Beauty","Loyal"},
            {"Rohit Kumar","rohit.kumar@gmail.com","Appliances","At Risk"},
            {"Nisha Joshi","nisha.joshi@yahoo.com","Footwear","New Customer"},
            {"Siddharth Rao","siddharth.rao@gmail.com","Electronics","VIP"},
            {"Divya Nair","divya.nair@outlook.com","Clothing","High Intent"},
            {"Karan Malhotra","karan.malhotra@gmail.com","Electronics","Loyal"},
            {"Pooja Iyer","pooja.iyer@gmail.com","Beauty","Cart Abandoner"},
            {"Arjun Bose","arjun.bose@hotmail.com","Electronics","High Intent"},
            {"Meera Pillai","meera.pillai@gmail.com","Accessories","Dormant"},
            {"Rajesh Gupta","rajesh.gupta@yahoo.com","Appliances","At Risk"},
            {"Sunita Tiwari","sunita.tiwari@gmail.com","Clothing","New Customer"},
            {"Manish Aggarwal","manish.aggarwal@gmail.com","Electronics","VIP"},
            {"Ritu Saxena","ritu.saxena@outlook.com","Beauty","Loyal"},
            {"Gaurav Jain","gaurav.jain@gmail.com","Stationery","Dormant"},
            {"Asha Bhatt","asha.bhatt@yahoo.com","Footwear","At Risk"},
            {"Vivek Pandey","vivek.pandey@gmail.com","Electronics","High Intent"},
            {"Kavita Sharma","kavita.sharma@gmail.com","Clothing","Cart Abandoner"},
            {"Arun Mishra","arun.mishra@outlook.com","Appliances","Loyal"},
            {"Geeta Rani","geeta.rani@gmail.com","Beauty","New Customer"},
            {"Suresh Nambiar","suresh.nambiar@hotmail.com","Electronics","VIP"},
            {"Lakshmi Devi","lakshmi.devi@gmail.com","Accessories","At Risk"},
            {"Harish Chandra","harish.chandra@yahoo.com","Electronics","High Intent"},
            {"Poonam Garg","poonam.garg@gmail.com","Beauty","Loyal"},
            {"Deepak Thakur","deepak.thakur@outlook.com","Electronics","Cart Abandoner"},
            {"Sanjay Verma","sanjay.verma@gmail.com","Footwear","New Customer"},
            {"Anita Roy","anita.roy@gmail.com","Clothing","VIP"},
            {"Sunil Khanna","sunil.khanna@yahoo.com","Electronics","High Intent"},
            {"Rekha Agarwal","rekha.agarwal@gmail.com","Beauty","Dormant"},
            {"Manoj Srivastava","manoj.srivastava@hotmail.com","Appliances","At Risk"},
            {"Shilpa Dubey","shilpa.dubey@gmail.com","Clothing","Loyal"},
            {"Vinay Kumar","vinay.kumar@outlook.com","Electronics","VIP"},
            {"Priyanka Sen","priyanka.sen@gmail.com","Beauty","Cart Abandoner"},
            {"Ramesh Yadav","ramesh.yadav@yahoo.com","Stationery","New Customer"},
            {"Usha Menon","usha.menon@gmail.com","Footwear","High Intent"},
            {"Nikhil Joshi","nikhil.joshi@gmail.com","Electronics","Loyal"},
            {"Swati Desai","swati.desai@outlook.com","Clothing","VIP"},
            {"Devendra Patil","devendra.patil@gmail.com","Appliances","At Risk"},
            {"Pallavi Kulkarni","pallavi.kulkarni@yahoo.com","Beauty","Dormant"},
            {"Aakash Reddy","aakash.reddy@gmail.com","Electronics","High Intent"},
            {"Tanvi Shah","tanvi.shah@hotmail.com","Accessories","Cart Abandoner"},
            {"Bharat Lal","bharat.lal@gmail.com","Electronics","VIP"},
            {"Jyoti Tripathi","jyoti.tripathi@outlook.com","Clothing","Loyal"},
            {"Girish Shetty","girish.shetty@gmail.com","Electronics","High Intent"},
            {"Nalini Rao","nalini.rao@yahoo.com","Beauty","At Risk"},
            {"Vishal Chopra","vishal.chopra@gmail.com","Electronics","VIP"}
        };

        Random rng = new Random(99);
        List<Customer> customers = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            String segment = data[i][3];
            int totalOrders = "VIP".equals(segment) ? 12 + rng.nextInt(15) : ("New Customer".equals(segment) ? 1 : 2 + rng.nextInt(8));
            BigDecimal avgOrder = BigDecimal.valueOf(3500 + rng.nextInt(18000));
            BigDecimal totalSpend = avgOrder.multiply(BigDecimal.valueOf(totalOrders));

            int intentScore;
            String churnRisk;
            int daysSinceLast;

            if ("High Intent".equals(segment)) {
                intentScore = 80 + rng.nextInt(18);
                churnRisk = "LOW";
                daysSinceLast = 2 + rng.nextInt(10);
            } else if ("VIP".equals(segment)) {
                intentScore = 75 + rng.nextInt(20);
                churnRisk = "LOW";
                daysSinceLast = 5 + rng.nextInt(20);
            } else if ("Cart Abandoner".equals(segment)) {
                intentScore = 70 + rng.nextInt(22);
                churnRisk = "MEDIUM";
                daysSinceLast = 3 + rng.nextInt(15);
            } else if ("At Risk".equals(segment)) {
                intentScore = 30 + rng.nextInt(25);
                churnRisk = "HIGH";
                daysSinceLast = 65 + rng.nextInt(40);
            } else if ("Dormant".equals(segment)) {
                intentScore = 15 + rng.nextInt(20);
                churnRisk = "HIGH";
                daysSinceLast = 110 + rng.nextInt(60);
            } else {
                intentScore = 45 + rng.nextInt(35);
                churnRisk = "LOW";
                daysSinceLast = 15 + rng.nextInt(30);
            }

            Customer c = customerRepository.save(Customer.builder()
                    .customerId("CUS-SEED-" + (i + 1))
                    .name(data[i][0])
                    .email(data[i][1])
                    .phone("+91 98" + (10000000 + rng.nextInt(89999999)))
                    .preferredCategory(data[i][2])
                    .business(business)
                    .totalOrders(totalOrders)
                    .totalSpend(totalSpend)
                    .averageOrderValue(avgOrder)
                    .lastOrderDate(LocalDateTime.now().minusDays(daysSinceLast))
                    .engagementScore(Math.min(98, Math.max(15, intentScore - 5 + rng.nextInt(15))))
                    .purchaseIntentScore(intentScore)
                    .churnRisk(churnRisk)
                    .segment(segment)
                    .createdAt(LocalDateTime.now().minusMonths(rng.nextInt(12) + 1))
                    .build());
            customers.add(c);
        }
        return customers;
    }

    private void createOrders(Business business, List<Customer> customers, List<Product> products) {
        String[] statuses = {"COMPLETED", "COMPLETED", "COMPLETED", "SHIPPED", "PROCESSING"};
        Random rng = new Random(77);
        int ordersCreated = 0;

        for (Customer customer : customers) {
            int numOrders = customer.getTotalOrders();
            for (int j = 0; j < numOrders && ordersCreated < 180; j++) {
                Product p1 = products.get(rng.nextInt(products.size()));
                int qty = rng.nextInt(2) + 1;
                BigDecimal total = p1.getPrice().multiply(BigDecimal.valueOf(qty));

                Order order = Order.builder()
                        .customer(customer)
                        .business(business)
                        .status(statuses[rng.nextInt(statuses.length)])
                        .totalAmount(total)
                        .orderDate(LocalDateTime.now().minusDays(rng.nextInt(160)))
                        .createdAt(LocalDateTime.now().minusDays(rng.nextInt(160)))
                        .build();

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(p1)
                        .quantity(qty)
                        .priceAtPurchase(p1.getPrice())
                        .build();
                order.getItems().add(item);
                orderRepository.save(order);
                ordersCreated++;
            }
        }
    }

    private void createAbandonedCarts(Business business, List<Customer> customers, List<Product> products) {
        Random rng = new Random(55);
        List<Customer> cartCustomers = customers.stream()
                .filter(c -> "Cart Abandoner".equals(c.getSegment()) || "High Intent".equals(c.getSegment()))
                .limit(15)
                .toList();

        for (Customer customer : cartCustomers) {
            Product p1 = products.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(customer.getPreferredCategory()))
                    .findFirst()
                    .orElse(products.get(rng.nextInt(products.size())));

            Cart cart = Cart.builder()
                    .customer(customer)
                    .business(business)
                    .status("ABANDONED")
                    .createdAt(LocalDateTime.now().minusDays(rng.nextInt(6) + 1))
                    .updatedAt(LocalDateTime.now().minusDays(rng.nextInt(2) + 1))
                    .build();

            CartItem ci = CartItem.builder()
                    .cart(cart)
                    .product(p1)
                    .quantity(rng.nextInt(2) + 1)
                    .build();
            cart.getItems().add(ci);
            cartRepository.save(cart);
        }
    }

    private void createCustomerEvents(List<Customer> customers, List<Product> products) {
        Random rng = new Random(33);
        String[] events = {"VIEW_PRODUCT", "VIEW_PRODUCT", "ADD_TO_CART", "START_CHECKOUT", "VIEW_CATEGORY"};

        for (Customer c : customers) {
            int numEvents = 3 + rng.nextInt(6);
            for (int i = 0; i < numEvents; i++) {
                Product p = products.get(rng.nextInt(products.size()));
                customerEventRepository.save(CustomerEvent.builder()
                        .customer(c)
                        .product(p)
                        .eventType(events[rng.nextInt(events.length)])
                        .timestamp(LocalDateTime.now().minusDays(rng.nextInt(30)))
                        .build());
            }
        }
    }

    private void createCampaigns(Business business) {
        List<Campaign> list = List.of(
                Campaign.builder()
                        .business(business)
                        .name("Festival Electronics Super Sale")
                        .targetSegment("VIP")
                        .status("ACTIVE")
                        .offerDetails("Flat 12% instant discount on flagship smartphones & audio gear")
                        .generatedMessage("Exclusive VIP access: Enjoy 12% off on top electronics with code FESTVIP12!")
                        .estimatedImpact(BigDecimal.valueOf(185000))
                        .createdByAgent(false)
                        .createdAt(LocalDateTime.now().minusDays(15))
                        .build(),
                Campaign.builder()
                        .business(business)
                        .name("AI High-Intent Cart Recovery Blitz")
                        .targetSegment("Cart Abandoner")
                        .status("ACTIVE")
                        .offerDetails("Personalized 10% coupon + Free Express Delivery")
                        .generatedMessage("Complete your order today with code RECOVER10 to enjoy free express delivery!")
                        .estimatedImpact(BigDecimal.valueOf(64000))
                        .createdByAgent(true)
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .build(),
                Campaign.builder()
                        .business(business)
                        .name("Win-Back Dormant VIP Customers")
                        .targetSegment("At Risk")
                        .status("PENDING_APPROVAL")
                        .offerDetails("15% loyalty comeback discount on favorite category")
                        .generatedMessage("We miss you! Here is an exclusive 15% discount on your favorite products.")
                        .estimatedImpact(BigDecimal.valueOf(48000))
                        .createdByAgent(true)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                Campaign.builder()
                        .business(business)
                        .name("Monsoon Footwear Clearance")
                        .targetSegment("Loyal")
                        .status("COMPLETED")
                        .offerDetails("Buy 1 Get 1 at 50% off on all sneakers")
                        .generatedMessage("Step up your style with our monsoon footwear specials.")
                        .estimatedImpact(BigDecimal.valueOf(92000))
                        .createdByAgent(false)
                        .createdAt(LocalDateTime.now().minusDays(45))
                        .build()
        );
        campaignRepository.saveAll(list);
    }

    private void createAgentActions(Business business, User user, List<Customer> customers) {
        Customer c1 = customers.get(0);
        Customer c2 = customers.get(3);

        List<AgentAction> actions = List.of(
                AgentAction.builder()
                        .business(business)
                        .user(user)
                        .goal("Convert High-Intent Customer " + c1.getName() + " with personalized coupon")
                        .reasoningSummary("Customer shows intent score 88/100 and viewed Electronics products 4 times this week without checkout.")
                        .toolUsed("createOffer")
                        .parameters("{\"customerId\":" + c1.getId() + ",\"discountPercent\":10,\"code\":\"INTENT10\"}")
                        .result("Coupon code 'INTENT10' generated and delivered via push notification.")
                        .status("EXECUTED")
                        .createdAt(LocalDateTime.now().minusHours(4))
                        .build(),
                AgentAction.builder()
                        .business(business)
                        .user(user)
                        .goal("Recover High-Value Abandoned Cart for " + c2.getName())
                        .reasoningSummary("Cart value ₹12,999 abandoned 24h ago. Recovery probability scored at 82%.")
                        .toolUsed("createRecoveryAction")
                        .parameters("{\"cartId\":1,\"customerId\":" + c2.getId() + ",\"discountPercent\":10}")
                        .result("Recovery email dispatched with 10% limited-time incentive.")
                        .status("EXECUTED")
                        .createdAt(LocalDateTime.now().minusHours(12))
                        .build(),
                AgentAction.builder()
                        .business(business)
                        .user(user)
                        .goal("Launch At-Risk Customer Retention Campaign")
                        .reasoningSummary("Identified 8 VIP customers inactive for >60 days risking ₹92,000 in annual revenue.")
                        .toolUsed("createCampaign")
                        .parameters("{\"name\":\"VIP Win-Back Campaign\",\"targetSegment\":\"At Risk\",\"estimatedImpact\":48000}")
                        .result(null)
                        .status("PENDING_APPROVAL")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .build(),
                AgentAction.builder()
                        .business(business)
                        .user(user)
                        .goal("Dispatch 15% Retention Credit to High-Churn Risk Customer")
                        .reasoningSummary("Customer engagement dropped to 18/100. High churn risk detected.")
                        .toolUsed("createOffer")
                        .parameters("{\"customerId\":7,\"discountPercent\":15,\"code\":\"COMEBACK15\"}")
                        .result(null)
                        .status("PENDING_APPROVAL")
                        .createdAt(LocalDateTime.now().minusMinutes(45))
                        .build(),
                AgentAction.builder()
                        .business(business)
                        .user(user)
                        .goal("Bundle Recommendation for iPhone 15 Pro & Sony Headphones")
                        .reasoningSummary("Cross-category co-purchase affinity measured at 0.88. Estimated AOV uplift +18%.")
                        .toolUsed("createCampaign")
                        .parameters("{\"name\":\"Pro Audio & Mobile Combo\",\"targetSegment\":\"High Intent\",\"estimatedImpact\":75000}")
                        .result("Action approved by admin.")
                        .status("APPROVED")
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build()
        );
        agentActionRepository.saveAll(actions);
    }

    private void createGrowthOpportunities(Business business, List<Customer> customers, List<Product> products) {
        Customer cHigh = customers.stream().filter(c -> "High Intent".equals(c.getSegment())).findFirst().orElse(customers.get(0));
        Customer cRisk = customers.stream().filter(c -> "At Risk".equals(c.getSegment())).findFirst().orElse(customers.get(6));
        Product pTop = products.get(0);

        List<GrowthOpportunity> opps = List.of(
                GrowthOpportunity.builder()
                        .business(business)
                        .title("Recover Abandoned High-Value Cart – ₹14,999")
                        .type("RECOVER_CART")
                        .targetCustomer(cHigh)
                        .estimatedImpact(BigDecimal.valueOf(14999))
                        .confidenceScore(84)
                        .recommendedAction("Send personalized free-shipping + 10% discount recovery email.")
                        .reason("Customer abandoned high-value electronics cart 18 hours ago with 84% recovery probability.")
                        .status("PENDING")
                        .createdAt(LocalDateTime.now().minusHours(6))
                        .build(),
                GrowthOpportunity.builder()
                        .business(business)
                        .title("Convert High-Intent Buyer – " + cHigh.getName())
                        .type("HIGH_INTENT")
                        .targetCustomer(cHigh)
                        .estimatedImpact(BigDecimal.valueOf(8500))
                        .confidenceScore(91)
                        .recommendedAction("Trigger personalized " + cHigh.getPreferredCategory() + " recommendation with 10% limited-time incentive.")
                        .reason("Purchase intent score: 91/100. Frequent browsing in " + cHigh.getPreferredCategory() + " without checkout.")
                        .status("PENDING")
                        .createdAt(LocalDateTime.now().minusHours(10))
                        .build(),
                GrowthOpportunity.builder()
                        .business(business)
                        .title("Prevent VIP Churn – " + cRisk.getName())
                        .type("PREVENT_CHURN")
                        .targetCustomer(cRisk)
                        .estimatedImpact(BigDecimal.valueOf(36000))
                        .confidenceScore(78)
                        .recommendedAction("Launch exclusive 15% win-back campaign with free express delivery.")
                        .reason("High lifetime value customer inactive for 70+ days. Churn risk categorized as HIGH.")
                        .status("PENDING")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                GrowthOpportunity.builder()
                        .business(business)
                        .title("Launch Cross-Sell Bundle for " + pTop.getName())
                        .type("BUNDLE")
                        .targetProduct(pTop)
                        .estimatedImpact(BigDecimal.valueOf(95000))
                        .confidenceScore(86)
                        .recommendedAction("Create bundle campaign offering 10% discount on headphones & case accessories.")
                        .reason("Top seller with 300+ sales and high complementary accessory purchase correlation.")
                        .status("PENDING")
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build()
        );
        opportunityRepository.saveAll(opps);
    }
}
