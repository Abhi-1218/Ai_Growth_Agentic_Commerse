package com.growthpilot.repository;

import com.growthpilot.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    List<Payment> findAllByBusinessId(Long businessId);
    long countByBusinessIdAndStatus(Long businessId, String status);
}
