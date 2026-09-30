package com.growthpilot.service;

import com.growthpilot.entity.*;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.*;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WishlistService {
    private final WishlistItemRepository items;
    private final ProductRepository products;
    private final SecurityUtils security;
    public WishlistService(WishlistItemRepository items, ProductRepository products, SecurityUtils security) {
        this.items = items; this.products = products; this.security = security;
    }
    @Transactional(readOnly = true)
    public List<Product> list() {
        return items.findAllByCustomerIdOrderByCreatedAtDesc(security.getCurrentCustomer().getId())
                .stream().map(WishlistItem::getProduct).toList();
    }
    @Transactional
    public List<Product> add(Long productId) {
        Customer c = security.getCurrentCustomer();
        Product p = products.findByIdAndBusinessId(productId, c.getBusiness().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (items.findByCustomerIdAndProductId(c.getId(), productId).isEmpty()) {
            items.save(WishlistItem.builder().customer(c).product(p).createdAt(LocalDateTime.now()).build());
        }
        return list();
    }
    @Transactional
    public List<Product> remove(Long productId) {
        WishlistItem item = items.findByCustomerIdAndProductId(security.getCurrentCustomer().getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        items.delete(item); return list();
    }
}
