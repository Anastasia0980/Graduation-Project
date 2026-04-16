package org.example.rlplatform.service;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.vo.BattleModelOptionVO;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BattleService {

    Result<?> submitAndMaybeStart(Integer assignmentId, MultipartFile model, MultipartFile config);

    Result<?> submitBattleModel(Integer assignmentId, MultipartFile model, MultipartFile config);

    List<BattleModelOptionVO> listMyBattleModels(Integer assignmentId);

    List<BattleModelOptionVO> listOpponentBattleModels(Integer assignmentId, Long mySubmissionId);

    Result<?> challengeBySubmission(Integer assignmentId, Long mySubmissionId, Long opponentSubmissionId);

    Result<?> submitBotAndStart(Integer assignmentId, String difficulty, MultipartFile model, MultipartFile config);

    List<BattleTaskDetailVO> listBattleTasks();
}