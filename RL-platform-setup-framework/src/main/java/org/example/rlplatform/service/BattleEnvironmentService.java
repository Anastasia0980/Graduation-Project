package org.example.rlplatform.service;

import org.example.rlplatform.entity.BattleEnvironment;
import org.example.rlplatform.entity.TeacherUploadResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BattleEnvironmentService {

    BattleEnvironment create(String name,
                             String code,
                             Boolean isGpu,
                             String cudaDevice,
                             MultipartFile requirements,
                             MultipartFile script);

    List<BattleEnvironment> listAll();

    List<BattleEnvironment> listReadyBattleEnvironments();

    BattleEnvironment getReadyByCode(String code);

    String resolveWorkspace(String code);

    void disable(Integer id);

    void delete(Integer id);

    List<TeacherUploadResource> listTeacherResources();

    List<TeacherUploadResource> uploadTeacherResources(String name, MultipartFile[] files);

    void deleteTeacherResource(Integer id);

    Map<String, Object> downloadEnvironmentPackage(Integer id);

    Map<String, Object> downloadTeacherResource(Integer id);

    List<Map<String, Object>> listStudentDownloadItems();
}
