package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ScoreExportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreExportRecordRepository extends JpaRepository<ScoreExportRecord, Long> {

    List<ScoreExportRecord> findByTeacherIdOrderByExportTimeDesc(Integer teacherId);
}