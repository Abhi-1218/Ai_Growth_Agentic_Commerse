package com.growthpilot.repository;

import com.growthpilot.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<WishlistItem> findByCustomerIdAndProductId(Long customerId, Long productId);
    Optional<WishlistItem> findByIdAndCustomerId(Long id, Long customerId);
}
