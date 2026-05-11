package com.project.payment.repo;

import com.project.payment.entity.PaymentCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCodeRepository extends JpaRepository<PaymentCode, Long> {
  List<PaymentCode> findByUserIdOrderByIdDesc(Long userId);
}

