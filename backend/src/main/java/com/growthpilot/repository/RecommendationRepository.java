package com.growthpilot.repository;

import com.growthpilot.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findAllByCustomerIdOrderByScoreDesc(Long customerId);
    void deleteAllByCustomerId(Long customerId);
}
