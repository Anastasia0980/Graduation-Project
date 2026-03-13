<template>
  <div class='detail-page'>
    <AppTopbar
      :logged-in='true'
      user-name='张三'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='detail-topbar'>
      <button class='back-btn' @click='goBack'>返回</button>
      <div class='page-title'>任务详情</div>
    </div>

    <div class='detail-body'>
      <div class='main-content'>
        <div class='content-card'>
          <div class='task-header'>
            <div>
              <div class='task-mode-badge'>{{ modeLabelText }}</div>
              <h1 class='task-title'>{{ taskTitle }}</h1>
              <div class='task-subtitle'>{{ taskSubtitle }}</div>
            </div>
            <div class='task-status'>开放中</div>
          </div>

          <section class='detail-section'>
            <h2>简介</h2>
            <div class='intro-block'>
              <div class='intro-text'>
                <p>
                  本任务基于 PettingZoo 提供的井字棋环境开展智能体测评与对战。学生需按照任务要求提交模型文件与必要配置，
                  平台将在统一环境中执行测评流程，并保存结果与相关记录。
                </p>
                <p>
                  当前任务模式为“{{ modeLabelText }}”。若为团队锦标赛模式，学生需在当前任务详情页完成创建队伍、加入队伍或退出队伍等操作，
                  并在队伍建立后查看当前对战安排。
                </p>
                <p>
                  该任务适合作为强化学习课程中的基础测评任务，用于考察学生对离散动作空间、状态建模、对战流程与模型提交规范的掌握情况。
                </p>
              </div>

              <div class='intro-image-box'>
                <img :src='tictactoeImage' alt='井字棋环境示意图' class='intro-image' />
              </div>
            </div>
          </section>

          <section class='detail-section'>
            <h2>规则</h2>
            <p>
              井字棋棋盘由 3 × 3 的九个格子组成，双方轮流在空位置落子。一方若先在横向、纵向或对角线上形成连续三个相同标记，则判定获胜。
              若棋盘全部填满且无人形成三连，则判定为平局。
            </p>
            <p>
              智能体只能在尚未被占用的位置执行动作。若输出非法动作，例如落在已被占用的位置，平台会将其视为违规操作并记录在结果中。
            </p>
          </section>

          <section class='detail-section'>
            <h2>观测</h2>
            <p>
              环境观测由当前棋盘状态构成，可视为对 9 个位置的离散描述。每个位置一般包括空位、己方落子、对方落子等信息。
              智能体需要根据当前观测选择合适的落子位置。
            </p>
          </section>

          <section class='detail-section'>
            <h2>动作空间</h2>
            <p>动作空间为离散动作空间，可表示为：</p>
            <div class='formula-box'>
              <span class='formula'>A = {0, 1, 2, 3, 4, 5, 6, 7, 8}</span>
            </div>
          </section>

          <section class='detail-section'>
            <h2>奖励</h2>
            <p>平台测评时可采用简化奖励设计来衡量智能体表现，例如：</p>
            <div class='formula-box'>
              <span class='formula'>R = winReward · I_win + drawReward · I_draw - invalidPenalty · I_invalid</span>
            </div>
          </section>

          <section class='detail-section'>
            <h2>评测说明</h2>
            <ul class='detail-list'>
              <li>测评环境：PettingZoo 井字棋环境</li>
              <li>任务类型：{{ modeLabelText }}</li>
              <li>模型要求：需提交可被平台脚本正常加载的模型与配置文件</li>
              <li>动作限制：仅允许输出合法离散动作</li>
              <li>时间限制：单步决策时间与总执行时间均受平台控制</li>
              <li>结果输出：平台保存结果、状态与视频记录</li>
            </ul>
          </section>
        </div>
      </div>

      <aside class='right-sidebar'>
        <div class='side-card'>
          <div class='side-title'>提交任务</div>

          <template v-if='taskMode === "single"'>
            <div class='side-desc'>
              当前任务为单人模式。学生提交模型后，平台将在预设环境中独立完成测评。
            </div>
            <button class='primary-btn submit-action-btn' @click='goSubmit'>提交单人测评</button>
          </template>

          <template v-else-if='taskMode === "battle"'>
            <div class='side-desc'>
              当前任务为对战模式。点击下方按钮后，可继续选择“真人对战”或“人机对战”。
            </div>
            <button class='primary-btn submit-action-btn' @click='showBattleDialog = true'>选择对战方式</button>

            <div class='mini-bot-info'>
              <div class='mini-bot-title'>人机模型配置情况</div>
              <div class='mini-bot-row'>
                <span>简单</span>
                <span :class='botStatusClass(hasEasyBot)'>{{ hasEasyBot ? '已配置' : '未配置' }}</span>
              </div>
              <div class='mini-bot-row'>
                <span>中等</span>
                <span :class='botStatusClass(hasMediumBot)'>{{ hasMediumBot ? '已配置' : '未配置' }}</span>
              </div>
              <div class='mini-bot-row'>
                <span>困难</span>
                <span :class='botStatusClass(hasHardBot)'>{{ hasHardBot ? '已配置' : '未配置' }}</span>
              </div>
            </div>
          </template>

          <template v-else>
            <div class='side-desc'>
              当前任务为团队锦标赛模式。请先完成队伍操作，再查看当前对战安排。
            </div>

            <div class='tournament-btn-group'>
              <button class='primary-btn' @click='showCreateTeamDialog = true'>创建队伍</button>
              <button class='secondary-btn side-secondary' @click='showJoinTeamDialog = true'>加入队伍</button>
              <button class='danger-btn' :disabled='!hasTeam' @click='showLeaveTeamDialog = true'>退出队伍</button>
              <button class='primary-btn' :disabled='!hasTeam' @click='goTournamentBracket'>查看当前对战安排</button>
            </div>

            <div class='team-info-box' v-if='hasTeam'>
              <div class='team-info-title'>当前队伍</div>
              <div class='team-info-row'>
                <span>队伍名称</span>
                <span>{{ teamInfo.name }}</span>
              </div>
              <div class='team-info-row'>
                <span>队伍码</span>
                <span>{{ teamInfo.teamCode }}</span>
              </div>
              <div class='team-info-row'>
                <span>队长</span>
                <span>{{ teamInfo.captain }}</span>
              </div>
              <div class='team-info-row'>
                <span>人数</span>
                <span>{{ teamInfo.members.length }} / 3</span>
              </div>
            </div>
          </template>
        </div>

        <div class='side-card'>
          <div class='side-title'>任务信息</div>
          <div class='mini-info-list'>
            <div class='mini-info-row'>
              <span class='mini-label'>模式</span>
              <span class='mini-value'>{{ modeLabelText }}</span>
            </div>
            <div class='mini-info-row'>
              <span class='mini-label'>环境</span>
              <span class='mini-value'>PettingZoo 井字棋</span>
            </div>
            <div class='mini-info-row'>
              <span class='mini-label'>教师</span>
              <span class='mini-value'>王老师</span>
            </div>
          </div>
        </div>

        <div class='side-card ranking-side-card'>
          <div class='side-title'>排行榜</div>
          <div class='ranking-top-list'>
            <div
              v-for='item in topThreeRanking'
              :key='item.rank'
              class='ranking-top-item'
            >
              <div class='ranking-left'>
                <span class='ranking-rank'>{{ item.rank }}</span>
                <span class='ranking-name'>{{ item.name }}</span>
              </div>
              <div class='ranking-score'>{{ item.score }}</div>
            </div>
          </div>
          <button class='secondary-btn show-all-btn' @click='showRankingDialog = true'>
            展开所有
          </button>
        </div>
      </aside>
    </div>

    <div v-if='showBattleDialog' class='dialog-mask' @click='showBattleDialog = false'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>选择对战方式</div>
          <button class='close-btn' @click='showBattleDialog = false'>关闭</button>
        </div>

        <div class='dialog-body'>
          <div class='battle-option'>
            <div class='battle-option-title'>真人对战</div>
            <div class='battle-option-desc'>提交模型后加入匹配池，系统自动匹配其他学生进行对战。</div>
            <button class='primary-btn option-btn' @click='goSubmit'>真人对战</button>
          </div>

          <div class='battle-option'>
            <div class='battle-option-title'>人机对战</div>
            <div class='battle-option-desc'>教师若上传了任意难度的人机模型，则本按钮可点击；否则保持灰色不可用。</div>
            <button
              class='option-btn'
              :class='hasAnyBotModel ? "primary-btn" : "disabled-btn"'
              :disabled='!hasAnyBotModel'
              @click='goSubmit'
            >
              人机对战
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if='showRankingDialog' class='dialog-mask' @click='showRankingDialog = false'>
      <div class='dialog-box ranking-dialog' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>完整排行榜</div>
          <button class='close-btn' @click='showRankingDialog = false'>关闭</button>
        </div>

        <div class='dialog-body ranking-dialog-body'>
          <table class='ranking-table'>
            <thead>
              <tr>
                <th>排名</th>
                <th>姓名</th>
                <th>分数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for='item in rankingList' :key='item.rank'>
                <td>{{ item.rank }}</td>
                <td>{{ item.name }}</td>
                <td>{{ item.score }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if='showCreateTeamDialog' class='dialog-mask' @click='closeCreateTeamDialog'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>创建队伍</div>
          <button class='close-btn' @click='closeCreateTeamDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='form-item'>
            <label>队伍名称</label>
            <input v-model='newTeamName' type='text' placeholder='请输入队伍名称' />
          </div>
          <div class='form-item'>
            <label>队伍码</label>
            <input value='TEAM2026A' type='text' disabled />
          </div>
          <div class='dialog-tip'>当前队伍码先写死为 TEAM2026A。</div>
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn footer-btn' @click='closeCreateTeamDialog'>取消</button>
          <button class='primary-btn footer-btn' @click='createTeam'>创建</button>
        </div>
      </div>
    </div>

    <div v-if='showJoinTeamDialog' class='dialog-mask' @click='closeJoinTeamDialog'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>加入队伍</div>
          <button class='close-btn' @click='closeJoinTeamDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='form-item'>
            <label>队伍码</label>
            <input v-model='joinTeamCode' type='text' placeholder='请输入队伍码' />
          </div>
          <div class='dialog-tip'>当前可输入示例队伍码：TEAM2026A</div>
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn footer-btn' @click='closeJoinTeamDialog'>取消</button>
          <button class='primary-btn footer-btn' @click='joinTeam'>加入</button>
        </div>
      </div>
    </div>

    <div v-if='showLeaveTeamDialog' class='dialog-mask' @click='showLeaveTeamDialog = false'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>退出队伍</div>
          <button class='close-btn' @click='showLeaveTeamDialog = false'>关闭</button>
        </div>
        <div class='dialog-body'>
          确定要退出当前队伍吗？
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn footer-btn' @click='showLeaveTeamDialog = false'>取消</button>
          <button class='danger-btn footer-btn' @click='leaveTeam'>退出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import tictactoeImage from '../assets/tictactoe.png'

export default {
  name: 'TaskDetailView',
  components: {
    AppTopbar
  },
  data () {
    return {
      tictactoeImage,
      taskTitle: '井字棋任务',
      taskMode: 'single',
      modeLabelText: '单人模式',
      taskSubtitle: 'PettingZoo 环境下的强化学习测评任务',
      hasEasyBot: false,
      hasMediumBot: false,
      hasHardBot: false,
      showBattleDialog: false,
      showRankingDialog: false,

      rankingList: [
        { rank: 1, name: '李四', score: 1280 },
        { rank: 2, name: '张三', score: 1205 },
        { rank: 3, name: '王五', score: 1150 },
        { rank: 4, name: '赵六', score: 1088 },
        { rank: 5, name: '陈七', score: 1020 },
        { rank: 6, name: '孙八', score: 980 }
      ],

      showCreateTeamDialog: false,
      showJoinTeamDialog: false,
      showLeaveTeamDialog: false,
      newTeamName: '',
      joinTeamCode: '',
      hasTeam: false,
      teamInfo: {
        name: '',
        teamCode: '',
        captain: '',
        members: []
      }
    }
  },
  computed: {
    hasAnyBotModel () {
      return this.hasEasyBot || this.hasMediumBot || this.hasHardBot
    },
    topThreeRanking () {
      return this.rankingList.slice(0, 3)
    }
  },
  created () {
    this.initTaskInfo()
  },
  watch: {
    '$route.query': {
      handler () {
        this.initTaskInfo()
      },
      deep: true
    }
  },
  methods: {
    initTaskInfo () {
      const title = this.$route.query.title
      const taskMode = this.$route.query.taskMode || 'single'

      this.taskTitle = title || '井字棋任务'
      this.taskMode = taskMode
      this.hasEasyBot = this.$route.query.hasEasyBot === 'true'
      this.hasMediumBot = this.$route.query.hasMediumBot === 'true'
      this.hasHardBot = this.$route.query.hasHardBot === 'true'

      if (taskMode === 'single') {
        this.modeLabelText = '单人模式'
        this.taskSubtitle = 'PettingZoo 环境下的单模型独立测评任务'
      } else if (taskMode === 'battle') {
        this.modeLabelText = '对战模式'
        this.taskSubtitle = '学生点击提交后可继续选择真人对战或人机对战'
      } else {
        this.modeLabelText = '团队锦标赛'
        this.taskSubtitle = '学生在当前任务详情页中完成组队、退队与查看对战安排等操作'
      }
    },
    botStatusClass (status) {
      return status ? 'status-ok' : 'status-off'
    },
    createTeam () {
      const teamName = this.newTeamName.trim() || '未命名队伍'
      this.teamInfo = {
        name: teamName,
        teamCode: 'TEAM2026A',
        captain: '张三',
        members: [
          { studentId: '2023123456', name: '张三', role: '队长' }
        ]
      }
      this.hasTeam = true
      this.closeCreateTeamDialog()
    },
    joinTeam () {
      if (!this.joinTeamCode.trim()) {
        alert('请输入队伍码')
        return
      }
      this.teamInfo = {
        name: 'Alpha 队',
        teamCode: 'TEAM2026A',
        captain: '李四',
        members: [
          { studentId: '2023123457', name: '李四', role: '队长' },
          { studentId: '2023123456', name: '张三', role: '成员' }
        ]
      }
      this.hasTeam = true
      this.closeJoinTeamDialog()
    },
    leaveTeam () {
      this.hasTeam = false
      this.teamInfo = {
        name: '',
        teamCode: '',
        captain: '',
        members: []
      }
      this.showLeaveTeamDialog = false
    },
    closeCreateTeamDialog () {
      this.showCreateTeamDialog = false
      this.newTeamName = ''
    },
    closeJoinTeamDialog () {
      this.showJoinTeamDialog = false
      this.joinTeamCode = ''
    },
    goTournamentBracket () {
      this.$router.push('/student/tournament')
    },
    goBack () {
      this.$router.back()
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    },
    goSubmit () {
      this.showBattleDialog = false
      this.$router.push('/battle')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  color: #303133;
}

.detail-topbar {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.back-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-body {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 20px;
  padding: 20px;
  align-items: start;
}

.main-content {
  min-width: 0;
}

.content-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 24px;
}

.task-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 24px;
}

.task-mode-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  margin-bottom: 12px;
  border-radius: 4px;
  background: #ecf5ff;
  color: #1f4e8c;
  font-size: 13px;
  font-weight: 700;
}

.task-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2d3d;
}

.task-subtitle {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.task-status {
  min-width: 72px;
  height: 32px;
  padding: 0 12px;
  background: #ecf5ff;
  color: #1f4e8c;
  border: 1px solid #bfd7f4;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.detail-section {
  margin-bottom: 28px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h2 {
  margin: 0 0 14px;
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-section p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.9;
  color: #303133;
}

.intro-block {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
  align-items: start;
}

.intro-image-box {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.intro-image {
  width: 100%;
  border-radius: 6px;
  display: block;
}

.formula-box {
  margin: 12px 0 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow-x: auto;
}

.formula {
  font-size: 15px;
  color: #1f2d3d;
  font-family: 'Times New Roman', serif;
}

.detail-list {
  margin: 0 0 12px 18px;
  padding: 0;
  color: #303133;
}

.detail-list li {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.8;
}

.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 84px;
}

.side-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.side-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 14px;
}

.side-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 16px;
}

.primary-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn:hover {
  background: #173b69;
}

.secondary-btn {
  width: 100%;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
}

.secondary-btn:hover {
  border-color: #1f4e8c;
  color: #1f4e8c;
}

.side-secondary {
  width: 100%;
}

.danger-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 4px;
  background: #d9534f;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.danger-btn:hover {
  background: #c9302c;
}

.danger-btn:disabled,
.secondary-btn:disabled,
.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.tournament-btn-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-info-box {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.team-info-title,
.mini-bot-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2d3d;
}

.team-info-row,
.mini-bot-row,
.mini-info-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 10px;
}

.team-info-row:last-child,
.mini-bot-row:last-child,
.mini-info-row:last-child {
  margin-bottom: 0;
}

.mini-bot-info {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.status-ok {
  color: #1f4e8c;
  font-weight: 700;
}

.status-off {
  color: #909399;
}

.mini-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mini-label {
  font-size: 14px;
  color: #909399;
}

.mini-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  text-align: right;
}

.ranking-side-card {
  padding-bottom: 16px;
}

.ranking-top-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ranking-top-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.ranking-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ranking-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1f4e8c;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.ranking-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.ranking-score {
  font-size: 14px;
  color: #1f4e8c;
  font-weight: 700;
}

.show-all-btn {
  margin-top: 14px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(31, 45, 61, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 20px;
}

.dialog-box {
  width: 560px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.small-box {
  width: 420px;
}

.ranking-dialog {
  width: 720px;
}

.dialog-header {
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.close-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.dialog-body {
  padding: 20px;
}

.ranking-dialog-body {
  max-height: 460px;
  overflow-y: auto;
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;
}

.ranking-table th,
.ranking-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 12px;
  text-align: left;
  font-size: 14px;
  color: #303133;
}

.ranking-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.battle-option {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
  margin-bottom: 16px;
}

.battle-option:last-child {
  margin-bottom: 0;
}

.battle-option-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.battle-option-desc {
  margin-bottom: 14px;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.option-btn {
  width: 100%;
}

.disabled-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 4px;
  background: #c0c4cc;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.dialog-tip {
  font-size: 13px;
  color: #909399;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.footer-btn {
  min-width: 96px;
}

@media (max-width: 1100px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .right-sidebar {
    position: static;
  }
}

@media (max-width: 900px) {
  .intro-block {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .detail-topbar {
    height: auto;
    padding: 14px 16px;
    flex-wrap: wrap;
  }

  .detail-body {
    padding: 16px;
  }

  .content-card,
  .side-card {
    padding: 16px;
  }

  .task-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-title {
    font-size: 24px;
  }
}
</style>
