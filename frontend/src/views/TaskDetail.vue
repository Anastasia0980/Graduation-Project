<template>
  <div class='detail-page'>
    <AppTopbar
      :logged-in='true'
      :user-name='displayUserName'
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
            <div class='task-status'>{{ taskStatusText }}</div>
          </div>

          <section class='detail-section'>
            <h2>可选模型</h2>
            <div v-if='algorithmTags.length > 0' class='algorithm-tag-list'>
              <span
                v-for='item in algorithmTags'
                :key='item'
                class='algorithm-tag'
              >
                {{ item }}
              </span>
            </div>
            <div v-else class='empty-algorithm-text'>当前未配置可选模型</div>
          </section>

          <section class='detail-section'>
            <h2>简介</h2>
            <div class='intro-block'>
              <div class='intro-text'>
                <p>{{ introText }}</p>
              </div>

              <div class='intro-image-box'>
                <img :src='taskImage' alt='任务图标' class='intro-image' />
              </div>
            </div>
          </section>

          <section class='detail-section'>
            <h2>规则</h2>
            <p>{{ ruleText }}</p>
          </section>

          <section class='detail-section'>
            <h2>观测</h2>
            <p>{{ observationText }}</p>
          </section>

          <section class='detail-section'>
            <h2>动作空间</h2>
            <p>{{ actionSpaceText }}</p>
          </section>

          <section class='detail-section'>
            <h2>奖励</h2>
            <p>{{ rewardText }}</p>
          </section>

          <section class='detail-section'>
            <h2>评测说明</h2>
            <p>{{ evaluationText }}</p>
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
            <button
              class='primary-btn submit-action-btn'
              :class='{ "ended-submit-btn": isTaskEnded }'
              @click='handleSingleSubmitClick'
            >
              提交单人测评
            </button>
          </template>

          <template v-else-if='taskMode === "battle"'>
            <div class='side-desc'>
              当前任务为对战模式。请先上传自己的对战模型，上传成功后可在“已提交模型”中选择自己的模型，再选择其他同学已上传的模型发起异步挑战。
            </div>
            <button
              class='primary-btn submit-action-btn'
              :class='{ "ended-submit-btn": isTaskEnded }'
              @click='handleBattleEntryClick'
            >
              上传或选择对战方式
            </button>
            <button
              class='secondary-btn submit-action-btn side-secondary'
              @click='openMyBattleModelsDialog'
            >
              查看已提交模型
            </button>

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
              当前任务为分组对战模式。学生需先完成组队，再由队长统一上传模型；后续队伍可像多人对战一样自主选择其他队伍发起异步挑战。
            </div>

            <div class='tournament-btn-group'>
              <button class='primary-btn' :disabled='hasTeam' @click='showCreateTeamDialog = true'>创建队伍</button>
              <button class='secondary-btn side-secondary' :disabled='hasTeam' @click='showJoinTeamDialog = true'>加入队伍</button>
              <button class='danger-btn' :disabled='!hasTeam || !teamInfo.isCaptain' @click='showDissolveTeamDialog = true'>解散队伍</button>
              <button class='danger-btn side-secondary' :disabled='!hasTeam || teamInfo.isCaptain' @click='showLeaveTeamDialog = true'>退出队伍</button>
              <button class='primary-btn' :disabled='!hasTeam' @click='handleTeamBattleEntryClick'>上传模型</button>
              <button class='secondary-btn side-secondary' :disabled='!hasTeam' @click='openMyBattleModelsDialog'>查看已提交模型</button>
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
              <div class='team-member-stack'>
                <div class='team-member-row'>
                  <span>队长</span>
                  <span>{{ displayTeamSlot(0) }}</span>
                </div>
                <div class='team-member-row'>
                  <span>队员1</span>
                  <span>{{ displayTeamSlot(1) }}</span>
                </div>
                <div class='team-member-row'>
                  <span>队员2</span>
                  <span>{{ displayTeamSlot(2) }}</span>
                </div>
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
              <span class='mini-value'>{{ environmentText }}</span>
            </div>
            <div class='mini-info-row'>
              <span class='mini-label'>任务截止时间</span>
              <span class='mini-value'>{{ deadlineText }}</span>
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
                <span class='ranking-name'>{{ item.displayName }}</span>
              </div>
              <div class='ranking-score'>{{ item.score }}</div>
            </div>
          </div>
          <button class='secondary-btn show-all-btn' @click='goRankingDetail'>
            查看详情
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
            <div class='battle-option-desc'>先上传当前版本模型并保存到“已提交模型”，后续可随时返回选择自己的模型，再指定其他同学的模型发起挑战。</div>
            <button class='primary-btn option-btn' @click='openSubmitDialog("human")'>上传真人对战模型</button>
            <button class='secondary-btn option-btn side-secondary' @click='openMyBattleModelsDialog'>选择已提交模型</button>
          </div>

          <div class='battle-option'>
            <div class='battle-option-title'>人机对战</div>
            <div class='battle-option-desc'>教师若上传了任意难度的人机模型，则可继续选择具体难度进行对战。</div>
            <button
              class='option-btn'
              :class='hasAnyBotModel ? "primary-btn" : "disabled-btn"'
              :disabled='!hasAnyBotModel'
              @click='openBotDifficultyDialog'
            >
              人机对战
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if='showBotDifficultyDialog' class='dialog-mask' @click='closeBotDifficultyDialog'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>选择人机难度</div>
          <button class='close-btn' @click='closeBotDifficultyDialog'>关闭</button>
        </div>

        <div class='dialog-body'>
          <div class='action-btn-group'>
            <button
              class='option-btn'
              :class='hasEasyBot ? "primary-btn" : "disabled-btn"'
              :disabled='!hasEasyBot'
              @click='openSubmitDialog("bot-easy")'
            >
              简单人机对战
            </button>
            <button
              class='option-btn'
              :class='hasMediumBot ? "primary-btn" : "disabled-btn"'
              :disabled='!hasMediumBot'
              @click='openSubmitDialog("bot-medium")'
            >
              中等人机对战
            </button>
            <button
              class='option-btn'
              :class='hasHardBot ? "primary-btn" : "disabled-btn"'
              :disabled='!hasHardBot'
              @click='openSubmitDialog("bot-hard")'
            >
              困难人机对战
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if='showEndedDialog' class='dialog-mask' @click='showEndedDialog = false'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>任务提示</div>
          <button class='close-btn' @click='showEndedDialog = false'>关闭</button>
        </div>
        <div class='dialog-body'>
          任务已结束，不可提交。
        </div>
        <div class='dialog-footer'>
          <button class='primary-btn footer-btn' @click='showEndedDialog = false'>确定</button>
        </div>
      </div>
    </div>

    <div v-if='showSubmitDialog' class='dialog-mask' @click='closeSubmitDialog'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>提交模型</div>
          <button class='close-btn' @click='closeSubmitDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='dialog-tip submit-mode-tip'>当前提交方式：{{ submitModeText }}</div>

          <div class='form-item'>
            <label>Config</label>
            <div class='file-picker-row'>
              <input
                ref='configInput'
                class='hidden-file-input'
                type='file'
                accept='.json'
                @change='handleFileChange($event, "config")'
              />
              <button class='file-select-btn' type='button' @click='triggerFileSelect("config")'>
                选择文件
              </button>
              <div class='file-name-box'>
                {{ selectedFiles.config ? selectedFiles.config.name : '未选择配置文件' }}
              </div>
              <button
                class='file-remove-btn'
                type='button'
                :disabled='!selectedFiles.config'
                @click='clearSelectedFile("config")'
              >
                删除
              </button>
            </div>
          </div>

          <div class='form-item'>
            <label>Model</label>
            <div class='file-picker-row'>
              <input
                ref='modelInput'
                class='hidden-file-input'
                type='file'
                accept='.pt,.pth'
                @change='handleFileChange($event, "model")'
              />
              <button class='file-select-btn' type='button' @click='triggerFileSelect("model")'>
                选择文件
              </button>
              <div class='file-name-box'>
                {{ selectedFiles.model ? selectedFiles.model.name : '未选择模型文件' }}
              </div>
              <button
                class='file-remove-btn'
                type='button'
                :disabled='!selectedFiles.model'
                @click='clearSelectedFile("model")'
              >
                删除
              </button>
            </div>
          </div>

          <div v-if='submitMessage' class='feedback-box' :class='submitFeedbackClass'>
            {{ submitMessage }}
          </div>

          <div v-if='evaluationId' class='feedback-box success-box'>
            提交成功：evaluationId = {{ evaluationId }}
          </div>
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn footer-btn' @click='closeSubmitDialog'>取消</button>
          <button class='primary-btn footer-btn' :disabled='loadingSubmit' @click='submit'>
            {{ loadingSubmit ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if='showMyBattleModelsDialog' class='dialog-mask' @click='closeMyBattleModelsDialog'>
      <div class='dialog-box large-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>已提交模型</div>
          <button class='close-btn' @click='closeMyBattleModelsDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='task-dialog-top'>
            <div class='dialog-tip'>当前展示你在该任务下已上传的对战模型。选择其中一个模型后，可继续选择其他同学的模型发起挑战。</div>
            <button class='secondary-inline-btn' :disabled='myBattleModelsLoading' @click='loadMyBattleModels'>
              {{ myBattleModelsLoading ? '刷新中...' : '刷新列表' }}
            </button>
          </div>

          <div v-if='myBattleModelsMessage' class='feedback-box' :class='myBattleModelsFeedbackClass'>
            {{ myBattleModelsMessage }}
          </div>

          <div v-if='myBattleModels.length > 0' class='battle-model-list'>
            <div
              v-for='item in myBattleModels'
              :key='item.submissionId'
              class='battle-model-item'
            >
              <div class='battle-model-main'>
                <div class='battle-model-name'>{{ item.modelName || 'model.pt' }}</div>
                <div class='battle-model-meta'>
                  <span>提交时间：{{ item.submitTime || '--' }}</span>
                  <span>战绩：{{ item.winCount }}胜 {{ item.loseCount }}负 {{ item.drawCount }}平</span>
                </div>
              </div>
              <div class='battle-model-actions'>
                <button class='primary-btn small-action-btn' @click='openOpponentDialog(item)'>发起挑战</button>
              </div>
            </div>
          </div>

          <div v-else class='empty-state'>当前暂无已提交模型，请先上传模型。</div>
        </div>
      </div>
    </div>

    <div v-if='showOpponentDialog' class='dialog-mask' @click='closeOpponentDialog'>
      <div class='dialog-box large-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>选择对手模型</div>
          <button class='close-btn' @click='closeOpponentDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='dialog-tip opponent-selected-tip'>
            当前出战模型：{{ selectedMyBattleModel ? selectedMyBattleModel.modelName : '--' }}
          </div>

          <div v-if='opponentModelsMessage' class='feedback-box' :class='opponentModelsFeedbackClass'>
            {{ opponentModelsMessage }}
          </div>

          <div v-if='opponentModels.length > 0' class='battle-model-list'>
            <div
              v-for='item in opponentModels'
              :key='item.submissionId'
              class='battle-model-item'
            >
              <div class='battle-model-main'>
                <div class='battle-model-name'>{{ item.studentName }} - {{ item.modelName || 'model.pt' }}</div>
                <div class='battle-model-meta'>
                  <span>提交时间：{{ item.submitTime || '--' }}</span>
                  <span>战绩：{{ item.winCount }}胜 {{ item.loseCount }}负 {{ item.drawCount }}平</span>
                </div>
              </div>
              <div class='battle-model-actions'>
                <button class='primary-btn small-action-btn' :disabled='challengeLoading || (taskMode === "tournament" && !teamInfo.isCaptain)' @click='challengeOpponent(item)'>
                  {{ challengeLoading ? '发起中...' : '挑战' }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class='empty-state'>当前暂无可挑战的其他同学模型。</div>
        </div>
      </div>
    </div>

    <div v-if='showTaskListDialog' class='dialog-mask' @click='closeTaskListDialog'>
      <div class='dialog-box large-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>查看测评</div>
          <button class='close-btn' @click='closeTaskListDialog'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='task-dialog-top'>
            <div class='dialog-tip'>当前展示后端返回的对战测评任务列表。</div>
            <button class='secondary-inline-btn' :disabled='queryLoading' @click='loadTasks'>
              {{ queryLoading ? '刷新中...' : '刷新列表' }}
            </button>
          </div>

          <div v-if='queryMessage' class='feedback-box' :class='queryFeedbackClass'>
            {{ queryMessage }}
          </div>

          <div v-if='taskList.length > 0' class='task-table-wrap'>
            <table class='task-table'>
              <thead>
                <tr>
                  <th>任务编号</th>
                  <th>当前状态</th>
                  <th>学生1</th>
                  <th>学生2</th>
                  <th>胜负关系</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='task in taskList' :key='task.evaluationId'>
                  <td>{{ task.evaluationId }}</td>
                  <td>{{ task.status }}</td>
                  <td>{{ task.student1Id }}</td>
                  <td>{{ task.student2Id }}</td>
                  <td>{{ task.winnerText }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class='empty-state'>当前暂无测评记录。</div>
        </div>
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
          <div class='dialog-tip'>创建成功后会自动生成队伍码，并显示在当前队伍信息中。</div>
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
          <div class='dialog-tip'>请输入队长提供的真实队伍码。</div>
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

    <div v-if='showDissolveTeamDialog' class='dialog-mask' @click='showDissolveTeamDialog = false'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>解散队伍</div>
          <button class='close-btn' @click='showDissolveTeamDialog = false'>关闭</button>
        </div>
        <div class='dialog-body'>
          确定要解散当前队伍吗？解散后队伍成员将全部退出队伍。
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn footer-btn' @click='showDissolveTeamDialog = false'>取消</button>
          <button class='danger-btn footer-btn' @click='dissolveTeam'>解散</button>
        </div>
      </div>
    </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import tictactoeImage from '../assets/tictactoe.png'

const API_BASE = 'http://localhost:8080'

function normalizeFileUrl (url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  if (url.startsWith('/')) return `${API_BASE}${url}`
  return `${API_BASE}/${url}`
}

export default {
  name: 'TaskDetailView',
  components: {
    AppTopbar
  },
  data () {
    return {
      taskImage: tictactoeImage,
      loadingPage: false,
      loadingSubmit: false,
      taskTitle: '井字棋任务',
      taskMode: 'single',
      modeLabelText: '单人模式',
      taskSubtitle: 'PettingZoo 环境下的强化学习测评任务',
      taskStatusText: '开放中',
      environmentText: 'tictactoe_v3',
      deadlineText: '--',
      taskEnvironmentValue: 'tictactoe_v3',
      introText: '当前暂无任务简介。',
      ruleText: '当前暂无任务规则说明。',
      observationText: '当前暂无观测说明。',
      actionSpaceText: '当前暂无动作空间说明。',
      rewardText: '当前暂无奖励说明。',
      evaluationText: '当前暂无评测说明。',
      algorithmTags: [],
      hasEasyBot: false,
      hasMediumBot: false,
      hasHardBot: false,
      showBattleDialog: false,
      showBotDifficultyDialog: false,
      showEndedDialog: false,
      showSubmitDialog: false,
      showTaskListDialog: false,
      showMyBattleModelsDialog: false,
      showOpponentDialog: false,
      currentSubmitMode: 'single',
      currentBotDifficulty: '',
      queryLoading: false,
      myBattleModelsLoading: false,
      challengeLoading: false,
      submitMessage: '',
      queryMessage: '',
      evaluationId: null,
      selectedFiles: {
        config: null,
        model: null
      },
      taskList: [],
      myBattleModels: [],
      opponentModels: [],
      selectedMyBattleModel: null,
      rankingList: [],
      myBattleModelsMessage: '',
      opponentModelsMessage: '',
      showCreateTeamDialog: false,
      showJoinTeamDialog: false,
      showLeaveTeamDialog: false,
      showDissolveTeamDialog: false,
      teamLoading: false,
      teamActionLoading: false,
      newTeamName: '',
      joinTeamCode: '',
      hasTeam: false,
      teamInfo: {
        teamId: null,
        name: '',
        teamCode: '',
        captain: '',
        members: [],
        memberCount: 0,
        maxMembers: 3,
        isCaptain: false
      }
    }
  },
  computed: {
    hasAnyBotModel () {
      return this.hasEasyBot || this.hasMediumBot || this.hasHardBot
    },
    topThreeRanking () {
      return this.rankingList.slice(0, 3)
    },
    isTaskEnded () {
      return this.taskStatusText === '已结束'
    },
    submitModeText () {
      if (this.currentSubmitMode === 'human') {
        return '真人对战'
      }
      if (this.currentSubmitMode === 'bot-easy') {
        return '简单人机对战'
      }
      if (this.currentSubmitMode === 'bot-medium') {
        return '中等人机对战'
      }
      if (this.currentSubmitMode === 'bot-hard') {
        return '困难人机对战'
      }
      return '单人测评'
    },
    submitFeedbackClass () {
      return this.submitMessage.startsWith('提交失败') || this.submitMessage.startsWith('请')
        ? 'error-box'
        : 'success-box'
    },
    queryFeedbackClass () {
      return this.queryMessage.startsWith('查询失败') ? 'error-box' : 'success-box'
    },
    myBattleModelsFeedbackClass () {
      return this.myBattleModelsMessage.startsWith('加载失败') ? 'error-box' : 'success-box'
    },
    opponentModelsFeedbackClass () {
      return this.opponentModelsMessage.startsWith('发起失败') || this.opponentModelsMessage.startsWith('加载失败')
        ? 'error-box'
        : 'success-box'
    },
    displayUserName () {
      return localStorage.getItem('auth_name') || ''
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
    async initTaskInfo () {
      const title = this.$route.query.title
      const taskMode = this.$route.query.taskMode || 'single'
      const assignmentId = this.getAssignmentId()

      this.taskTitle = title || '井字棋任务'
      this.taskMode = taskMode
      this.applyTaskModeInfo(taskMode)

      if (assignmentId) {
        await this.loadTaskDetail(assignmentId)
        if (this.taskMode === 'battle') {
          await this.loadBotStatus(assignmentId)
        }
        if (this.taskMode === 'tournament') {
          await this.loadMyTeam(assignmentId)
        }
        await this.loadRankingList(assignmentId)
      } else {
        this.rankingList = []
        this.hasEasyBot = this.$route.query.hasEasyBot === 'true'
        this.hasMediumBot = this.$route.query.hasMediumBot === 'true'
        this.hasHardBot = this.$route.query.hasHardBot === 'true'
      }
    },
    getAssignmentId () {
      return this.$route.query.assignmentId || this.$route.query.taskId || this.$route.query.id || ''
    },
    async loadTaskDetail (assignmentId) {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('当前未登录或登录已过期，请重新登录')
        return
      }

      const role = (localStorage.getItem('auth_role') || '').toUpperCase()
      const listUrl = role === 'TEACHER'
        ? `${API_BASE}/teacher/assignments?pageNum=0&pageSize=100`
        : `${API_BASE}/me/assignments?pageNum=0&pageSize=100`

      this.loadingPage = true
      try {
        const response = await fetch(listUrl, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '任务详情加载失败')
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        const currentTask = content.find(item => String(item.id) === String(assignmentId))

        if (!currentTask) {
          return
        }

        this.fillTaskDetail(currentTask)
      } catch (error) {
        ElMessage.error(error.message || '任务详情加载失败')
      } finally {
        this.loadingPage = false
      }
    },
    async loadBotStatus (assignmentId) {
      const token = localStorage.auth_token
        ? `Bearer ${localStorage.auth_token}`
        : ''

      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/system-bots/status`, {
          method: 'GET',
          headers: {
            Authorization: token
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0 || !result.data) {
          this.hasEasyBot = false
          this.hasMediumBot = false
          this.hasHardBot = false
          return
        }

        this.hasEasyBot = !!result.data.easy
        this.hasMediumBot = !!result.data.medium
        this.hasHardBot = !!result.data.hard
      } catch (error) {
        this.hasEasyBot = false
        this.hasMediumBot = false
        this.hasHardBot = false
      }
    },
    fillTaskDetail (task) {
      const config = this.parseTaskConfig(task)
      const taskMode = this.mapTaskMode(task.evaluationMode)

      this.taskTitle = titleSafe(task.title, this.taskTitle)
      this.taskMode = taskMode
      this.applyTaskModeInfo(taskMode)

      this.environmentText = task.environment || 'tictactoe_v3'
      this.taskEnvironmentValue = task.environment || 'tictactoe_v3'
      this.deadlineText = this.formatDateTime(task.deadline)
      this.taskStatusText = this.computeTaskStatus(task.deadline)

      this.introText = config.overview || '当前暂无任务简介。'
      this.ruleText = config.rules || '当前暂无任务规则说明。'
      this.observationText = config.observationSpace || '当前暂无观测说明。'
      this.actionSpaceText = config.actionSpace || '当前暂无动作空间说明。'
      this.rewardText = config.rewardFunction || '当前暂无奖励说明。'
      this.evaluationText = config.evaluationFunction || '当前暂无评测说明。'
      this.algorithmTags = this.parseAgentNames(task.agentName)
      this.taskImage = task.taskIcon ? normalizeFileUrl(task.taskIcon) : tictactoeImage
    },
    parseTaskConfig (task) {
      if (task.config && typeof task.config === 'object') {
        return task.config
      }
      if (task.configJson) {
        try {
          return JSON.parse(task.configJson)
        } catch (error) {
          return {}
        }
      }
      return {}
    },
    parseAgentNames (value) {
      if (!value) {
        return []
      }
      return String(value)
        .split(',')
        .map(item => item.trim())
        .filter(item => item)
    },
    mapTaskMode (evaluationMode) {
      const mode = (evaluationMode || '').toUpperCase()
      if (mode === 'VERSUS' || mode === 'BATTLE') {
        return 'battle'
      }
      if (mode === 'TEAM' || mode === 'TOURNAMENT') {
        return 'tournament'
      }
      return 'single'
    },
    applyTaskModeInfo (taskMode) {
      if (taskMode === 'single') {
        this.modeLabelText = '单人模式'
        this.taskSubtitle = 'PettingZoo 环境下的单模型独立测评任务'
      } else if (taskMode === 'battle') {
        this.modeLabelText = '对战模式'
        this.taskSubtitle = '学生先上传模型保存，再从已提交模型中选择自己的模型并指定对手发起挑战'
      } else {
        this.modeLabelText = '分组对战模式'
        this.taskSubtitle = '学生在当前任务详情页中完成组队、上传队伍模型，并以队伍为单位发起异步挑战'
      }
    },

    async loadRankingList (assignmentId) {
      if (!assignmentId || this.taskMode === 'single') {
        this.rankingList = []
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.rankingList = []
        return
      }

      const path = this.taskMode === 'tournament'
        ? `/assignments/${assignmentId}/team-leaderboard?pageNum=0&pageSize=3`
        : `/assignments/${assignmentId}/leaderboard?pageNum=0&pageSize=3`

      try {
        const response = await fetch(`${API_BASE}${path}`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '排行榜加载失败')
        }
        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.rankingList = content.map(item => ({
          rank: item.rank,
          displayName: this.taskMode === 'tournament' ? (item.teamName || '未知队伍') : (item.nickname || '未知学生'),
          score: item.ladderScore ?? item.bestScore ?? 0
        }))
      } catch (error) {
        this.rankingList = []
      }
    },
    handleTeamBattleEntryClick () {
      if (!this.hasTeam) {
        ElMessage.warning('请先创建或加入队伍')
        return
      }
      if (!this.teamInfo.isCaptain) {
        ElMessage.warning('仅队长可上传模型')
        return
      }
      this.openSubmitDialog('human')
    },
    goRankingDetail () {
      if (this.taskMode === 'single') {
        return
      }
      const role = (localStorage.getItem('auth_role') || '').toUpperCase()
      const path = role === 'TEACHER' ? '/teacher/ranking' : '/student/ranking'
      const mode = this.taskMode === 'tournament' ? 'team' : 'versus'
      this.$router.push({
        path,
        query: {
          mode,
          taskId: this.getAssignmentId()
        }
      })
    },
    formatDateTime (value) {
      if (!value) {
        return '--'
      }

      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')

      return `${year}-${month}-${day} ${hour}:${minute}`
    },
    computeTaskStatus (deadline) {
      if (!deadline) {
        return '开放中'
      }

      const deadlineDate = new Date(deadline)
      if (Number.isNaN(deadlineDate.getTime())) {
        return '开放中'
      }

      return deadlineDate.getTime() < Date.now() ? '已结束' : '开放中'
    },
    botStatusClass (status) {
      return status ? 'status-ok' : 'status-off'
    },
    handleSingleSubmitClick () {
      if (this.isTaskEnded) {
        this.showEndedDialog = true
        return
      }
      this.openSubmitDialog('single')
    },
    handleBattleEntryClick () {
      if (this.isTaskEnded) {
        this.showEndedDialog = true
        return
      }
      this.showBattleDialog = true
    },
    openBotDifficultyDialog () {
      this.showBattleDialog = false
      this.showBotDifficultyDialog = true
    },
    closeBotDifficultyDialog () {
      this.showBotDifficultyDialog = false
    },
    resetSubmissionState () {
      this.submitMessage = ''
      this.queryMessage = ''
      this.evaluationId = null
      this.selectedFiles = {
        config: null,
        model: null
      }
      this.clearNativeFileInput('config')
      this.clearNativeFileInput('model')
    },
    openSubmitDialog (mode) {
      this.showBattleDialog = false
      this.showBotDifficultyDialog = false
      this.currentSubmitMode = mode
      this.currentBotDifficulty = mode.startsWith('bot-') ? mode.replace('bot-', '') : ''
      this.resetSubmissionState()
      this.showSubmitDialog = true
    },
    closeSubmitDialog () {
      this.showSubmitDialog = false
      this.loadingSubmit = false
    },
    async openTaskListDialog () {
      await this.loadTasks()
      this.showTaskListDialog = true
    },
    closeTaskListDialog () {
      this.showTaskListDialog = false
    },
    triggerFileSelect (type) {
      if (type === 'config' && this.$refs.configInput) {
        this.$refs.configInput.click()
      }
      if (type === 'model' && this.$refs.modelInput) {
        this.$refs.modelInput.click()
      }
    },
    handleFileChange (e, type) {
      this.selectedFiles[type] = e.target.files[0] || null
    },
    clearNativeFileInput (type) {
      if (type === 'config' && this.$refs.configInput) {
        this.$refs.configInput.value = ''
      }
      if (type === 'model' && this.$refs.modelInput) {
        this.$refs.modelInput.value = ''
      }
    },
    clearSelectedFile (type) {
      this.selectedFiles[type] = null
      this.clearNativeFileInput(type)
    },
    async submit () {
      if (!this.selectedFiles.config || !this.selectedFiles.model) {
        this.submitMessage = '请同时选择 config 和 model 文件'
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.submitMessage = '当前未登录或登录已过期，请重新登录'
        return
      }

      const form = new FormData()
      form.append('model', this.selectedFiles.model)
      form.append('config', this.selectedFiles.config)

      if (this.currentBotDifficulty) {
        form.append('difficulty', this.currentBotDifficulty)
      }

      this.loadingSubmit = true
      this.submitMessage = ''
      this.evaluationId = null

      try {
        const assignmentId = this.getAssignmentId()

        let submitUrl = ''
        if (this.currentSubmitMode === 'human') {
          submitUrl = `${API_BASE}/battle/models/${assignmentId}`
        } else if (this.currentSubmitMode.startsWith('bot-')) {
          submitUrl = `${API_BASE}/battle/bot-submit/${assignmentId}`
        } else if (this.currentSubmitMode === 'single') {
          submitUrl = `${API_BASE}/assignments/${assignmentId}/evaluations`
        } else {
          submitUrl = `${API_BASE}/evaluations/uploadAndRun`
        }

        const resp = await fetch(submitUrl, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: form
        })

        const res = await resp.json()

        if (!resp.ok || res.code !== 0) {
          throw new Error(res.message || '提交失败')
        }

        const payload = res.data || {}
        this.submitMessage = payload.message || '提交成功'

        if (payload.evaluationId) {
          this.evaluationId = payload.evaluationId
        } else if (payload.id) {
          this.evaluationId = payload.id
        }

        if (this.currentSubmitMode === 'human') {
          await this.loadMyBattleModels()
        }
      } catch (e) {
        this.submitMessage = `提交失败：${e.message}`
      } finally {
        this.loadingSubmit = false
      }
    },
    async openMyBattleModelsDialog () {
      if (this.isTaskEnded) {
        this.showEndedDialog = true
        return
      }
      await this.loadMyBattleModels()
      this.showBattleDialog = false
      this.showMyBattleModelsDialog = true
    },
    closeMyBattleModelsDialog () {
      this.showMyBattleModelsDialog = false
    },
    async loadMyBattleModels () {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.myBattleModelsMessage = '加载失败：当前未登录或登录已过期，请重新登录'
        this.myBattleModels = []
        return
      }

      this.myBattleModelsLoading = true
      this.myBattleModelsMessage = ''
      try {
        const assignmentId = this.getAssignmentId()
        const resp = await fetch(`${API_BASE}/battle/models/${assignmentId}/mine`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const res = await resp.json()
        if (!resp.ok || res.code !== 0) {
          throw new Error(res.message || '加载失败')
        }
        this.myBattleModels = Array.isArray(res.data) ? res.data : []
        this.myBattleModelsMessage = this.myBattleModels.length > 0 ? '模型列表加载成功' : ''
      } catch (e) {
        this.myBattleModels = []
        this.myBattleModelsMessage = `加载失败：${e.message}`
      } finally {
        this.myBattleModelsLoading = false
      }
    },
    async openOpponentDialog (item) {
      if (this.taskMode === 'tournament' && !this.teamInfo.isCaptain) {
        ElMessage.warning('仅队长可发起挑战')
        return
      }
      this.selectedMyBattleModel = item
      this.showOpponentDialog = true
      await this.loadOpponentModels(item.submissionId)
    },
    closeOpponentDialog () {
      this.showOpponentDialog = false
      this.opponentModels = []
      this.opponentModelsMessage = ''
      this.challengeLoading = false
    },
    async loadOpponentModels (mySubmissionId) {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.opponentModelsMessage = '加载失败：当前未登录或登录已过期，请重新登录'
        this.opponentModels = []
        return
      }

      this.opponentModelsMessage = ''
      try {
        const assignmentId = this.getAssignmentId()
        const resp = await fetch(`${API_BASE}/battle/models/${assignmentId}/opponents?mySubmissionId=${mySubmissionId}`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const res = await resp.json()
        if (!resp.ok || res.code !== 0) {
          throw new Error(res.message || '加载失败')
        }
        this.opponentModels = Array.isArray(res.data) ? res.data : []
      } catch (e) {
        this.opponentModels = []
        this.opponentModelsMessage = `加载失败：${e.message}`
      }
    },
    async challengeOpponent (item) {
      if (!this.selectedMyBattleModel || !this.selectedMyBattleModel.submissionId) {
        this.opponentModelsMessage = '发起失败：请先选择自己的模型'
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.opponentModelsMessage = '发起失败：当前未登录或登录已过期，请重新登录'
        return
      }

      this.challengeLoading = true
      this.opponentModelsMessage = ''
      try {
        const assignmentId = this.getAssignmentId()
        const form = new FormData()
        form.append('mySubmissionId', this.selectedMyBattleModel.submissionId)
        form.append('opponentSubmissionId', item.submissionId)

        const resp = await fetch(`${API_BASE}/battle/challenge/${assignmentId}`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: form
        })
        const res = await resp.json()
        if (!resp.ok || res.code !== 0) {
          throw new Error(res.message || '发起挑战失败')
        }
        const payload = res.data || {}
        this.opponentModelsMessage = payload.message || '挑战已发起'
        if (payload.evaluationId) {
          this.evaluationId = payload.evaluationId
        }
        ElMessage.success(this.opponentModelsMessage)
        this.showOpponentDialog = false
        this.showMyBattleModelsDialog = false
      } catch (e) {
        this.opponentModelsMessage = `发起失败：${e.message}`
      } finally {
        this.challengeLoading = false
      }
    },
    async loadTasks () {
      this.queryLoading = true
      this.queryMessage = ''
      try {
        const resp = await fetch(`${API_BASE}/battle/tasks`)
        const res = await resp.json()

        if (res.code !== 0) {
          throw new Error(res.message || '查询失败')
        }

        this.taskList = res.data || []
        this.queryMessage = '任务列表加载成功'
      } catch (e) {
        this.taskList = []
        this.queryMessage = `查询失败：${e.message}`
      } finally {
        this.queryLoading = false
      }
    },
    async createTeam () {
      const teamName = this.newTeamName.trim()
      if (!teamName) {
        ElMessage.warning('请输入队伍名称')
        return
      }

      const assignmentId = this.getAssignmentId()
      const token = localStorage.getItem('auth_token')
      if (!assignmentId || !token) {
        ElMessage.error('当前未登录或任务信息缺失')
        return
      }

      this.teamActionLoading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/teams`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ teamName })
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '创建队伍失败')
        }
        this.applyTeamInfo(result.data)
        this.closeCreateTeamDialog()
        ElMessage.success(`队伍创建成功，队伍码：${this.teamInfo.teamCode}`)
      } catch (error) {
        ElMessage.error(error.message || '创建队伍失败')
      } finally {
        this.teamActionLoading = false
      }
    },
    async joinTeam () {
      const teamCode = this.joinTeamCode.trim().toUpperCase()
      if (!teamCode) {
        ElMessage.warning('请输入队伍码')
        return
      }

      const assignmentId = this.getAssignmentId()
      const token = localStorage.getItem('auth_token')
      if (!assignmentId || !token) {
        ElMessage.error('当前未登录或任务信息缺失')
        return
      }

      this.teamActionLoading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/teams/join`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ teamCode })
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '加入队伍失败')
        }
        this.applyTeamInfo(result.data)
        this.closeJoinTeamDialog()
        ElMessage.success('加入队伍成功')
      } catch (error) {
        ElMessage.error(error.message || '加入队伍失败')
      } finally {
        this.teamActionLoading = false
      }
    },
    async leaveTeam () {
      const assignmentId = this.getAssignmentId()
      const token = localStorage.getItem('auth_token')
      if (!assignmentId || !token) {
        ElMessage.error('当前未登录或任务信息缺失')
        return
      }

      this.teamActionLoading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/teams/leave`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '退出队伍失败')
        }
        this.resetTeamInfo()
        this.showLeaveTeamDialog = false
        ElMessage.success('退出队伍成功')
      } catch (error) {
        ElMessage.error(error.message || '退出队伍失败')
      } finally {
        this.teamActionLoading = false
      }
    },
    async dissolveTeam () {
      const assignmentId = this.getAssignmentId()
      const token = localStorage.getItem('auth_token')
      if (!assignmentId || !token) {
        ElMessage.error('当前未登录或任务信息缺失')
        return
      }

      this.teamActionLoading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/teams/dissolve`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '解散队伍失败')
        }
        this.resetTeamInfo()
        this.showDissolveTeamDialog = false
        ElMessage.success('队伍已解散')
      } catch (error) {
        ElMessage.error(error.message || '解散队伍失败')
      } finally {
        this.teamActionLoading = false
      }
    },
    async loadMyTeam (assignmentId) {
      const token = localStorage.getItem('auth_token')
      if (!assignmentId || !token) {
        this.resetTeamInfo()
        return
      }

      this.teamLoading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/teams/my`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '队伍信息加载失败')
        }
        this.applyTeamInfo(result.data)
      } catch (error) {
        this.resetTeamInfo()
      } finally {
        this.teamLoading = false
      }
    },
    applyTeamInfo (data) {
      if (!data) {
        this.resetTeamInfo()
        return
      }
      const memberNames = Array.isArray(data.memberNames) ? data.memberNames : []
      this.teamInfo = {
        teamId: data.teamId || null,
        name: data.teamName || '',
        teamCode: data.teamCode || '',
        captain: data.captainName || '',
        members: memberNames,
        memberCount: data.memberCount || memberNames.length,
        maxMembers: data.maxMembers || 3,
        isCaptain: !!data.captain
      }
      this.hasTeam = true
    },
    resetTeamInfo () {
      this.hasTeam = false
      this.teamInfo = {
        teamId: null,
        name: '',
        teamCode: '',
        captain: '',
        members: [],
        memberCount: 0,
        maxMembers: 3,
        isCaptain: false
      }
    },
    displayTeamSlot (index) {
      return this.teamInfo.members[index] || '等待加入'
    },
    closeCreateTeamDialog () {
      this.showCreateTeamDialog = false
      this.newTeamName = ''
    },
    closeJoinTeamDialog () {
      this.showJoinTeamDialog = false
      this.joinTeamCode = ''
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
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
      sessionStorage.removeItem('mock_logged_out_view')
      this.$router.replace('/')
    }
  }
}

function titleSafe (value, fallback) {
  return value || fallback
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
  white-space: pre-wrap;
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

.algorithm-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.algorithm-tag {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  border-radius: 16px;
  background: #ecf5ff;
  color: #1f4e8c;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid #bfd7f4;
}

.empty-algorithm-text {
  font-size: 14px;
  color: #909399;
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

.ended-submit-btn {
  background: #c0c4cc;
  color: #ffffff;
}

.ended-submit-btn:hover {
  background: #c0c4cc;
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

.team-member-stack {
  margin-top: 6px;
}

.team-member-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px dashed #ebeef5;
  font-size: 14px;
  color: #606266;
}

.team-member-row:first-child {
  border-top: 1px dashed #ebeef5;
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
  word-break: break-word;
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
  max-height: calc(100vh - 40px);
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.small-box {
  width: 420px;
}

.large-box {
  width: 900px;
}

.ranking-dialog {
  width: 720px;
}

.dialog-header {
  min-height: 56px;
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
  overflow-y: auto;
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

.form-item input,
.form-item select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  background: #ffffff;
  color: #303133;
}

.hidden-file-input {
  display: none;
}

.file-picker-row {
  display: grid;
  grid-template-columns: 110px 1fr 88px;
  gap: 10px;
  align-items: center;
}

.file-select-btn {
  height: 40px;
  border: 1px solid #1f4e8c;
  border-radius: 4px;
  background: #ecf5ff;
  color: #1f4e8c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.file-select-btn:hover {
  background: #dcecff;
}

.file-name-box {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 14px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-remove-btn {
  height: 40px;
  border: 1px solid #e4b9b9;
  border-radius: 4px;
  background: #fff6f6;
  color: #c45656;
  font-size: 14px;
  cursor: pointer;
}

.file-remove-btn:hover:not(:disabled) {
  background: #fdeeee;
}

.file-remove-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.dialog-tip {
  font-size: 13px;
  color: #909399;
}

.submit-mode-tip {
  margin-bottom: 16px;
  color: #1f4e8c;
  font-size: 14px;
}

.action-btn-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-box {
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.7;
}

.success-box {
  background: #f0f9eb;
  border: 1px solid #d9ecff;
  color: #1f4e8c;
}

.error-box {
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  color: #c45656;
}

.task-dialog-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.secondary-inline-btn {
  min-width: 108px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  cursor: pointer;
}

.secondary-inline-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.secondary-inline-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.task-table-wrap {
  overflow-x: auto;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.task-table th,
.task-table td {
  border: 1px solid #ebeef5;
  padding: 12px 10px;
  text-align: center;
  font-size: 14px;
  color: #303133;
}

.task-table th {
  background: #f8fafc;
  color: #1f2d3d;
  font-weight: 700;
}

.empty-state {
  padding: 28px 12px;
  text-align: center;
  color: #909399;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
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

  .large-box {
    width: 100%;
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

  .task-header,
  .task-dialog-top,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .task-title {
    font-size: 24px;
  }

  .file-picker-row {
    grid-template-columns: 1fr;
  }
}

.battle-model-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.battle-model-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafbfd;
}

.battle-model-main {
  flex: 1;
  min-width: 0;
}

.battle-model-name {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
  word-break: break-all;
}

.battle-model-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.battle-model-actions {
  flex-shrink: 0;
}

.small-action-btn {
  min-width: 92px;
  height: 34px;
  padding: 0 14px;
}

.opponent-selected-tip {
  margin-bottom: 14px;
}

</style>
