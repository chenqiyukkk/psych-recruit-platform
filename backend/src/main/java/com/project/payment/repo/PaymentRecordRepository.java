package com.project.payment.repo;

import com.project.payment.entity.PaymentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
  Optional<PaymentRecord> findByRegistrationId(Long registrationId);
}

