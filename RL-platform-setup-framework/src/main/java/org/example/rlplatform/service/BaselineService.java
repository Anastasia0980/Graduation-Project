package org.example.rlplatform.service;

import org.example.rlplatform.entity.BaselineOption;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BaselineService {

    /**
     * 获取某个任务环境下的 baseline catalog。
     * 返回结构：
     * {
     *   "easy": [BaselineOption...],
     *   "medium": [...],
     *   "hard": [...]
     * }
     */
    Map<String, List<BaselineOption>> getBaselineCatalogByEnvironment(String environment);

    /**
     * 上传并落盘 baseline.pth（一次上传只处理一个 environment + difficulty + algorithm）
     */
    BaselineOption uploadBaseline(String environment, String difficulty, String algorithm, MultipartFile model);

    /**
     * baseline 软删除：仅将 baseline 表记录置为 isDeleted=true（不删除文件本体）
     */
    void softDeleteBaseline(String environment, String difficulty, String algorithm);
}

