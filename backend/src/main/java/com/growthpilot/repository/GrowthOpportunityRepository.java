package com.growthpilot.repository;

import com.growthpilot.entity.GrowthOpportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrowthOpportunityRepository extends JpaRepository<GrowthOpportunity, Long> {
    Page<GrowthOpportunity> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);
    List<GrowthOpportunity> findAllByBusinessIdAndStatus(Long businessId, String status);
    long countByBusinessIdAndStatus(Long businessId, String status);
}
