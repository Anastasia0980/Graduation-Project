package org.example.rlplatform.service;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BattleService {

    /**
     * 学生提交参赛文件：
     * - 保存到 workspace/uploads/<studentId>/<uuid>/{config.json, model.pt}
     * - 入内存队列
     * - 若凑齐两人，自动创建 evaluation 并异步执行对战
     */
    Result<?> submitAndMaybeStart(MultipartFile model, MultipartFile config, Long studentId, String environment, Integer games);

    /**
     * 查询所有对战任务详情
     */
    List<BattleTaskDetailVO> listBattleTasks();
}