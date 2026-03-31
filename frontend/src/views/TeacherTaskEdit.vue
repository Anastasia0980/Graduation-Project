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
            <div class='card-title'>任务环境及模型</div>

            <div class='form-grid'>
              <div class='form-item full-width'>
                <label>模型环境</label>
                <select v-model='taskForm.environmentCode'>
                  <option value='tictactoe_v3'>tictactoe_v3</option>
                  <option value='connect_four_v3'>connect_four_v3</option>
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
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

const API_BASE = 'http://localhost:8080'

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
      submitStrategy: 'once',
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
        tournamentRule: ''
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
        tournamentRule: ''
      }
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

      return {
        overview: this.taskForm.intro,
        rules: rulesText,
        observationSpace: this.taskForm.observation,
        actionSpace: this.taskForm.actionSpace,
        rewardFunction: this.taskForm.reward,
        evaluationFunction: this.taskForm.evaluation,
        algorithmOptions: this.algorithmOptions
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
      if (this.selectedAlgorithms.length === 0) {
        ElMessage.warning('请至少选择一个可用算法')
        return false
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
