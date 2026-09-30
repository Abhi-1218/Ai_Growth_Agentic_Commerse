package com.growthpilot.repository;

import com.growthpilot.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByBusinessId(Long businessId, Pageable pageable);
    List<Product> findAllByBusinessId(Long businessId);
    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);
    long countByBusinessId(Long businessId);

    @Query("SELECT p FROM Product p WHERE p.business.id = :businessId ORDER BY p.totalSales DESC")
    List<Product> findTopProductsByBusinessId(Long businessId, Pageable pageable);

    List<Product> findByBusinessIdAndCategory(Long businessId, String category);
    List<Product> findByBusinessIdAndNameContainingIgnoreCaseOrBusinessIdAndDescriptionContainingIgnoreCase(
            Long businessId, String name, Long sameBusinessId, String description);
}
