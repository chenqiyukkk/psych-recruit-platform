package com.project.registration.repo;

import com.project.registration.entity.Registration;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration,Long> {


    boolean existsByExperimentIdAndUserId(Long experimentId,Long userId);

    Optional<Registration> findByExperimentIdAndUserId(Long experimentId, Long userId);

    List<Registration> findByUserIdOrderByAppliedAtDesc(Long userId);

    List<Registration> findByUserIdAndIsCompletedTrueOrderByAppliedAtDesc(Long userId);

    List<Registration> findByExperimentIdOrderByAppliedAtDesc(Long experimentId);

    long countByExperimentIdAndStatusIn(Long experimentId, Collection<String> statuses);

    @Query("SELECT r.experimentId, COUNT(r) FROM Registration r WHERE r.experimentId IN :experimentIds AND r.status IN :statuses GROUP BY r.experimentId")
    List<Object[]> countApprovedByExperimentIds(@Param("experimentIds") List<Long> experimentIds, @Param("statuses") Collection<String> statuses);
}
