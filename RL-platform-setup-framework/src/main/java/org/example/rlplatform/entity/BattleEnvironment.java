package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "battle_environment")
public class BattleEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "mode_type", nullable = false, length = 30)
    private String modeType = "BATTLE";

    @Column(name = "conda_env_name", nullable = false, length = 150)
    private String condaEnvName;

    @Column(name = "python_path", columnDefinition = "TEXT")
    private String pythonPath;

    @Column(name = "script_path", nullable = false, columnDefinition = "TEXT")
    private String scriptPath;

    @Column(name = "requirements_path", nullable = false, columnDefinition = "TEXT")
    private String requirementsPath;

    @Column(name = "workspace_path", nullable = false, columnDefinition = "TEXT")
    private String workspacePath;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "CREATING";

    @Column(name = "install_log", columnDefinition = "LONGTEXT")
    private String installLog;

    @Column(name = "is_gpu", nullable = false)
    private Boolean isGpu = false;

    @Column(name = "cuda_device", length = 50)
    private String cudaDevice;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
