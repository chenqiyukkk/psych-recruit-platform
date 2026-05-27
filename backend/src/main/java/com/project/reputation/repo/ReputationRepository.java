package com.project.reputation.repo;

import com.project.reputation.entity.ReputationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReputationRepository extends JpaRepository<ReputationLog,Long> {

    List<ReputationLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
