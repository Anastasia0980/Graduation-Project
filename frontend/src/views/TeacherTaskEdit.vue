<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      user-name='王老师'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTeacherHome'
      @user-click='goTeacherHome'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar
        active-menu='task-manage'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <div class='header-left'>
            <button class='back-btn' @click='goBack'>返回</button>
            <h2>修改任务</h2>
          </div>
        </div>

        <div class='card'>
          <div class='card-title'>任务基础信息</div>

          <div class='form-grid'>
            <div class='form-item full-width'>
              <label>任务名称</label>
              <input v-model='taskForm.name' type='text' placeholder='请输入任务名称' />
            </div>

            <div class='form-item'>
              <label>适用班级</label>
              <select v-model='taskForm.className'>
                <option>人工智能 2201</option>
                <option>人工智能 2202</option>
                <option>软件工程 2201</option>
              </select>
            </div>

            <div class='form-item'>
              <label>测评环境</label>
              <select v-model='taskForm.environment'>
                <option>PettingZoo 井字棋环境</option>
                <option>PettingZoo Connect Four 环境</option>
              </select>
            </div>

            <div class='form-item'>
              <label>开始时间</label>
              <input v-model='taskForm.startTime' type='datetime-local' />
            </div>

            <div class='form-item'>
              <label>截止时间</label>
              <input v-model='taskForm.deadline' type='datetime-local' />
            </div>
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>任务模式配置</div>

          <div class='mode-grid'>
            <div class='form-item'>
              <label>任务类型</label>
              <select v-model='taskMode'>
                <option value='single'>单人模式</option>
                <option value='battle'>对战模式</option>
                <option value='tournament'>团队锦标赛模式</option>
              </select>
            </div>
          </div>

          <div class='mode-tip-box'>
            <div class='mode-tip-title'>当前任务说明</div>
            <div class='mode-tip-text'>
              <template v-if='taskMode === "single"'>
                当前为单人模式。学生提交模型后，平台在预设环境中独立完成测评，并返回奖励、结果和相关记录。
              </template>
              <template v-else-if='taskMode === "battle"'>
                当前为对战模式。学生进入任务后可选择真人对战；若教师上传了任意难度的人机模型，学生端还将开放人机对战入口。
              </template>
              <template v-else>
                当前为团队锦标赛模式。学生需先完成组队，再由队伍统一提交模型，平台将按淘汰赛机制逐轮决出最终胜者。
              </template>
            </div>
          </div>
        </div>

        <div v-if='taskMode === "battle"' class='card section-space'>
          <div class='card-title'>人机模型配置</div>

          <div class='difficulty-box'>
            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>简单模式人机模型</div>
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button' @click='triggerFileInput("easyBotInput")'>上传简单模型</button>
                <span class='upload-text'>{{ easyBotFileName }}</span>
                <input
                  ref='easyBotInput'
                  class='hidden-file-input'
                  type='file'
                  @change='handleBotFileChange($event, "easy")'
                />
              </div>
            </div>

            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>中等模式人机模型</div>
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button' @click='triggerFileInput("mediumBotInput")'>上传中等模型</button>
                <span class='upload-text'>{{ mediumBotFileName }}</span>
                <input
                  ref='mediumBotInput'
                  class='hidden-file-input'
                  type='file'
                  @change='handleBotFileChange($event, "medium")'
                />
              </div>
            </div>

            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>困难模式人机模型</div>
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button' @click='triggerFileInput("hardBotInput")'>上传困难模型</button>
                <span class='upload-text'>{{ hardBotFileName }}</span>
                <input
                  ref='hardBotInput'
                  class='hidden-file-input'
                  type='file'
                  @change='handleBotFileChange($event, "hard")'
                />
              </div>
            </div>
          </div>

          <div class='upload-tip top-gap'>
            说明：只要教师上传了任意一个难度的人机模型，学生端“人机对战”按钮就会由灰色变为可点击。
          </div>
        </div>

        <div v-if='taskMode === "tournament"' class='card section-space'>
          <div class='card-title'>团队锦标赛配置</div>

          <div class='form-grid'>
            <div class='form-item'>
              <label>队伍最少人数</label>
              <select v-model='teamMin'>
                <option value='1'>1 人</option>
                <option value='2'>2 人</option>
                <option value='3'>3 人</option>
              </select>
            </div>

            <div class='form-item'>
              <label>队伍最多人数</label>
              <select v-model='teamMax'>
                <option value='1'>1 人</option>
                <option value='2'>2 人</option>
                <option value='3'>3 人</option>
              </select>
            </div>

            <div class='form-item full-width'>
              <label>模型提交策略</label>
              <select v-model='submitStrategy'>
                <option value='once'>只提交一次模型，系统自动完成全部轮次</option>
                <option value='round'>晋级后允许重新提交模型（当前仅做界面预留）</option>
              </select>
            </div>

            <div class='form-item full-width'>
              <label>淘汰赛说明</label>
              <textarea
                v-model='taskForm.tournamentRule'
                rows='5'
                placeholder='请输入淘汰赛机制说明，例如晋级规则、对局轮次、决赛方式、冠军判定方式等'
              ></textarea>
            </div>
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>任务说明</div>

          <div class='form-grid'>
            <div class='form-item full-width'>
              <label>任务图标</label>

              <div class='upload-box'>
                <button class='upload-btn' type='button' @click='triggerFileInput("taskIconInput")'>选择任务图标</button>
                <span class='upload-text'>{{ taskIconFileName }}</span>
                <input
                  ref='taskIconInput'
                  class='hidden-file-input'
                  type='file'
                  accept='image/*'
                  @change='handleTaskIconChange'
                />
              </div>

              <div class='upload-tip'>
                当前采用选择文件上传形式，暂为前端静态展示。
              </div>
            </div>

            <div class='form-item full-width'>
              <label>简介</label>
              <textarea
                v-model='taskForm.intro'
                rows='5'
                placeholder='请输入任务简介，例如任务背景、教学目标、适用场景等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>规则</label>
              <textarea
                v-model='taskForm.rule'
                rows='6'
                placeholder='请输入任务规则，例如对战规则、胜负判定规则、非法动作限制等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>观测</label>
              <textarea
                v-model='taskForm.observation'
                rows='5'
                placeholder='请输入环境观测说明，例如观测维度、状态表示、输入格式等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>动作空间</label>
              <textarea
                v-model='taskForm.actionSpace'
                rows='4'
                placeholder='请输入动作空间说明，例如离散动作数量、动作编号与具体操作的映射关系等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>奖励</label>
              <textarea
                v-model='taskForm.reward'
                rows='5'
                placeholder='请输入奖励设计说明，例如获胜奖励、平局奖励、非法动作惩罚等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>评测说明</label>
              <textarea
                v-model='taskForm.evaluation'
                rows='6'
                placeholder='请输入评测说明，例如测评环境、时间限制、提交要求、结果记录方式等'
              ></textarea>
            </div>

            <div class='form-item full-width'>
              <label>算法模板文件</label>

              <div class='upload-box'>
                <button class='upload-btn' type='button' @click='triggerFileInput("templateInput")'>选择模板文件</button>
                <span class='upload-text'>{{ templateFileName }}</span>
                <input
                  ref='templateInput'
                  class='hidden-file-input'
                  type='file'
                  @change='handleTemplateFileChange'
                />
              </div>

              <div class='upload-tip'>
                可上传算法模板文件，供学生下载参考。当前按钮仅为界面展示，上传功能暂未接入。
              </div>
            </div>
          </div>
        </div>

        <div class='bottom-action-row'>
          <button class='primary-btn' type='button' @click='saveTask'>保存修改</button>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

const mockTaskMap = {
  1: {
    name: '井字棋对战游戏',
    className: '人工智能 2201',
    environment: 'PettingZoo 井字棋环境',
    startTime: '2026-07-01T08:00',
    deadline: '2026-07-10T23:59',
    mode: 'battle',
    taskIconName: 'tictactoe-battle.png',
    templateFileName: 'battle_template.zip',
    easyBotName: 'easy_model.zip',
    mediumBotName: 'medium_model.zip',
    hardBotName: 'hard_model.zip',
    teamMin: '1',
    teamMax: '3',
    submitStrategy: 'once',
    tournamentRule: '',
    intro: '本任务基于 PettingZoo 提供的井字棋环境开展智能体测评与对战。学生需按照任务要求提交模型文件与必要配置，平台将在统一环境中执行测评流程，并保存结果与相关记录。',
    rule: '井字棋棋盘由 3 × 3 的九个格子组成，双方轮流在空位置落子。一方若先在横向、纵向或对角线上形成连续三个相同标记，则判定获胜。若棋盘全部填满且无人形成三连，则判定为平局。智能体只能在尚未被占用的位置执行动作。若输出非法动作，例如落在已被占用的位置，平台会将其视为违规操作并记录在结果中。',
    observation: '环境观测由当前棋盘状态构成，可视为对 9 个位置的离散描述。每个位置一般包括空位、己方落子、对方落子等信息。智能体需要根据当前观测选择合适的落子位置。',
    actionSpace: '动作空间为离散动作空间，可表示为：A = {0, 1, 2, 3, 4, 5, 6, 7, 8}。',
    reward: '平台测评时可采用简化奖励设计来衡量智能体表现，例如获胜给予正奖励，平局给予较小奖励，非法动作给予惩罚，并在结果中记录违规行为。',
    evaluation: '测评环境：PettingZoo 井字棋环境；任务类型：对战模式；模型要求：需提交可被平台脚本正常加载的模型与配置文件；动作限制：仅允许输出合法离散动作；时间限制：单步决策时间与总执行时间均受平台控制；结果输出：平台保存结果、状态与视频记录。'
  },
  2: {
    name: '井字棋单人测评任务',
    className: '人工智能 2202',
    environment: 'PettingZoo 井字棋环境',
    startTime: '2026-07-02T08:00',
    deadline: '2026-07-15T23:59',
    mode: 'single',
    taskIconName: 'tictactoe-single.png',
    templateFileName: 'single_template.zip',
    easyBotName: '当前未选择文件',
    mediumBotName: '当前未选择文件',
    hardBotName: '当前未选择文件',
    teamMin: '1',
    teamMax: '3',
    submitStrategy: 'once',
    tournamentRule: '',
    intro: '本任务面向强化学习课程中的单人测评场景，学生提交模型后，平台将在统一环境下自动完成运行与评分，用于考察学生对环境建模和模型训练的掌握情况。',
    rule: '学生提交模型后，由平台在统一环境中执行固定轮次测评。若模型无法正常加载、动作非法或运行超时，系统将记录异常并影响最终结果。',
    observation: '观测信息由当前棋盘状态或环境状态组成，学生需要确保模型能够正确解析输入数据，并输出与环境动作空间一致的结果。',
    actionSpace: '动作空间为离散动作集合，学生提交的模型输出必须落在平台允许的动作编号范围内。',
    reward: '测评结果依据环境奖励累计值、成功率、稳定性以及是否存在非法动作等因素综合统计。',
    evaluation: '平台统一完成模型加载、环境执行、结果记录与数据入库，最终保存测评状态、得分结果与相关运行日志。'
  },
  3: {
    name: '井字棋团队锦标赛任务',
    className: '软件工程 2201',
    environment: 'PettingZoo 井字棋环境',
    startTime: '2026-06-01T08:00',
    deadline: '2026-06-18T23:59',
    mode: 'tournament',
    taskIconName: 'tictactoe-tournament.png',
    templateFileName: 'tournament_template.zip',
    easyBotName: '当前未选择文件',
    mediumBotName: '当前未选择文件',
    hardBotName: '当前未选择文件',
    teamMin: '1',
    teamMax: '3',
    submitStrategy: 'once',
    tournamentRule: '学生需先在任务详情页完成组队，再由队伍统一参与锦标赛。平台依据既定对战树逐轮推进比赛，并最终决出冠军队伍。',
    intro: '该任务用于团队锦标赛测评场景，学生需要先创建队伍或加入队伍，再由队伍参与统一赛程。平台会保存每轮对战结果和最终排名。',
    rule: '团队锦标赛采用淘汰赛机制。队伍在规定时间内提交模型后进入赛程。若某队模型无法正常运行或存在非法动作，平台将记录异常并影响晋级结果。',
    observation: '环境观测仍以井字棋状态信息为主，队伍提交的模型必须与平台约定的观测格式和执行脚本保持一致。',
    actionSpace: '动作空间为离散动作集合，对应井字棋棋盘位置编号。模型仅允许输出合法空位动作。',
    reward: '锦标赛中主要记录胜负关系、轮次进展和最终名次，也可结合奖励累计值作为辅助评价指标。',
    evaluation: '平台将保存每轮对战记录、晋级情况、最终名次与相关视频回放，用于课程考核展示与成绩评定。'
  }
}

export default {
  name: 'TeacherTaskEditView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      taskId: '',
      taskMode: 'single',
      teamMin: '1',
      teamMax: '3',
      submitStrategy: 'once',
      taskIconFileName: '当前未选择文件',
      templateFileName: '当前未选择文件',
      easyBotFileName: '当前未选择文件',
      mediumBotFileName: '当前未选择文件',
      hardBotFileName: '当前未选择文件',
      taskForm: {
        name: '',
        className: '人工智能 2201',
        environment: 'PettingZoo 井字棋环境',
        startTime: '',
        deadline: '',
        intro: '',
        rule: '',
        observation: '',
        actionSpace: '',
        reward: '',
        evaluation: '',
        tournamentRule: ''
      }
    }
  },
  created () {
    this.initTaskData()
  },
  methods: {
    initTaskData () {
      this.taskId = this.$route.params.taskId
      const currentTask = mockTaskMap[this.taskId] || mockTaskMap[1]

      this.taskForm = {
        name: currentTask.name,
        className: currentTask.className,
        environment: currentTask.environment,
        startTime: currentTask.startTime,
        deadline: currentTask.deadline,
        intro: currentTask.intro,
        rule: currentTask.rule,
        observation: currentTask.observation,
        actionSpace: currentTask.actionSpace,
        reward: currentTask.reward,
        evaluation: currentTask.evaluation,
        tournamentRule: currentTask.tournamentRule
      }

      this.taskMode = currentTask.mode
      this.teamMin = currentTask.teamMin
      this.teamMax = currentTask.teamMax
      this.submitStrategy = currentTask.submitStrategy
      this.taskIconFileName = currentTask.taskIconName
      this.templateFileName = currentTask.templateFileName
      this.easyBotFileName = currentTask.easyBotName
      this.mediumBotFileName = currentTask.mediumBotName
      this.hardBotFileName = currentTask.hardBotName
    },
    triggerFileInput (refName) {
      const input = this.$refs[refName]
      if (input) {
        input.click()
      }
    },
    handleTaskIconChange (event) {
      const file = event.target.files && event.target.files[0]
      this.taskIconFileName = file ? file.name : this.taskIconFileName
    },
    handleTemplateFileChange (event) {
      const file = event.target.files && event.target.files[0]
      this.templateFileName = file ? file.name : this.templateFileName
    },
    handleBotFileChange (event, level) {
      const file = event.target.files && event.target.files[0]
      if (!file) return

      if (level === 'easy') {
        this.easyBotFileName = file.name
      } else if (level === 'medium') {
        this.mediumBotFileName = file.name
      } else if (level === 'hard') {
        this.hardBotFileName = file.name
      }
    },
    saveTask () {
      alert('保存修改成功')
    },
    goBack () {
      this.$router.push('/teacher/tasks')
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goPublishTask () {
      this.$router.push('/teacher/publish')
    },
    goTaskManage () {
      this.$router.push('/teacher/tasks')
    },
    goClassData () {
      this.$router.push('/teacher/classes')
    },
    goExportScore () {
      this.$router.push('/teacher/export')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.page {
  min-height: 100vh;
  background: #f5f7fa;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  color: #303133;
}

.layout {
  display: flex;
  min-height: calc(100vh - 64px);
}

.content-area {
  flex: 1;
  padding: 20px;
}

.page-header {
  margin-bottom: 18px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.back-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.section-space {
  margin-top: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #1f2d3d;
}

.form-grid,
.mode-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item input,
.form-item select,
.form-item textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  font-size: 14px;
  color: #303133;
  background: #ffffff;
}

.form-item input:focus,
.form-item select:focus,
.form-item textarea:focus {
  border-color: #1f4e8c;
}

.mode-tip-box {
  margin-top: 18px;
  border: 1px solid #dcdfe6;
  background: #f8fafc;
  border-radius: 6px;
  padding: 16px;
}

.mode-tip-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 10px;
}

.mode-tip-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.difficulty-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.difficulty-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  background: #fafbfc;
}

.difficulty-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.difficulty-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
}

.difficulty-status {
  font-size: 13px;
  color: #909399;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.upload-btn:hover {
  border-color: #1f4e8c;
  color: #1f4e8c;
}

.upload-text {
  font-size: 14px;
  color: #606266;
}

.hidden-file-input {
  display: none;
}

.upload-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.top-gap {
  margin-top: 12px;
}

.bottom-action-row {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-btn {
  min-width: 96px;
  height: 38px;
  padding: 0 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  border: none;
  background: #1f4e8c;
  color: #ffffff;
}

.primary-btn:hover {
  background: #173b69;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .form-grid,
  .mode-grid {
    grid-template-columns: 1fr;
  }
}
</style>
