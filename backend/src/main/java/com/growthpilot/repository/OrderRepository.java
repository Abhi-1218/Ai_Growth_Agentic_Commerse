package com.growthpilot.repository;

import com.growthpilot.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByBusinessId(Long businessId, Pageable pageable);
    List<Order> findAllByBusinessId(Long businessId);
    List<Order> findAllByCustomerId(Long customerId);
    long countByBusinessId(Long businessId);
    Optional<Order> findByIdAndBusinessId(Long id, Long businessId);
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.business.id = :businessId")
    BigDecimal sumTotalRevenueByBusinessId(Long businessId);
}
