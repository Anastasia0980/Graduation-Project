package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.battle.BattleRunner;
import org.example.rlplatform.battle.InMemoryBattleQueue;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.Submission;
import org.example.rlplatform.service.BattleService;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BattleServiceImpl implements BattleService {

    @Autowired
    private InMemoryBattleQueue battleQueue;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private BattleRunner battleRunner;

    @Value("${evaluation.workspace:}")
    private String workspace;

    @Override
    public Result<?> submitAndMaybeStart(MultipartFile model, MultipartFile config, Long studentId, String environment, Integer games) {

        try {
            if (studentId == null) return Result.error("studentId is required");
            if (games == null || games <= 0) games = 50;
            if (environment == null || environment.isBlank()) environment = "tictactoe_v3";

            String baseDir = (workspace != null && !workspace.isBlank())
                    ? workspace
                    : Paths.get(System.getProperty("user.dir")).toString();

            String uuid = UUID.randomUUID().toString().replace("-", "");
            Path studentDir = Paths.get(baseDir, "uploads", String.valueOf(studentId), uuid);
            Files.createDirectories(studentDir);

            Path configPath = studentDir.resolve("config.json");
            Path modelPath = studentDir.resolve("model.pt");

            config.transferTo(configPath);
            model.transferTo(modelPath);

            String rel = Paths.get("uploads", String.valueOf(studentId), uuid).toString().replace("\\", "/");
            Submission sub = new Submission(studentId, environment, games, rel);
            int qSize = battleQueue.enqueue(sub);

            Submission[] pair = battleQueue.tryDequeuePair(environment);

            if (pair == null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("queued", true);
                data.put("queueSize", qSize);
                data.put("environment", environment);
                data.put("message", "已进入匹配队列，等待另一位同学提交。");
                return Result.success(data);
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setStudentId(pair[0].getStudentId());
            evaluation.setAgentName("BATTLE");
            evaluation.setEnvironment(environment);
            evaluation.setModelId(null);
            evaluation.setEpisodes(games);
            evaluation.setStatus("PENDING");
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());

            evaluationRepository.save(evaluation);

            // 保存双方学生ID到 battle_participant 表
            BattleParticipant bp = new BattleParticipant();
            bp.setEvaluationId(evaluation.getId());
            bp.setStudent1Id(pair[0].getStudentId());
            bp.setStudent2Id(pair[1].getStudentId());
            battleParticipantRepository.save(bp);

            battleRunner.runBattleAsync(
                    evaluation.getId(),
                    pair[0].getStudentDirRel(),
                    pair[1].getStudentDirRel()
            );

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("queued", false);
            data.put("matched", true);
            data.put("evaluationId", evaluation.getId());
            data.put("student1", pair[0].getStudentId());
            data.put("student2", pair[1].getStudentId());
            data.put("environment", environment);
            data.put("games", games);
            data.put("message", "匹配成功，已启动对战评测。");
            return Result.success(data);

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public List<BattleTaskDetailVO> listBattleTasks() {

        List<Evaluation> evaluations = evaluationRepository.findByAgentNameOrderByIdDesc("BATTLE");
        List<BattleTaskDetailVO> result = new ArrayList<>();

        for (Evaluation evaluation : evaluations) {
            BattleTaskDetailVO vo = new BattleTaskDetailVO();
            vo.setEvaluationId(evaluation.getId());
            vo.setStatus(buildStatusText(evaluation.getStatus()));

            Optional<BattleParticipant> bpOpt = battleParticipantRepository.findByEvaluationId(evaluation.getId());
            if (bpOpt.isPresent()) {
                BattleParticipant bp = bpOpt.get();
                vo.setStudent1Id(bp.getStudent1Id());
                vo.setStudent2Id(bp.getStudent2Id());
            }

            List<EvaluationResult> evaluationResults = evaluationResultRepository.findByEvaluationId(evaluation.getId());
            if (evaluationResults != null && !evaluationResults.isEmpty()) {
                EvaluationResult er = evaluationResults.get(0);
                vo.setWinner(er.getWinner());
                vo.setResultDir(er.getResultDir());

                if (bpOpt.isPresent()) {
                    BattleParticipant bp = bpOpt.get();
                    vo.setWinnerText(buildWinnerText(er.getWinner(), bp.getStudent1Id(), bp.getStudent2Id()));
                } else {
                    vo.setWinnerText("结果已生成，但未找到对战双方信息");
                }
            } else {
                vo.setWinner(null);
                vo.setWinnerText(buildStatusText(evaluation.getStatus()));
            }

            result.add(vo);
        }

        return result;
    }

    private String buildWinnerText(Integer winner, Long student1Id, Long student2Id) {
        if (winner == null) return "暂无结果";
        if (winner == 0) return "平局";
        if (winner == 1) return "学生 " + student1Id + " 获胜";
        if (winner == 2) return "学生 " + student2Id + " 获胜";
        return "未知结果";
    }

    private String buildStatusText(String status) {
        if (status == null) return "未知状态";
        switch (status) {
            case "PENDING":
                return "排队中";
            case "RUNNING":
                return "测评中";
            case "FAILED":
                return "测评失败";
            case "FINISHED":
                return "已完成";
            default:
                return status;
        }
    }
}