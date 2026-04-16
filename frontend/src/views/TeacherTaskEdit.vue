<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      :user-name='teacherName'
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
        @task-hall-click='goTaskHall'
        @history-click='goHistory'
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

        <div v-if='loading' class='card loading-card'>
          任务加载中...
        </div>

        <template v-else>
          <div class='card'>
            <div class='card-title'>任务基础信息</div>

            <div class='form-grid'>
              <div class='form-item full-width'>
                <label>任务名称</label>
                <input v-model='taskForm.name' type='text' placeholder='请输入任务名称' />
              </div>

              <div class='form-item'>
                <label>适用班级</label>
                <select v-model='taskForm.classId' disabled>
                  <option value='' disabled>请选择班级</option>
                  <option
                    v-for='item in classOptions'
                    :key='item.id'
                    :value='item.id'
                  >
                    {{ item.name }}
                  </option>
                </select>
                <div class='field-tip'>当前后端修改接口暂不支持变更任务所属班级，此处仅做展示。</div>
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
                  <option value='tournament'>分组对战模式</option>
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
                  当前为分组对战模式。学生需先完成组队，再由队伍统一提交模型，平台将按异步自主挑战机制累计队伍战绩并生成分组排行榜。
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
                  <div class='difficulty-status'>{{ easyBotStatusText }}</div>
                </div>

                <div class='upload-pair-box'>
                  <div class='upload-box'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("easyConfigInput")'>上传Config</button>
                    <span class='upload-text'>{{ easyBotConfigFileName }}</span>
                    <input
                      ref='easyConfigInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.json'
                      @change='handleBotFileChange($event, "easy", "config")'
                    />
                  </div>

                  <div class='upload-box top-gap-mini'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("easyModelInput")'>上传Model</button>
                    <span class='upload-text'>{{ easyBotModelFileName }}</span>
                    <input
                      ref='easyModelInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.pt,.pth'
                      @change='handleBotFileChange($event, "easy", "model")'
                    />
                  </div>
                </div>
              </div>

              <div class='difficulty-item'>
                <div class='difficulty-header'>
                  <div class='difficulty-title'>中等模式人机模型</div>
                  <div class='difficulty-status'>{{ mediumBotStatusText }}</div>
                </div>

                <div class='upload-pair-box'>
                  <div class='upload-box'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("mediumConfigInput")'>上传Config</button>
                    <span class='upload-text'>{{ mediumBotConfigFileName }}</span>
                    <input
                      ref='mediumConfigInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.json'
                      @change='handleBotFileChange($event, "medium", "config")'
                    />
                  </div>

                  <div class='upload-box top-gap-mini'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("mediumModelInput")'>上传Model</button>
                    <span class='upload-text'>{{ mediumBotModelFileName }}</span>
                    <input
                      ref='mediumModelInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.pt,.pth'
                      @change='handleBotFileChange($event, "medium", "model")'
                    />
                  </div>
                </div>
              </div>

              <div class='difficulty-item'>
                <div class='difficulty-header'>
                  <div class='difficulty-title'>困难模式人机模型</div>
                  <div class='difficulty-status'>{{ hardBotStatusText }}</div>
                </div>

                <div class='upload-pair-box'>
                  <div class='upload-box'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("hardConfigInput")'>上传Config</button>
                    <span class='upload-text'>{{ hardBotConfigFileName }}</span>
                    <input
                      ref='hardConfigInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.json'
                      @change='handleBotFileChange($event, "hard", "config")'
                    />
                  </div>

                  <div class='upload-box top-gap-mini'>
                    <button class='upload-btn' type='button' @click='triggerFileInput("hardModelInput")'>上传Model</button>
                    <span class='upload-text'>{{ hardBotModelFileName }}</span>
                    <input
                      ref='hardModelInput'
                      class='hidden-file-input'
                      type='file'
                      accept='.pt,.pth'
                      @change='handleBotFileChange($event, "hard", "model")'
                    />
                  </div>
                </div>
              </div>
            </div>

            <div class='upload-tip top-gap'>
              说明：只要教师完整上传任意一个难度的人机模型（config 和 model），学生端“人机对战”按钮就会由灰色变为可点击。
            </div>
          </div>

          <div v-if='taskMode === "tournament"' class='card section-space'>
            <div class='card-title'>分组对战配置</div>

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

              <div class='form-item'>
                <label>自由组队截止时间</label>
                <input v-model='taskForm.teamGroupDeadline' type='datetime-local' />
              </div>
            </div>
          </div>

          <div v-if='taskMode === "single"' class='card section-space'>
            <div class='card-title'>闯关 Baseline 配置（T1~T10 每关唯一）</div>

            <div class='form-grid'>
              <div
                v-for='taskId in taskIds'
                :key='taskId'
                class='form-item full-width'
              >
                <label>{{ taskId }}</label>
                <div class='baseline-diff-block'>
                  <div class='baseline-diff-subtitle'>已选</div>
                  <div v-if='!baselineSelectedOptionByTask[taskId]' class='baseline-empty'>未选择</div>
                  <div v-else class='baseline-selected-list'>
                    <div class='baseline-item'>
                      <span>{{ baselineSelectedOptionByTask[taskId].label || baselineSelectedOptionByTask[taskId].algorithm || baselineSelectedOptionByTask[taskId].id }}</span>
                      <button type='button' class='baseline-x-btn' @click='removeBaselineFromSelection(taskId)'>X</button>
                    </div>
                  </div>

                  <div class='baseline-diff-subtitle'>可选</div>
                  <div v-if='baselineAvailableOptionsByTask[taskId].length === 0' class='baseline-empty'>暂无可选</div>
                  <div v-else class='baseline-available-list'>
                    <div
                      v-for='opt in baselineAvailableOptionsByTask[taskId]'
                      :key='opt.id'
                      class='baseline-item baseline-item-available'
                    >
                      <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                      <div class='baseline-action-buttons'>
                        <button type='button' class='baseline-add-btn' @click='addBaselineToSelection(taskId, opt.id)'>设为当前关</button>
                        <button type='button' class='baseline-x-btn' @click='softDeleteBaselineFromCatalog(taskId, opt)'>X</button>
                      </div>
                    </div>
                  </div>

                  <div class='baseline-upload-box'>
                    <div class='baseline-row'>
                      <span class='baseline-upload-label'>algorithm：</span>
                      <input v-model='baselineUpload[taskId].algorithm' class='baseline-algo-input' placeholder='例如 DQN / DDQN' />
                    </div>
                    <div class='baseline-row'>
                      <input
                        type='file'
                        accept='.pth'
                        :ref='`baselineFileInput_${taskId}`'
                        class='baseline-file-input'
                        @change='handleBaselineFileChange($event, taskId)'
                      />
                      <span class='baseline-file-name'>{{ baselineUpload[taskId].fileName }}</span>
                      <button type='button' class='baseline-upload-btn' @click='uploadBaselineModel(taskId)'>
                        上传并设为本关
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class='card section-space'>
            <div class='card-title'>任务环境及模型</div>

            <div class='form-grid'>
              <div class='form-item full-width'>
                <label>模型环境</label>
                <select v-model='taskForm.environmentCode'>
                  <option value='tictactoe_v3'>tictactoe_v3</option>
                  <option value='connect_four_v3'>connect_four_v3</option>
                  <option value='LunarLander-v3'>LunarLander-v3</option>
                </select>
              </div>

              <div class='form-item full-width'>
                <label>可用算法</label>
                <div class='algorithm-btn-group'>
                  <button
                    v-for='item in algorithmOptions'
                    :key='item'
                    type='button'
                    class='algorithm-btn'
                    :class='{ "algorithm-btn-active": selectedAlgorithms.includes(item) }'
                    @click='toggleAlgorithm(item)'
                  >
                    {{ item }}
                  </button>

                  <button
                    type='button'
                    class='algorithm-add-btn'
                    @click='openAlgorithmDialog'
                  >
                    + 添加算法
                  </button>
                </div>
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
            </div>
          </div>

          <div class='bottom-action-row'>
            <button class='secondary-btn' type='button' @click='goBack'>取消</button>
            <button class='primary-btn' type='button' :disabled='saving' @click='saveTask'>
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </template>
      </main>
    </div>
  </div>
    <div v-if='algorithmDialogVisible' class='dialog-mask' @click='closeAlgorithmDialog'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>添加算法标签</div>
        </div>

        <div class='dialog-body'>
          <div class='dialog-label'>请输入算法名</div>
          <input
            v-model='customAlgorithmName'
            class='dialog-input'
            type='text'
            maxlength='30'
            placeholder='请输入算法名'
            @keyup.enter='confirmAddAlgorithm'
          />
        </div>

        <div class='dialog-footer'>
          <button type='button' class='secondary-btn dialog-btn' @click='closeAlgorithmDialog'>取消</button>
          <button type='button' class='primary-btn dialog-btn' @click='confirmAddAlgorithm'>确定</button>
        </div>
      </div>
    </div>

</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

const API_BASE = 'http://localhost:8080'
const TASK_IDS = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10']

export default {
  name: 'TeacherTaskEditView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      taskId: '',
      loading: false,
      saving: false,
      taskMode: 'single',
      teamMin: '1',
      teamMax: '3',
      taskIconFileName: '当前未选择文件',

      easyBotConfigFileName: '当前未选择文件',
      easyBotModelFileName: '当前未选择文件',
      mediumBotConfigFileName: '当前未选择文件',
      mediumBotModelFileName: '当前未选择文件',
      hardBotConfigFileName: '当前未选择文件',
      hardBotModelFileName: '当前未选择文件',

      easyBotConfigured: false,
      mediumBotConfigured: false,
      hardBotConfigured: false,

      botFiles: {
        easy: { config: null, model: null },
        medium: { config: null, model: null },
        hard: { config: null, model: null }
      },

      classOptions: [],
      classMap: {},
      algorithmOptions: ['DDPG', 'DQN', 'QLearning'],
      selectedAlgorithms: [],
      algorithmDialogVisible: false,
      customAlgorithmName: '',
      taskIds: TASK_IDS,
      // baseline catalog（来自后端 /assignments/{id}/baseline-catalog，key: T1..T10）
      baselineCatalog: TASK_IDS.reduce((acc, taskId) => {
        acc[taskId] = []
        return acc
      }, {}),
      // baseline 选中项（每关唯一，存 baselineOption.id）
      baselineSelectedIds: TASK_IDS.reduce((acc, taskId) => {
        acc[taskId] = null
        return acc
      }, {}),
      // 上传 baseline.pth 的输入（一次上传一个 taskId+algorithm）
      baselineUpload: TASK_IDS.reduce((acc, taskId) => {
        acc[taskId] = { algorithm: '', file: null, fileName: '当前未选择文件' }
        return acc
      }, {}),
      taskForm: {
        name: '',
        classId: '',
        environmentCode: 'tictactoe_v3',
        deadline: '',
        intro: '',
        rule: '',
        observation: '',
        actionSpace: '',
        reward: '',
        evaluation: '',
        teamGroupDeadline: ''
      }
    }
  },
  computed: {
    easyBotStatusText () {
      return this.easyBotConfigured ? '已配置' : '未配置'
    },
    mediumBotStatusText () {
      return this.mediumBotConfigured ? '已配置' : '未配置'
    },
    hardBotStatusText () {
      return this.hardBotConfigured ? '已配置' : '未配置'
    },
    baselineSelectedOptionByTask () {
      const mapped = {}
      this.taskIds.forEach(taskId => {
        const selectedId = this.baselineSelectedIds?.[taskId]
        mapped[taskId] = selectedId ? this.getBaselineOptionById(taskId, selectedId) : null
      })
      return mapped
    },
    baselineAvailableOptionsByTask () {
      const mapped = {}
      this.taskIds.forEach(taskId => {
        const catalogList = this.baselineCatalog?.[taskId] || []
        const selectedId = this.baselineSelectedIds?.[taskId]
        mapped[taskId] = catalogList.filter(opt => opt && opt.id !== selectedId)
      })
      return mapped
    }
  },
  created () {
    this.taskId = this.$route.params.taskId
    this.initTaskData()
  },
  methods: {
    async initTaskData () {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        this.goBack()
        return
      }

      this.loading = true
      try {
        await this.loadClassOptions(token)

        const response = await fetch(`${API_BASE}/teacher/assignments?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '任务加载失败')
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        const currentTask = content.find(item => String(item.id) === String(this.taskId))

        if (!currentTask) {
          throw new Error('未找到当前任务')
        }

        this.fillTaskForm(currentTask)

        // baseline 只在 single 模式展示，但这里提前加载避免用户切换时出现空白
        await this.loadBaselineCatalog(token)

        if (this.taskMode === 'battle') {
          await this.loadBotStatus()
        }
      } catch (error) {
        ElMessage.error(error.message || '任务加载失败')
        this.goBack()
      } finally {
        this.loading = false
      }
    },
    async loadClassOptions (token) {
      try {
        const response = await fetch(`${API_BASE}/class?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          this.classOptions = []
          this.classMap = {}
          return
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.classOptions = content.map(item => ({
          id: item.id,
          name: item.name
        }))

        const map = {}
        content.forEach(item => {
          map[item.id] = item.name
        })
        this.classMap = map
      } catch (error) {
        this.classOptions = []
        this.classMap = {}
      }
    },
    async loadBaselineCatalog (token) {
      try {
        const response = await fetch(`${API_BASE}/assignments/${this.taskId}/baseline-catalog`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.warning(result.message || 'Baseline catalog 加载失败')
          return
        }

        const catalog = result.data || {}
        this.taskIds.forEach(taskId => {
          this.baselineCatalog[taskId] = Array.isArray(catalog[taskId]) ? catalog[taskId] : []
        })
      } catch (error) {
        ElMessage.warning(error.message || 'Baseline catalog 加载失败')
      }
    },
    async loadBotStatus () {
      const token = localStorage.auth_token
        ? `Bearer ${localStorage.auth_token}`
        : ''

      try {
        const response = await fetch(`${API_BASE}/assignments/${this.taskId}/system-bots/status`, {
          method: 'GET',
          headers: {
            Authorization: token
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0 || !result.data) {
          this.easyBotConfigured = false
          this.mediumBotConfigured = false
          this.hardBotConfigured = false
          return
        }

        this.easyBotConfigured = !!result.data.easy
        this.mediumBotConfigured = !!result.data.medium
        this.hardBotConfigured = !!result.data.hard

        if (this.easyBotConfigured) {
          this.easyBotConfigFileName = '已配置 config.json'
          this.easyBotModelFileName = '已配置 model.pt'
        }
        if (this.mediumBotConfigured) {
          this.mediumBotConfigFileName = '已配置 config.json'
          this.mediumBotModelFileName = '已配置 model.pt'
        }
        if (this.hardBotConfigured) {
          this.hardBotConfigFileName = '已配置 config.json'
          this.hardBotModelFileName = '已配置 model.pt'
        }
      } catch (error) {
        this.easyBotConfigured = false
        this.mediumBotConfigured = false
        this.hardBotConfigured = false
      }
    },
    fillTaskForm (task) {
      const config = this.parseTaskConfig(task)
      const classId = task.studentClass && task.studentClass.id ? task.studentClass.id : ''

      this.taskMode = this.parseTaskMode(task.evaluationMode)
      this.selectedAlgorithms = this.parseAgentNames(task.agentName)
      this.algorithmOptions = this.mergeAlgorithmOptions(config, this.selectedAlgorithms)
      this.taskForm = {
        name: task.title || '',
        classId,
        environmentCode: task.environment || 'tictactoe_v3',
        deadline: this.formatForDatetimeLocal(task.deadline),
        intro: config.overview || '',
        rule: config.rules || '',
        observation: config.observationSpace || '',
        actionSpace: config.actionSpace || '',
        reward: config.rewardFunction || '',
        evaluation: config.evaluationFunction || '',
        teamGroupDeadline: this.formatForDatetimeLocal(task.teamGroupDeadline)
      }

      const taskBaselineOptions = config && config.taskBaselineOptions ? config.taskBaselineOptions : null
      this.taskIds.forEach(taskId => {
        const item = taskBaselineOptions ? taskBaselineOptions[taskId] : null
        this.baselineSelectedIds[taskId] = item && item.id ? item.id : null
      })
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
    parseTaskMode (evaluationMode) {
      const mode = (evaluationMode || '').toUpperCase()
      if (mode === 'VERSUS' || mode === 'BATTLE') {
        return 'battle'
      }
      if (mode === 'TEAM' || mode === 'TOURNAMENT') {
        return 'tournament'
      }
      return 'single'
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
    mergeAlgorithmOptions (config, selectedAlgorithms) {
      const baseOptions = ['DDPG', 'DQN', 'QLearning']
      const configOptions = Array.isArray(config.algorithmOptions) ? config.algorithmOptions : []
      const merged = [...baseOptions]

      configOptions.forEach(item => {
        if (item && !merged.includes(item)) {
          merged.push(item)
        }
      })

      selectedAlgorithms.forEach(item => {
        if (item && !merged.includes(item)) {
          merged.push(item)
        }
      })

      return merged
    },
    formatForDatetimeLocal (value) {
      if (!value) {
        return ''
      }

      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return ''
      }

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')

      return `${year}-${month}-${day}T${hour}:${minute}`
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
    handleBotFileChange (event, level, fileType) {
      const file = event.target.files && event.target.files[0]
      if (!file) return

      this.botFiles[level][fileType] = file

      if (level === 'easy' && fileType === 'config') {
        this.easyBotConfigFileName = file.name
      } else if (level === 'easy' && fileType === 'model') {
        this.easyBotModelFileName = file.name
      } else if (level === 'medium' && fileType === 'config') {
        this.mediumBotConfigFileName = file.name
      } else if (level === 'medium' && fileType === 'model') {
        this.mediumBotModelFileName = file.name
      } else if (level === 'hard' && fileType === 'config') {
        this.hardBotConfigFileName = file.name
      } else if (level === 'hard' && fileType === 'model') {
        this.hardBotModelFileName = file.name
      }
    },
    openAlgorithmDialog () {
      this.customAlgorithmName = ''
      this.algorithmDialogVisible = true
    },
    closeAlgorithmDialog () {
      this.algorithmDialogVisible = false
      this.customAlgorithmName = ''
    },
    confirmAddAlgorithm () {
      const name = this.customAlgorithmName ? this.customAlgorithmName.trim() : ''
      if (!name) {
        ElMessage.warning('请输入算法名')
        return
      }

      const exists = this.algorithmOptions.some(item => item.toLowerCase() === name.toLowerCase())
      if (!exists) {
        this.algorithmOptions.push(name)
      }

      const actualName = this.algorithmOptions.find(item => item.toLowerCase() === name.toLowerCase()) || name
      if (!this.selectedAlgorithms.includes(actualName)) {
        this.selectedAlgorithms.push(actualName)
      }

      this.closeAlgorithmDialog()
    },
    toggleAlgorithm (name) {
      const index = this.selectedAlgorithms.indexOf(name)
      if (index > -1) {
        this.selectedAlgorithms.splice(index, 1)
      } else {
        this.selectedAlgorithms.push(name)
      }
    },
    addBaselineToSelection (taskId, baselineId) {
      this.baselineSelectedIds[taskId] = baselineId
    },
    removeBaselineFromSelection (taskId) {
      this.baselineSelectedIds[taskId] = null
    },
    getBaselineOptionById (taskId, baselineId) {
      const catalogList = this.baselineCatalog?.[taskId] || []
      const fromCatalog = catalogList.find(opt => opt && opt.id === baselineId)
      if (fromCatalog) return fromCatalog

      // 兜底：当 catalog 暂未加载或找不到该 id 时，通过 baselineId 推断展示 label
      const parts = String(baselineId || '').split('-')
      const algoKey = parts[1] || ''
      return {
        id: baselineId,
        label: algoKey ? String(algoKey).toUpperCase() : baselineId,
        algorithm: algoKey,
        modelPath: `${this.taskForm.environmentCode || 'LunarLander-v3'}/${taskId}/${algoKey}/baseline.pth`
      }
    },
    handleBaselineFileChange (event, taskId) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      this.baselineUpload[taskId].file = file
      this.baselineUpload[taskId].fileName = file.name
    },
    resetBaselineUpload (taskId) {
      const baselineState = this.baselineUpload[taskId]
      if (!baselineState) return

      baselineState.algorithm = ''
      baselineState.file = null
      baselineState.fileName = '当前未选择文件'

      const fileInputRef = this.$refs[`baselineFileInput_${taskId}`]
      const fileInput = Array.isArray(fileInputRef) ? fileInputRef[0] : fileInputRef
      if (fileInput) {
        fileInput.value = ''
      }
    },
    async uploadBaselineModel (taskId) {
      const upload = this.baselineUpload[taskId] || {}
      const algorithm = String(upload.algorithm || '').trim()
      const file = upload.file

      if (!algorithm) {
        ElMessage.warning(`请输入 ${taskId} 算法名`)
        return
      }
      if (!file) {
        ElMessage.warning(`请选择 ${taskId} 的 baseline.pth 文件`)
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      try {
        const formData = new FormData()
        formData.append('taskId', taskId)
        formData.append('algorithm', algorithm)
        formData.append('model', file)

        const response = await fetch(`${API_BASE}/assignments/${this.taskId}/baseline-upload`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: formData
        })

        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || 'baseline 上传失败')
        }

        ElMessage.success('baseline 上传成功')

        // 上传成功后刷新 catalog，并把刚上传的项加入已选列表
        await this.loadBaselineCatalog(token)

        const baselineId = `${taskId}-${algorithm.toLowerCase()}`
        this.addBaselineToSelection(taskId, baselineId)

        this.resetBaselineUpload(taskId)
      } catch (error) {
        ElMessage.error(error.message || 'baseline 上传失败，请检查后端是否已启动')
      }
    },
    async softDeleteBaselineFromCatalog (taskId, opt) {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      const baselineId = opt && opt.id ? opt.id : ''
      const algorithm = opt && (opt.algorithm || opt.label)
        ? String(opt.algorithm || opt.label)
        : (baselineId ? String(baselineId).split('-')[1] : '')

      if (!algorithm) {
        ElMessage.warning('baseline 标识缺失，无法删除')
        return
      }

      try {
        await ElMessageBox.confirm(
          `确认删除 ${taskId} 的该 baseline 资源吗？\n删除后将从可选列表中消失。`,
          '提示',
          {
            type: 'warning',
            confirmButtonText: '确认删除',
            cancelButtonText: '取消'
          }
        )
      } catch (e) {
        // 用户取消确认弹窗
        return
      }

      const params = new URLSearchParams({
        environment: this.taskForm.environmentCode || '',
        taskId,
        algorithm
      })

      try {
        const response = await fetch(`${API_BASE}/baselines/soft-delete?${params.toString()}`, {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })

        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || 'baseline 删除失败')
        }

        // 删除后刷新 catalog（删除=软删除，所以删除项应消失在可选列表）
        ElMessage.success('baseline 已删除')
        if (baselineId) {
          this.removeBaselineFromSelection(taskId)
        }
        await this.loadBaselineCatalog(token)
      } catch (error) {
        ElMessage.error(error.message || 'baseline 删除失败，请检查后端是否已启动')
      }
    },
    getEvaluationModeValue () {
      if (this.taskMode === 'battle') {
        return 'VERSUS'
      }
      if (this.taskMode === 'tournament') {
        return 'TEAM'
      }
      return 'SINGLE'
    },
    buildConfigPayload () {
      const rulesText = this.taskMode === 'tournament' && this.taskForm.tournamentRule
        ? `${this.taskForm.rule}\n\n淘汰赛说明：\n${this.taskForm.tournamentRule}`
        : this.taskForm.rule

      const taskBaselineOptions = {}
      this.taskIds.forEach(taskId => {
        const selectedId = this.baselineSelectedIds[taskId]
        if (!selectedId) return
        const catalogList = this.baselineCatalog[taskId] || []
        const fromCatalog = catalogList.find(opt => opt && opt.id === selectedId)
        if (fromCatalog) {
          taskBaselineOptions[taskId] = fromCatalog
          return
        }
        const algoKey = String(selectedId || '').split('-')[1] || ''
        taskBaselineOptions[taskId] = {
          id: selectedId,
          label: algoKey ? String(algoKey).toUpperCase() : selectedId,
          algorithm: algoKey,
          modelPath: `${this.taskForm.environmentCode || 'LunarLander-v3'}/${taskId}/${algoKey}/baseline.pth`
        }
      })

      return {
        overview: this.taskForm.intro,
        rules: this.taskForm.rule,
        observationSpace: this.taskForm.observation,
        actionSpace: this.taskForm.actionSpace,
        rewardFunction: this.taskForm.reward,
        evaluationFunction: this.taskForm.evaluation,
        algorithmOptions: this.algorithmOptions,
        teamMaxMembers: Number(this.teamMax || 3),
        taskBaselineOptions
      }
    },
    validateTaskForm () {
      if (!this.taskForm.name.trim()) {
        ElMessage.warning('请输入任务名称')
        return false
      }
      if (!this.taskForm.deadline) {
        ElMessage.warning('请选择截止时间')
        return false
      }
      if (this.taskMode === 'tournament') {
        if (!this.taskForm.teamGroupDeadline) {
          ElMessage.warning('请选择自由组队截止时间')
          return false
        }

        const now = new Date()
        const teamGroupDeadline = new Date(this.taskForm.teamGroupDeadline)
        const taskDeadline = new Date(this.taskForm.deadline)

        if (Number.isNaN(teamGroupDeadline.getTime()) || Number.isNaN(taskDeadline.getTime())) {
          ElMessage.warning('组队截止时间或任务截止时间格式不正确')
          return false
        }
        if (teamGroupDeadline <= now) {
          ElMessage.warning('自由组队截止时间必须晚于当前时间')
          return false
        }
        if (teamGroupDeadline >= taskDeadline) {
          ElMessage.warning('自由组队截止时间必须早于任务截止时间')
          return false
        }
      }
      if (this.selectedAlgorithms.length === 0) {
        ElMessage.warning('请至少选择一个可用算法')
        return false
      }
      if (this.taskMode === 'single') {
        for (const taskId of this.taskIds) {
          if (!this.baselineSelectedIds[taskId]) {
            ElMessage.warning(`请为 ${taskId} 选择一个 baseline`)
            return false
          }
        }
      }
      return true
    },
    async uploadBotPair (level, token) {
      const pair = this.botFiles[level]
      if (!pair.config || !pair.model) {
        return
      }

      const formData = new FormData()
      formData.append('config', pair.config)
      formData.append('model', pair.model)

      const response = await fetch(`${API_BASE}/assignments/${this.taskId}/system-bots/${level}`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`
        },
        body: formData
      })

      const result = await response.json()
      if (!response.ok || result.code !== 0) {
        throw new Error(result.message || `${level} 人机模型上传失败`)
      }
    },
    async uploadSelectedBotFiles (token) {
      await this.uploadBotPair('easy', token)
      await this.uploadBotPair('medium', token)
      await this.uploadBotPair('hard', token)
    },
    async saveTask () {
      if (!this.validateTaskForm()) {
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      const payload = {
        title: this.taskForm.name.trim(),
        evaluationMode: this.getEvaluationModeValue(),
        agentName: this.selectedAlgorithms.join(','),
        environment: this.taskForm.environmentCode,
        deadline: this.taskForm.deadline,
        teamGroupDeadline: this.taskMode === 'tournament' ? this.taskForm.teamGroupDeadline : null,
        config: this.buildConfigPayload()
      }

      this.saving = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${this.taskId}`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify(payload)
        })

        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '保存修改失败')
          return
        }

        if (this.taskMode === 'battle') {
          await this.uploadSelectedBotFiles(token)
          await this.loadBotStatus()
        }

        ElMessage.success('保存修改成功')
        this.$router.push('/teacher/tasks')
      } catch (error) {
        ElMessage.error(error.message || '保存修改失败，请检查后端是否已启动')
      } finally {
        this.saving = false
      }
    },
    goBack () {
      this.$router.push('/teacher/tasks')
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    goHistory () {
      this.$router.push('/teacher/history')
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
  gap: 14px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
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

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.loading-card {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
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

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
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

.upload-pair-box {
  display: flex;
  flex-direction: column;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.top-gap-mini {
  margin-top: 10px;
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
  margin-top: 14px;
}

.algorithm-btn-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.algorithm-btn {
  min-width: 98px;
  height: 38px;
  padding: 0 16px;
  border: none;
  border-radius: 4px;
  background: #303133;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.algorithm-btn-active {
  background: #1f4e8c;
}

.baseline-chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.baseline-chip {
  min-width: 80px;
  height: 32px;
  padding: 0 12px;
  border-radius: 16px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
}

.baseline-chip-active {
  border-color: #1f4e8c;
  background: #ecf5ff;
  color: #1f4e8c;
  font-weight: 600;
}

.baseline-diff-block {
  margin-top: 8px;
}

.baseline-diff-subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}

.baseline-empty {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.baseline-selected-list,
.baseline-available-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.baseline-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 1px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #ffffff;
  min-height: 20px;
}

.baseline-item-available {
  background: #fafafa;
}

.baseline-action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.baseline-x-btn {
  border: none;
  background: #f56c6c;
  color: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  height: 26px;
  width: 26px;
  font-size: 12px;
}

.baseline-add-btn {
  border: none;
  background: #1f4e8c;
  color: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  height: 28px;
  padding: 0 12px;
  font-size: 13px;
  white-space: nowrap;
}

.baseline-upload-box {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eeeeee;
}

.baseline-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.baseline-upload-label {
  width: 72px;
  font-size: 13px;
  color: #606266;
}

.baseline-algo-input {
  flex: 1;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 10px;
  font-size: 14px;
  outline: none;
}

.baseline-file-input {
  flex: 1;
}

.baseline-file-name {
  font-size: 13px;
  color: #606266;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.baseline-upload-btn {
  border: none;
  background: #303133;
  color: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
  white-space: nowrap;
}

.baseline-upload-btn:hover {
  background: #1f4e8c;
}

.bottom-action-row {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-btn,
.secondary-btn {
  min-width: 96px;
  height: 38px;
  padding: 0 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.primary-btn {
  border: none;
  background: #1f4e8c;
  color: #ffffff;
}

.secondary-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.primary-btn:hover {
  background: #173b69;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.algorithm-add-btn {
  min-width: 124px;
  height: 38px;
  padding: 0 20px;
  border: 1px solid #1f4e8c;
  border-radius: 4px;
  background: #ffffff;
  color: #1f4e8c;
  font-size: 14px;
  cursor: pointer;
}

.algorithm-add-btn:hover {
  background: #ecf5ff;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 3000;
}

.dialog-box {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.16);
}

.dialog-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.dialog-body {
  padding: 20px;
}

.dialog-label {
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  font-size: 14px;
  color: #303133;
  background: #ffffff;
}

.dialog-input:focus {
  border-color: #1f4e8c;
}

.dialog-footer {
  padding: 0 20px 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-btn {
  min-width: 90px;
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

  .difficulty-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .upload-box {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
