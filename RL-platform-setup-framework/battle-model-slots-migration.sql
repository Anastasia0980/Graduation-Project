ALTER TABLE battle_model_submission
    ADD COLUMN slot_index INT NULL,
    ADD COLUMN main_model TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE battle_participant
    ADD COLUMN student1_attachment_release_requested TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN student2_attachment_release_requested TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN attachments_cleaned TINYINT(1) NOT NULL DEFAULT 0;

-- Optional compatibility backfill: assign up to five active submissions per owner/task.
-- MySQL 8+ syntax.
UPDATE battle_model_submission b
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY assignment_id, student_id
               ORDER BY create_time ASC, id ASC
           ) AS rn
    FROM battle_model_submission
    WHERE active = 1
) ranked ON ranked.id = b.id
SET b.slot_index = CASE WHEN ranked.rn <= 5 THEN ranked.rn ELSE NULL END,
    b.active = CASE WHEN ranked.rn <= 5 THEN b.active ELSE 0 END,
    b.main_model = 0
WHERE b.active = 1;
