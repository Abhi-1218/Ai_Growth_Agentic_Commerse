package com.growthpilot.repository;

import com.growthpilot.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findAllByBusinessId(Long businessId, Pageable pageable);
    List<Customer> findAllByBusinessId(Long businessId);
    Optional<Customer> findByIdAndBusinessId(Long id, Long businessId);
    Optional<Customer> findByCustomerId(String customerId);
    Optional<Customer> findByEmailIgnoreCaseAndBusinessId(String email, Long businessId);
    List<Customer> findAllByBusinessIdAndSegment(Long businessId, String segment);
    List<Customer> findAllByBusinessIdAndChurnRisk(Long businessId, String churnRisk);
    long countByBusinessId(Long businessId);
    long countByBusinessIdAndSegment(Long businessId, String segment);

    @Query("SELECT c FROM Customer c WHERE c.business.id = :businessId AND c.purchaseIntentScore >= 70 ORDER BY c.purchaseIntentScore DESC")
    List<Customer> findHighIntentCustomers(@Param("businessId") Long businessId);

    @Query("SELECT c FROM Customer c WHERE c.business.id = :businessId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchCustomers(@Param("businessId") Long businessId, @Param("query") String query, Pageable pageable);
}
