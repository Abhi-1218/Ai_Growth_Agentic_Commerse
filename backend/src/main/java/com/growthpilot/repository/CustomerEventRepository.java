package com.growthpilot.repository;

import com.growthpilot.entity.CustomerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerEventRepository extends JpaRepository<CustomerEvent, Long> {
    List<CustomerEvent> findAllByCustomerIdOrderByTimestampDesc(Long customerId);
    long countByCustomerIdAndEventType(Long customerId, String eventType);
}
