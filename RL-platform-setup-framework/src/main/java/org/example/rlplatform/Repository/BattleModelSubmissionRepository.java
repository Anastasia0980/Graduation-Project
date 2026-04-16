package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.BattleModelSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleModelSubmissionRepository extends JpaRepository<BattleModelSubmission, Long> {

    List<BattleModelSubmission> findByAssignmentIdAndStudentIdAndActiveTrueOrderByIdDesc(Integer assignmentId, Long studentId);

    List<BattleModelSubmission> findByAssignmentIdAndStudentIdNotAndActiveTrueOrderByIdDesc(Integer assignmentId, Long studentId);

    Optional<BattleModelSubmission> findByIdAndActiveTrue(Long id);
}