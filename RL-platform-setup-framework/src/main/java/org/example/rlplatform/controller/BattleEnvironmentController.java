package org.example.rlplatform.controller;

import org.example.rlplatform.entity.BattleEnvironment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.TeacherUploadResource;
import org.example.rlplatform.service.BattleEnvironmentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/battle-environments")
public class BattleEnvironmentController {

    private final BattleEnvironmentService battleEnvironmentService;

    public BattleEnvironmentController(BattleEnvironmentService battleEnvironmentService) {
        this.battleEnvironmentService = battleEnvironmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> create(@RequestParam("name") String name,
                                              @RequestParam("code") String code,
                                              @RequestParam(value = "isGpu", required = false, defaultValue = "false") Boolean isGpu,
                                              @RequestParam(value = "cudaDevice", required = false) String cudaDevice,
                                              @RequestParam("requirements") MultipartFile requirements,
                                              @RequestParam("script") MultipartFile script) {
        BattleEnvironment env = battleEnvironmentService.create(name, code, isGpu, cudaDevice, requirements, script);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", env.getId());
        data.put("code", env.getCode());
        data.put("status", env.getStatus());
        return Result.success(data);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<BattleEnvironment>> listAll() {
        return Result.success(battleEnvironmentService.listAll());
    }

    @GetMapping("/ready")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<BattleEnvironment>> listReady() {
        return Result.success(battleEnvironmentService.listReadyBattleEnvironments());
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> disable(@PathVariable Integer id) {
        battleEnvironmentService.disable(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> delete(@PathVariable Integer id) {
        battleEnvironmentService.delete(id);
        return Result.success();
    }

    @GetMapping("/student-downloads")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public Result<List<Map<String, Object>>> listStudentDownloads() {
        return Result.success(battleEnvironmentService.listStudentDownloadItems());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> downloadEnvironment(@PathVariable Integer id) {
        Map<String, Object> data = battleEnvironmentService.downloadEnvironmentPackage(id);
        byte[] bytes = (byte[]) data.get("bytes");
        String fileName = (String) data.get("fileName");
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentHeader(fileName))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(resource);
    }

    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherUploadResource>> listTeacherResources() {
        return Result.success(battleEnvironmentService.listTeacherResources());
    }

    @PostMapping("/resources/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherUploadResource>> uploadTeacherResources(@RequestParam("name") String name,
                                                                      @RequestParam("files") MultipartFile[] files) {
        return Result.success(battleEnvironmentService.uploadTeacherResources(name, files));
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> deleteTeacherResource(@PathVariable Integer id) {
        battleEnvironmentService.deleteTeacherResource(id);
        return Result.success();
    }

    @GetMapping("/resources/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> downloadTeacherResource(@PathVariable Integer id) {
        Map<String, Object> data = battleEnvironmentService.downloadTeacherResource(id);
        Resource resource = (Resource) data.get("resource");
        String fileName = (String) data.get("fileName");
        long fileSize = (long) data.getOrDefault("fileSize", 0L);
        String contentType = (String) data.getOrDefault("contentType", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentHeader(fileName))
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileSize)
                .body(resource);
    }

    private String buildAttachmentHeader(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
