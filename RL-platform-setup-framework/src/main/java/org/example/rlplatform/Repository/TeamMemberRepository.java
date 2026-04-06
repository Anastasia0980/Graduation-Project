package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

    List<TeamMember> findByStudentIdAndIsDeletedFalse(Integer studentId);

    List<TeamMember> findByTeamIdAndIsDeletedFalseOrderByIdAsc(Integer teamId);

    Optional<TeamMember> findByTeamIdAndStudentIdAndIsDeletedFalse(Integer teamId, Integer studentId);

    long countByTeamIdAndIsDeletedFalse(Integer teamId);
}
