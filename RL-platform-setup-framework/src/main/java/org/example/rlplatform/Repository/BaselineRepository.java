package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.Baseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaselineRepository extends JpaRepository<Baseline, Long> {
    Optional<Baseline> findByEnvironmentAndTaskIdAndAlgorithm(String environment, String taskId, String algorithm);

    List<Baseline> findByEnvironmentAndIsDeletedFalse(String environment);
}

