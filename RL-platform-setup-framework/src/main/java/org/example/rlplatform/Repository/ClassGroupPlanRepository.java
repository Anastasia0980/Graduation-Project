package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ClassGroupPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassGroupPlanRepository extends JpaRepository<ClassGroupPlan, Integer> {

    List<ClassGroupPlan> findByClassIdAndActiveTrueOrderByCreatedAtDesc(Integer classId);

    ClassGroupPlan findByIdAndActiveTrue(Integer id);
}
