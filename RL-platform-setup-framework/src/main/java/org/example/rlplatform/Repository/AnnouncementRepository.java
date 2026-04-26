package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    Page<Announcement> findByIsDeletedFalseOrderByUpdateTimeDescIdDesc(Pageable pageable);

    Announcement findByIdAndIsDeletedFalse(Integer id);
}
