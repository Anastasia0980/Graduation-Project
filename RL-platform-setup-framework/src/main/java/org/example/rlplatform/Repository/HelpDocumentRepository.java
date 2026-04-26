package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.HelpDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpDocumentRepository extends JpaRepository<HelpDocument, Integer> {
    Page<HelpDocument> findByIsDeletedFalseOrderByUpdateTimeDescIdDesc(Pageable pageable);

    HelpDocument findByIdAndIsDeletedFalse(Integer id);
}
