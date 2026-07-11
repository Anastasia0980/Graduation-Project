package org.example.rlplatform.service;

import org.springframework.data.domain.Page;
import org.example.rlplatform.entity.StudentClass;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface StudentClassService {

    void create(StudentClass studentClass);

    void update(StudentClass studentClass, Integer id);

    void softDelete(Integer id);

    Page<StudentClass> listPage(Integer pageNum, Integer pageSize, Boolean  isDeleted);

    StudentClass findByName(String name);

    StudentClass findByIdAndIsDeletedFalse(Integer id);

    StudentClass findByCodeAndIsDeletedFalse(String code);

    Map<String, Object> importStudents(Integer classId, MultipartFile file);

    List<Map<String, Object>> listGroupPlans(Integer classId);
}
