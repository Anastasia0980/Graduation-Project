package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.BattleEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleEnvironmentRepository extends JpaRepository<BattleEnvironment, Integer> {

    List<BattleEnvironment> findAllByOrderByIdDesc();

    List<BattleEnvironment> findByIsDeletedFalseOrderByIdDesc();

    List<BattleEnvironment> findByStatusAndIsDeletedFalseOrderByIdDesc(String status);

    Optional<BattleEnvironment> findByCodeAndModeTypeAndIsDeletedFalse(String code, String modeType);

    BattleEnvironment findByIdAndIsDeletedFalse(Integer id);

    boolean existsByCodeAndIsDeletedFalse(String code);
}