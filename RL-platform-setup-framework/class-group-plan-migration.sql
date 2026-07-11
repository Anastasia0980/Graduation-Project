ALTER TABLE `user`
    ADD COLUMN student_no VARCHAR(50) NULL;

CREATE TABLE class_group_plan (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    plan_name VARCHAR(120) NOT NULL,
    source_file_name VARCHAR(255) NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    INDEX idx_class_group_plan_class (class_id),
    INDEX idx_class_group_plan_created_by (created_by)
);

CREATE TABLE class_group_plan_member (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_id INT NOT NULL,
    group_no VARCHAR(80) NOT NULL,
    student_id INT NOT NULL,
    student_name_snapshot VARCHAR(100) NULL,
    student_no_snapshot VARCHAR(50) NULL,
    member_order INT NOT NULL,
    INDEX idx_class_group_plan_member_plan (plan_id),
    INDEX idx_class_group_plan_member_group (plan_id, group_no, member_order)
);

ALTER TABLE experiment_assignment
    ADD COLUMN use_saved_group TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN group_plan_id INT NULL;
