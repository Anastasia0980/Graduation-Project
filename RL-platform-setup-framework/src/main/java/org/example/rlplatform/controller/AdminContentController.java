package org.example.rlplatform.controller;

import org.example.rlplatform.Repository.AnnouncementRepository;
import org.example.rlplatform.Repository.HelpDocumentRepository;
import org.example.rlplatform.entity.Announcement;
import org.example.rlplatform.entity.HelpDocument;
import org.example.rlplatform.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class AdminContentController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private HelpDocumentRepository helpDocumentRepository;

    @GetMapping("/announcements")
    public Result<Page<Announcement>> listAnnouncements(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return Result.success(announcementRepository.findByIsDeletedFalseOrderByUpdateTimeDescIdDesc(PageRequest.of(pageNum, pageSize)));
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            return Result.error("公告内容不能为空");
        }
        Announcement entity = new Announcement();
        entity.setContent(announcement.getContent().trim());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setIsDeleted(false);
        return Result.success(announcementRepository.save(entity));
    }

    @PatchMapping("/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Announcement> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement) {
        Announcement db = announcementRepository.findByIdAndIsDeletedFalse(id);
        if (db == null) {
            return Result.error("公告不存在或已删除");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            return Result.error("公告内容不能为空");
        }
        db.setContent(announcement.getContent().trim());
        db.setUpdateTime(LocalDateTime.now());
        return Result.success(announcementRepository.save(db));
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteAnnouncement(@PathVariable Integer id) {
        Announcement db = announcementRepository.findByIdAndIsDeletedFalse(id);
        if (db == null) {
            return Result.error("公告不存在或已删除");
        }
        db.setIsDeleted(true);
        db.setUpdateTime(LocalDateTime.now());
        announcementRepository.save(db);
        return Result.success();
    }

    @GetMapping("/help-docs")
    public Result<Page<HelpDocument>> listHelpDocs(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        return Result.success(helpDocumentRepository.findByIsDeletedFalseOrderByUpdateTimeDescIdDesc(PageRequest.of(pageNum, pageSize)));
    }

    @PostMapping("/help-docs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<HelpDocument> createHelpDoc(@RequestBody HelpDocument helpDocument) {
        if (helpDocument.getTitle() == null || helpDocument.getTitle().trim().isEmpty()) {
            return Result.error("说明标题不能为空");
        }
        if (helpDocument.getContent() == null || helpDocument.getContent().trim().isEmpty()) {
            return Result.error("说明内容不能为空");
        }
        HelpDocument entity = new HelpDocument();
        entity.setTitle(helpDocument.getTitle().trim());
        entity.setContent(helpDocument.getContent().trim());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setIsDeleted(false);
        return Result.success(helpDocumentRepository.save(entity));
    }

    @PatchMapping("/help-docs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<HelpDocument> updateHelpDoc(@PathVariable Integer id, @RequestBody HelpDocument helpDocument) {
        HelpDocument db = helpDocumentRepository.findByIdAndIsDeletedFalse(id);
        if (db == null) {
            return Result.error("说明不存在或已删除");
        }
        if (helpDocument.getTitle() == null || helpDocument.getTitle().trim().isEmpty()) {
            return Result.error("说明标题不能为空");
        }
        if (helpDocument.getContent() == null || helpDocument.getContent().trim().isEmpty()) {
            return Result.error("说明内容不能为空");
        }
        db.setTitle(helpDocument.getTitle().trim());
        db.setContent(helpDocument.getContent().trim());
        db.setUpdateTime(LocalDateTime.now());
        return Result.success(helpDocumentRepository.save(db));
    }

    @DeleteMapping("/help-docs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteHelpDoc(@PathVariable Integer id) {
        HelpDocument db = helpDocumentRepository.findByIdAndIsDeletedFalse(id);
        if (db == null) {
            return Result.error("说明不存在或已删除");
        }
        db.setIsDeleted(true);
        db.setUpdateTime(LocalDateTime.now());
        helpDocumentRepository.save(db);
        return Result.success();
    }
}
