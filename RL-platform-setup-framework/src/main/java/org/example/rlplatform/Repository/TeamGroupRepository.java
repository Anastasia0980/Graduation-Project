package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.TeamGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamGroupRepository extends JpaRepository<TeamGroup, Integer> {

    Optional<TeamGroup> findByAssignmentIdAndTeamCodeAndIsDeletedFalse(Integer assignmentId, String teamCode);

    Optional<TeamGroup> findByAssignmentIdAndCaptainStudentIdAndIsDeletedFalse(Integer assignmentId, Integer captainStudentId);

    List<TeamGroup> findByAssignmentIdAndIsDeletedFalseOrderByIdAsc(Integer assignmentId);

    TeamGroup findByIdAndIsDeletedFalse(Integer id);

    boolean existsByTeamCodeAndIsDeletedFalse(String teamCode);
}
