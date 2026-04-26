package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.TeacherUploadResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherUploadResourceRepository extends JpaRepository<TeacherUploadResource, Integer> {

    List<TeacherUploadResource> findAllByOrderByIdDesc();

    List<TeacherUploadResource> findByStatusAndIsDeletedFalseOrderByIdDesc(String status);

    TeacherUploadResource findByIdAndIsDeletedFalse(Integer id);
}
