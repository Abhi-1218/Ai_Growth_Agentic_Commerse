package com.growthpilot.repository;

import com.growthpilot.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Page<Campaign> findAllByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);
    long countByBusinessIdAndStatus(Long businessId, String status);
}
