package com.project.registration.repo;

import com.project.registration.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration,Long> {


    boolean existsByExperimentIdAndUserId(Long experimentId,Long userId);

    Optional<Registration> findByExperimentIdAndUserId(Long experimentId, Long userId);

    List<Registration> findByUserIdOrderByAppliedAtDesc(Long userId);

    List<Registration> findByUserIdAndIsCompletedTrueOrderByAppliedAtDesc(Long userId);

    List<Registration> findByExperimentIdOrderByAppliedAtDesc(Long experimentId);
}
