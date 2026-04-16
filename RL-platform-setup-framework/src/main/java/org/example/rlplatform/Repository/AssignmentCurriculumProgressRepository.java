package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.AssignmentCurriculumProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentCurriculumProgressRepository extends JpaRepository<AssignmentCurriculumProgress, Long> {

    Optional<AssignmentCurriculumProgress> findByStudentIdAndAssignmentId(Integer studentId, Integer assignmentId);
}
