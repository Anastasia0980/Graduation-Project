package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.BattleService;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/battle")
@CrossOrigin
public class BattleController {

    @Autowired
    private BattleService battleService;

    /**
     * 学生提交参赛：进入内存队列，凑齐两人自动对战
     */
    @PostMapping("/submit")
    public Result<?> submit(
            @RequestParam("model") MultipartFile model,
            @RequestParam("config") MultipartFile config,
            @RequestParam("studentId") Long studentId,
            @RequestParam(value = "environment", required = false) String environment,
            @RequestParam(value = "games", required = false) Integer games
    ) {
        return battleService.submitAndMaybeStart(model, config, studentId, environment, games);
    }

    /**
     * 查询任务详情列表
     */
    @GetMapping("/tasks")
    public Result<List<BattleTaskDetailVO>> listTasks() {
        return Result.success(battleService.listBattleTasks());
    }
}