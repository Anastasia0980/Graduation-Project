package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExperimentAssignmentRepository extends JpaRepository<ExperimentAssignment,Integer> {
    Page<ExperimentAssignment> findByStudentClass_IdAndIsDeletedFalse(Integer studentClassId, Pageable pageable);

    @Query("SELECT e FROM ExperimentAssignment e WHERE e.studentClass.id = :classId AND e.isDeleted = false "
            + "AND (e.publicationStatus IS NULL OR e.publicationStatus = :published)")
    Page<ExperimentAssignment> findPublishedForStudentClass(
            @Param("classId") Integer classId,
            @Param("published") PublicationStatus published,
            Pageable pageable);

    Page<ExperimentAssignment> findByTeacherIdAndIsDeletedFalse(Integer teacherId, Pageable pageable);

    ExperimentAssignment findByIdAndIsDeletedFalse(Integer id);
}
