package com.project.experiment.repo;

import com.project.experiment.entity.ExperimentTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentTagRepository extends JpaRepository<ExperimentTag, Long> {
  List<ExperimentTag> findByExperimentId(Long experimentId);

  void deleteByExperimentId(Long experimentId);
}

