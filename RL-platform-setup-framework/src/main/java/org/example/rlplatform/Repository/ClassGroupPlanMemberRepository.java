package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ClassGroupPlanMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassGroupPlanMemberRepository extends JpaRepository<ClassGroupPlanMember, Integer> {

    List<ClassGroupPlanMember> findByPlanIdOrderByGroupNoAscMemberOrderAsc(Integer planId);
}
