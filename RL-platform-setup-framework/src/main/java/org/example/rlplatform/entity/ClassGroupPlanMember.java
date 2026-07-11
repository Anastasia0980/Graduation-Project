package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "class_group_plan_member")
public class ClassGroupPlanMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "group_no", nullable = false, length = 80)
    private String groupNo;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name_snapshot", length = 100)
    private String studentNameSnapshot;

    @Column(name = "student_no_snapshot", length = 50)
    private String studentNoSnapshot;

    @Column(name = "member_order", nullable = false)
    private Integer memberOrder;
}
