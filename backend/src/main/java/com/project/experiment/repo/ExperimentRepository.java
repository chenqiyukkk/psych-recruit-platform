package com.project.experiment.repo;

import com.project.experiment.entity.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExperimentRepository
    extends JpaRepository<Experiment, Long>, JpaSpecificationExecutor<Experiment> {}

