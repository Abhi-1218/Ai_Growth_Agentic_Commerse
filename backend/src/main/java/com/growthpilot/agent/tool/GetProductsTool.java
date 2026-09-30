package com.growthpilot.agent.tool;

import com.growthpilot.entity.Product;
import com.growthpilot.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GetProductsTool implements AgentTool {

    private final ProductRepository productRepository;

    public GetProductsTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String getName() {
        return "getProducts";
    }

    @Override
    public String getDescription() {
        return "Retrieve catalog products filtered by category or top sales volume.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\"},\"topSalesOnly\":{\"type\":\"boolean\"}}}";
    }

    @Override
    public boolean requiresHumanApproval() {
        return false;
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters, Long businessId, Long userId) {
        List<Product> products;
        String category = parameters != null ? (String) parameters.get("category") : null;
        Boolean topOnly = parameters != null && Boolean.TRUE.equals(parameters.get("topSalesOnly"));

        if (category != null && !category.isBlank()) {
            products = productRepository.findByBusinessIdAndCategory(businessId, category);
        } else if (topOnly) {
            products = productRepository.findTopProductsByBusinessId(businessId, PageRequest.of(0, 10));
        } else {
            products = productRepository.findAllByBusinessId(businessId);
        }

        List<Map<String, Object>> result = products.stream().limit(10).map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getName(),
                "category", p.getCategory(),
                "price", p.getPrice(),
                "totalSales", p.getTotalSales() != null ? p.getTotalSales() : 0,
                "stock", p.getStock() != null ? p.getStock() : 0
        )).collect(Collectors.toList());

        return AgentToolResult.ok(result, "Retrieved " + result.size() + " products.");
    }
}
