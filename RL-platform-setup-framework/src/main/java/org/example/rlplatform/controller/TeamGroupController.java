package org.example.rlplatform.controller;

import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.TeamGroupRepository;
import org.example.rlplatform.Repository.TeamMemberRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.EvaluationMode;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.TeamGroup;
import org.example.rlplatform.entity.TeamMember;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.TeamGroupVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class TeamGroupController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TeamGroupRepository teamGroupRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ExperimentAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public TeamGroupController(TeamGroupRepository teamGroupRepository,
                               TeamMemberRepository teamMemberRepository,
                               ExperimentAssignmentRepository assignmentRepository,
                               UserRepository userRepository) {
        this.teamGroupRepository = teamGroupRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/assignments/{assignmentId}/teams/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<TeamGroupVO> myTeam(@PathVariable Integer assignmentId) {
        Integer userId = currentUserId();
        TeamGroup team = findMyTeam(assignmentId, userId);
        if (team == null) {
            return Result.success(null);
        }
        return Result.success(toVO(team, userId));
    }

    @PostMapping("/assignments/{assignmentId}/teams")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<TeamGroupVO> createTeam(@PathVariable Integer assignmentId,
                                          @RequestBody Map<String, Object> body) {
        Integer userId = currentUserId();
        ExperimentAssignment assignment = getValidTeamAssignment(assignmentId);
        ensureWithinGroupDeadline(assignment);

        if (findMyTeam(assignmentId, userId) != null) {
            return Result.error("当前任务下你已加入队伍");
        }

        String teamName = body == null ? "" : String.valueOf(body.getOrDefault("teamName", "")).trim();
        if (teamName.isBlank()) {
            return Result.error("请输入队伍名称");
        }

        TeamGroup team = new TeamGroup();
        team.setAssignmentId(assignment.getId());
        team.setTeamName(teamName);
        team.setTeamCode(generateTeamCode());
        team.setCaptainStudentId(userId);
        team.setMaxMembers(3);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        team.setLocked(false);
        team.setLockTime(null);
        team.setIsDeleted(false);
        team = teamGroupRepository.save(team);

        TeamMember captainMember = new TeamMember();
        captainMember.setTeamId(team.getId());
        captainMember.setStudentId(userId);
        captainMember.setJoinTime(LocalDateTime.now());
        captainMember.setIsDeleted(false);
        teamMemberRepository.save(captainMember);

        return Result.success(toVO(team, userId));
    }

    @PostMapping("/assignments/{assignmentId}/teams/join")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<TeamGroupVO> joinTeam(@PathVariable Integer assignmentId,
                                        @RequestBody Map<String, Object> body) {
        Integer userId = currentUserId();
        ExperimentAssignment assignment = getValidTeamAssignment(assignmentId);
        ensureWithinGroupDeadline(assignment);

        if (findMyTeam(assignmentId, userId) != null) {
            return Result.error("当前任务下你已加入队伍");
        }

        String teamCode = body == null ? "" : String.valueOf(body.getOrDefault("teamCode", "")).trim().toUpperCase();
        if (teamCode.isBlank()) {
            return Result.error("请输入队伍码");
        }

        TeamGroup team = teamGroupRepository.findByAssignmentIdAndTeamCodeAndIsDeletedFalse(assignmentId, teamCode)
                .orElse(null);
        if (team == null) {
            return Result.error("队伍不存在");
        }

        team = ensureAutoLock(team, assignment);
        if (Boolean.TRUE.equals(team.getLocked())) {
            return Result.error("当前队伍已锁定，无法加入");
        }

        long memberCount = teamMemberRepository.countByTeamIdAndIsDeletedFalse(team.getId());
        Integer maxMembers = team.getMaxMembers() == null ? 3 : team.getMaxMembers();
        if (memberCount >= maxMembers) {
            return Result.error("当前队伍人数已满");
        }

        if (teamMemberRepository.findByTeamIdAndStudentIdAndIsDeletedFalse(team.getId(), userId).isPresent()) {
            return Result.error("你已在该队伍中");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setStudentId(userId);
        member.setJoinTime(LocalDateTime.now());
        member.setIsDeleted(false);
        teamMemberRepository.save(member);

        team.setUpdateTime(LocalDateTime.now());
        teamGroupRepository.save(team);

        return Result.success(toVO(team, userId));
    }

    @PostMapping("/assignments/{assignmentId}/teams/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> leaveTeam(@PathVariable Integer assignmentId) {
        Integer userId = currentUserId();
        ExperimentAssignment assignment = getValidTeamAssignment(assignmentId);
        TeamGroup team = findMyTeam(assignmentId, userId);
        if (team == null) {
            return Result.error("当前未加入队伍");
        }
        team = ensureAutoLock(team, assignment);

        if (Boolean.TRUE.equals(team.getLocked())) {
            return Result.error("队伍成员已锁定，当前不可退出");
        }
        if (Objects.equals(team.getCaptainStudentId(), userId)) {
            return Result.error("队长不可退出队伍，请使用解散队伍");
        }

        TeamMember self = teamMemberRepository.findByTeamIdAndStudentIdAndIsDeletedFalse(team.getId(), userId)
                .orElse(null);
        if (self == null) {
            return Result.error("当前未加入队伍");
        }

        self.setIsDeleted(true);
        teamMemberRepository.save(self);

        team.setUpdateTime(LocalDateTime.now());
        teamGroupRepository.save(team);
        return Result.success();
    }

    @PostMapping("/assignments/{assignmentId}/teams/dissolve")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> dissolveTeam(@PathVariable Integer assignmentId) {
        Integer userId = currentUserId();
        ExperimentAssignment assignment = getValidTeamAssignment(assignmentId);
        TeamGroup team = findMyTeam(assignmentId, userId);
        if (team == null) {
            return Result.error("当前未加入队伍");
        }
        team = ensureAutoLock(team, assignment);

        if (!Objects.equals(team.getCaptainStudentId(), userId)) {
            return Result.error("只有队长可以解散队伍");
        }
        if (Boolean.TRUE.equals(team.getLocked())) {
            return Result.error("队伍成员已锁定，当前不可解散");
        }

        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsDeletedFalseOrderByIdAsc(team.getId());
        for (TeamMember member : members) {
            member.setIsDeleted(true);
        }
        teamMemberRepository.saveAll(members);

        team.setIsDeleted(true);
        team.setUpdateTime(LocalDateTime.now());
        teamGroupRepository.save(team);
        return Result.success();
    }

    @PostMapping("/assignments/{assignmentId}/teams/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<TeamGroupVO> confirmTeam(@PathVariable Integer assignmentId) {
        Integer userId = currentUserId();
        ExperimentAssignment assignment = getValidTeamAssignment(assignmentId);
        TeamGroup team = findMyTeam(assignmentId, userId);
        if (team == null) {
            return Result.error("当前未加入队伍");
        }
        if (!Objects.equals(team.getCaptainStudentId(), userId)) {
            return Result.error("只有队长可以确认队伍成员");
        }
        if (Boolean.TRUE.equals(team.getLocked())) {
            return Result.error("当前队伍已锁定");
        }

        lockTeam(team);
        return Result.success(toVO(team, userId));
    }

    private TeamGroup findMyTeam(Integer assignmentId, Integer userId) {
        ExperimentAssignment assignment = assignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        List<TeamMember> memberships = teamMemberRepository.findByStudentIdAndIsDeletedFalse(userId);
        for (TeamMember member : memberships) {
            TeamGroup team = teamGroupRepository.findByIdAndIsDeletedFalse(member.getTeamId());
            if (team != null && Objects.equals(team.getAssignmentId(), assignmentId)) {
                return ensureAutoLock(team, assignment);
            }
        }
        return null;
    }

    private TeamGroupVO toVO(TeamGroup team, Integer currentUserId) {
        TeamGroupVO vo = new TeamGroupVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setTeamCode(team.getTeamCode());
        vo.setCaptainId(team.getCaptainStudentId());
        vo.setMaxMembers(team.getMaxMembers() == null ? 3 : team.getMaxMembers());
        vo.setCaptain(currentUserId != null && Objects.equals(currentUserId, team.getCaptainStudentId()));
        vo.setLocked(Boolean.TRUE.equals(team.getLocked()));
        vo.setLockTime(team.getLockTime() == null ? "" : team.getLockTime().format(DATETIME_FORMATTER));

        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsDeletedFalseOrderByIdAsc(team.getId());
        List<Integer> userIds = new ArrayList<>();
        for (TeamMember member : members) {
            userIds.add(member.getStudentId());
        }

        Map<Integer, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                String displayName = user.getUsername();
                if (displayName == null || displayName.isBlank()) {
                    displayName = user.getNickname();
                }
                userNameMap.put(user.getId(), displayName == null ? "未知学生" : displayName);
            }
        }

        vo.setCaptainName(userNameMap.getOrDefault(team.getCaptainStudentId(), "未知学生"));

        List<String> memberNames = new ArrayList<>();
        memberNames.add(vo.getCaptainName());
        for (TeamMember member : members) {
            if (Objects.equals(member.getStudentId(), team.getCaptainStudentId())) {
                continue;
            }
            memberNames.add(userNameMap.getOrDefault(member.getStudentId(), "未知学生"));
        }
        while (memberNames.size() > vo.getMaxMembers()) {
            memberNames.remove(memberNames.size() - 1);
        }
        vo.setMemberNames(memberNames);
        vo.setMemberCount(members.size());
        return vo;
    }

    private ExperimentAssignment getValidTeamAssignment(Integer assignmentId) {
        ExperimentAssignment assignment = assignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            throw new RuntimeException("任务不存在");
        }
        if (assignment.getEvaluationMode() != EvaluationMode.TEAM) {
            throw new RuntimeException("当前任务未开启分组模式");
        }
        return assignment;
    }

    private void ensureWithinGroupDeadline(ExperimentAssignment assignment) {
        if (assignment == null) {
            return;
        }
        LocalDateTime deadline = assignment.getTeamGroupDeadline();
        if (deadline != null && !LocalDateTime.now().isBefore(deadline)) {
            throw new RuntimeException("自由组队时间已截止");
        }
    }

    private TeamGroup ensureAutoLock(TeamGroup team, ExperimentAssignment assignment) {
        if (team == null || assignment == null) {
            return team;
        }
        if (Boolean.TRUE.equals(team.getLocked())) {
            return team;
        }
        LocalDateTime teamGroupDeadline = assignment.getTeamGroupDeadline();
        if (teamGroupDeadline != null && !LocalDateTime.now().isBefore(teamGroupDeadline)) {
            lockTeam(team);
        }
        return team;
    }

    private void lockTeam(TeamGroup team) {
        team.setLocked(true);
        team.setLockTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        teamGroupRepository.save(team);
    }

    private String generateTeamCode() {
        while (true) {
            String code = "TEAM" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (!teamGroupRepository.existsByTeamCodeAndIsDeletedFalse(code)) {
                return code;
            }
        }
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object id = claims.get("id");
        if (id instanceof Integer integerId) {
            return integerId;
        }
        if (id instanceof Long longId) {
            return longId.intValue();
        }
        if (id instanceof String stringId) {
            return Integer.valueOf(stringId);
        }
        throw new RuntimeException("当前用户信息无效");
    }
}
