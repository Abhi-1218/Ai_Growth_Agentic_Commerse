package com.growthpilot.repository;

import com.growthpilot.entity.Cart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Page<Cart> findAllByBusinessId(Long businessId, Pageable pageable);
    List<Cart> findAllByBusinessId(Long businessId);
    Optional<Cart> findByIdAndBusinessId(Long id, Long businessId);
    Optional<Cart> findByCustomerIdAndStatus(Long customerId, String status);
    List<Cart> findAllByBusinessIdAndStatus(Long businessId, String status);
    long countByBusinessIdAndStatus(Long businessId, String status);

    @Query("SELECT c FROM Cart c WHERE c.business.id = :businessId AND c.status = 'ABANDONED' ORDER BY c.updatedAt DESC")
    List<Cart> findAbandonedCartsByBusinessId(Long businessId);
}
