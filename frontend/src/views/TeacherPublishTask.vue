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
          <h2>发布任务</h2>
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
              <select v-model='taskForm.classId'>
                <option value='' disabled>请选择班级</option>
                <option
                  v-for='item in classOptions'
                  :key='item.id'
                  :value='item.id'
                >
                  {{ item.name }}
                </option>
              </select>
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
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-group'>
                <div class='upload-box'>
                  <span class='upload-label'>Config</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("easyBotConfigInput")'>选择配置文件</button>
                  <span class='upload-text'>{{ easyBotConfigFileName }}</span>
                  <input
                    ref='easyBotConfigInput'
                    class='hidden-file-input'
                    type='file'
                    accept='.json'
                    @change='handleBotFileChange($event, "easy", "config")'
                  />
                </div>
                <div class='upload-box'>
                  <span class='upload-label'>Model</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("easyBotModelInput")'>选择模型文件</button>
                  <span class='upload-text'>{{ easyBotModelFileName }}</span>
                  <input
                    ref='easyBotModelInput'
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
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-group'>
                <div class='upload-box'>
                  <span class='upload-label'>Config</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("mediumBotConfigInput")'>选择配置文件</button>
                  <span class='upload-text'>{{ mediumBotConfigFileName }}</span>
                  <input
                    ref='mediumBotConfigInput'
                    class='hidden-file-input'
                    type='file'
                    accept='.json'
                    @change='handleBotFileChange($event, "medium", "config")'
                  />
                </div>
                <div class='upload-box'>
                  <span class='upload-label'>Model</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("mediumBotModelInput")'>选择模型文件</button>
                  <span class='upload-text'>{{ mediumBotModelFileName }}</span>
                  <input
                    ref='mediumBotModelInput'
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
                <div class='difficulty-status'>前端静态上传</div>
              </div>
              <div class='upload-group'>
                <div class='upload-box'>
                  <span class='upload-label'>Config</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("hardBotConfigInput")'>选择配置文件</button>
                  <span class='upload-text'>{{ hardBotConfigFileName }}</span>
                  <input
                    ref='hardBotConfigInput'
                    class='hidden-file-input'
                    type='file'
                    accept='.json'
                    @change='handleBotFileChange($event, "hard", "config")'
                  />
                </div>
                <div class='upload-box'>
                  <span class='upload-label'>Model</span>
                  <button class='upload-btn' type='button' @click='triggerFileInput("hardBotModelInput")'>选择模型文件</button>
                  <span class='upload-text'>{{ hardBotModelFileName }}</span>
                  <input
                    ref='hardBotModelInput'
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
            说明：只要教师上传了任意一个难度的人机模型，学生端“人机对战”按钮就会由灰色变为可点击。
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

        <div v-if='taskMode === "single"' class='card section-space'>
          <div class='card-title'>闯关配置</div>
          <div class='field-tip' style='margin-bottom:12px'>
            任务将先以草稿保存，随后自动发布；学生仅能看到已发布任务。发布后闯关结构不可再改。
          </div>
          <div class='stage-toolbar'>
            <button type='button' class='secondary-btn' @click='addCurriculumStage'>添加关卡</button>
          </div>

          <div class='form-grid'>
            <div
              v-for='(stage, stageIndex) in curriculumStages'
              :key='stage.stageId'
              class='form-item full-width'
            >
              <div class='stage-header-row'>
                <label>关卡 {{ stageIndex + 1 }} · {{ stage.stageId }}</label>
                <button type='button' class='secondary-btn' @click='removeCurriculumStage(stageIndex)'>删除本关</button>
              </div>
              <div class='baseline-diff-block'>
                <div class='baseline-row'>
                  <span class='baseline-upload-label'>标题</span>
                  <input v-model='stage.title' class='baseline-algo-input' placeholder='关卡名称' />
                </div>
                <div class='baseline-row' style='align-items:flex-start'>
                  <span class='baseline-upload-label'>envSpec</span>
                  <textarea v-model='stage.envSpecText' rows='5' class='baseline-algo-input' style='height:auto;min-height:100px;font-family:monospace' placeholder='JSON，键：enable_wind, wind_power, turbulence_power, height_scale, impulse_scale, initial_angle_deg'></textarea>
                </div>

                <div class='baseline-diff-subtitle'>已选 baseline</div>
                <div v-if='!selectedBaselineOption(stage)' class='baseline-empty'>未选择</div>
                <div v-else class='baseline-selected-list'>
                  <div class='baseline-item'>
                    <span>{{ selectedBaselineOption(stage).label || selectedBaselineOption(stage).algorithm || selectedBaselineOption(stage).id }}</span>
                    <button type='button' class='baseline-x-btn' @click='removeBaselineFromStage(stage)'>X</button>
                  </div>
                </div>

                <div class='baseline-diff-subtitle'>可选</div>
                <div v-if='availableBaselineOptions(stage).length === 0' class='baseline-empty'>暂无可选</div>
                <div v-else class='baseline-available-list'>
                  <div
                    v-for='opt in availableBaselineOptions(stage)'
                    :key='opt.id'
                    class='baseline-item baseline-item-available'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <div class='baseline-action-buttons'>
                      <button type='button' class='baseline-add-btn' @click='addBaselineToStage(stage, opt.id)'>设为本关</button>
                      <button type='button' class='baseline-x-btn' @click='softDeleteBaselineFromCatalog(stage.stageId, opt)'>X</button>
                    </div>
                  </div>
                </div>

                <div class='baseline-upload-box'>
                  <div class='baseline-row'>
                    <span class='baseline-upload-label'>algorithm：</span>
                    <input v-model='stage.baselineUploadAlgorithm' class='baseline-algo-input' placeholder='需以实际算法名开头，例如 distribdqn_500 / priorddqn_500 / rainbow_500' />
                  </div>
                  <div class='baseline-row'>
                    <input
                      type='file'
                      accept='.pth'
                      :ref='`baselineFileInput_${stage.stageId}`'
                      class='baseline-file-input'
                      @change='handleBaselineFileChange($event, stage)'
                    />
                    <span class='baseline-file-name'>{{ stage.baselineFileName }}</span>
                    <button type='button' class='baseline-upload-btn' @click='uploadBaselineModel(stage)'>
                      上传并设为本关
                    </button>
                  </div>
                </div>
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
          <button class='secondary-btn' type='button'>保存草稿</button>
          <button class='primary-btn' type='button' :disabled='publishing' @click='handlePublishTask'>
            {{ publishing ? '发布中...' : '发布任务' }}
          </button>
        </div>
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
const DEFAULT_ENV_SPEC_JSON = JSON.stringify({
  enable_wind: false,
  wind_power: 0.0,
  turbulence_power: 0.0,
  height_scale: 1.0,
  impulse_scale: 0.0,
  initial_angle_deg: 0.0
}, null, 2)

export default {
  name: 'TeacherPublishTaskView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      publishing: false,
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
      classOptions: [],
      algorithmOptions: ['DDPG', 'DQN', 'QLearning'],
      selectedAlgorithms: [],
      algorithmDialogVisible: false,
      customAlgorithmName: '',
      /** 有序闯关关卡（草稿可改） */
      curriculumStages: [],
      /** 环境目录下全部 baseline 目录扫描结果 */
      globalBaselineCatalog: {},
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
  created () {
    this.loadClassOptions()
    this.initCurriculumStagesIfEmpty()
  },
  watch: {
    'taskForm.environmentCode': {
      immediate: true,
      handler () {
        const token = localStorage.getItem('auth_token')
        if (!token) return

        this.curriculumStages.forEach(s => {
          s.selectedBaselineId = null
        })
        this.loadBaselineCatalog(token)
      }
    }
  },
  computed: {},
  methods: {
    newStageRow (title) {
      return {
        stageId: 'st_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
        title: title || '新关卡',
        envSpecText: DEFAULT_ENV_SPEC_JSON,
        selectedBaselineId: null,
        baselineUploadAlgorithm: '',
        baselineFile: null,
        baselineFileName: '当前未选择文件'
      }
    },
    initCurriculumStagesIfEmpty () {
      if (!this.curriculumStages || this.curriculumStages.length === 0) {
        this.curriculumStages.push(this.newStageRow('第 1 关'))
      }
    },
    addCurriculumStage () {
      this.curriculumStages.push(this.newStageRow(`第 ${this.curriculumStages.length + 1} 关`))
    },
    removeCurriculumStage (index) {
      if (this.curriculumStages.length <= 1) {
        ElMessage.warning('至少保留一关')
        return
      }
      this.curriculumStages.splice(index, 1)
    },
    selectedBaselineOption (stage) {
      const sid = stage && stage.selectedBaselineId
      if (!sid) return null
      return this.getBaselineOptionById(stage.stageId, sid)
    },
    availableBaselineOptions (stage) {
      const catalogList = this.globalBaselineCatalog[stage.stageId] || []
      const selectedId = stage.selectedBaselineId
      return catalogList.filter(opt => opt && opt.id !== selectedId)
    },
    addBaselineToStage (stage, baselineId) {
      stage.selectedBaselineId = baselineId
    },
    removeBaselineFromStage (stage) {
      stage.selectedBaselineId = null
    },
    async loadClassOptions () {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        return
      }

      try {
        const response = await fetch(`${API_BASE}/class?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          return
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.classOptions = content.map(item => ({
          id: item.id,
          name: item.name
        }))

        if (!this.taskForm.classId && this.classOptions.length > 0) {
          this.taskForm.classId = this.classOptions[0].id
        }
      } catch (error) {
        this.classOptions = []
      }
    },
    triggerFileInput (refName) {
      const input = this.$refs[refName]
      if (input) {
        input.click()
      }
    },
    handleTaskIconChange (event) {
      const file = event.target.files && event.target.files[0]
      this.taskIconFileName = file ? file.name : '当前未选择文件'
    },
    handleBotFileChange (event, level, type) {
      const file = event.target.files && event.target.files[0]
      const fileName = file ? file.name : '当前未选择文件'

      if (level === 'easy') {
        if (type === 'config') {
          this.easyBotConfigFileName = fileName
        } else {
          this.easyBotModelFileName = fileName
        }
      } else if (level === 'medium') {
        if (type === 'config') {
          this.mediumBotConfigFileName = fileName
        } else {
          this.mediumBotModelFileName = fileName
        }
      } else if (level === 'hard') {
        if (type === 'config') {
          this.hardBotConfigFileName = fileName
        } else {
          this.hardBotModelFileName = fileName
        }
      }
    },
    async uploadSystemBotFiles (assignmentId, token) {
      const uploadOne = async (difficulty, configRef, modelRef) => {
        const configInput = this.$refs[configRef]
        const modelInput = this.$refs[modelRef]
        const configFile = configInput && configInput.files ? configInput.files[0] : null
        const modelFile = modelInput && modelInput.files ? modelInput.files[0] : null

        if (!configFile && !modelFile) {
          return
        }

        if (!configFile || !modelFile) {
          throw new Error(`${difficulty}难度人机模型需同时上传config和model文件`)
        }

        const formData = new FormData()
        formData.append('config', configFile)
        formData.append('model', modelFile)

        const response = await fetch(`${API_BASE}/assignments/${assignmentId}/system-bots/${difficulty}`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`
          },
          body: formData
        })

        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || `${difficulty}难度人机模型上传失败`)
        }
      }

      await uploadOne('easy', 'easyBotConfigInput', 'easyBotModelInput')
      await uploadOne('medium', 'mediumBotConfigInput', 'mediumBotModelInput')
      await uploadOne('hard', 'hardBotConfigInput', 'hardBotModelInput')
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
    async loadBaselineCatalog (token) {
      try {
        const environment = this.taskForm.environmentCode
        const response = await fetch(`${API_BASE}/baselines/catalog?environment=${encodeURIComponent(environment)}`, {
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

        this.globalBaselineCatalog = result.data && typeof result.data === 'object' ? result.data : {}
      } catch (error) {
        ElMessage.warning(error.message || 'Baseline catalog 加载失败')
      }
    },
    getBaselineOptionById (taskId, baselineId) {
      const catalogList = this.globalBaselineCatalog?.[taskId] || []
      const fromCatalog = catalogList.find(opt => opt && opt.id === baselineId)
      if (fromCatalog) return fromCatalog

      // 兜底：当 catalog 暂未加载或找不到该 id 时，通过 id 推断展示 label/modelPath
      const parts = String(baselineId || '').split('-')
      const algoKey = parts[1] || ''
      const env = this.taskForm.environmentCode || 'tictactoe_v3'
      return {
        id: baselineId,
        label: algoKey ? String(algoKey).toUpperCase() : baselineId,
        algorithm: algoKey,
        modelPath: algoKey ? `${env}/${taskId}/${algoKey}/baseline.pth` : ''
      }
    },
    handleBaselineFileChange (event, stage) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      stage.baselineFile = file
      stage.baselineFileName = file.name
    },
    resetBaselineUpload (stage) {
      stage.baselineUploadAlgorithm = ''
      stage.baselineFile = null
      stage.baselineFileName = '当前未选择文件'
      const fileInputRef = this.$refs[`baselineFileInput_${stage.stageId}`]
      const fileInput = Array.isArray(fileInputRef) ? fileInputRef[0] : fileInputRef
      if (fileInput) {
        fileInput.value = ''
      }
    },
    normalizeBaselineAlgorithm (value) {
      return String(value || '').trim().toLowerCase()
    },
    isValidBaselineAlgorithm (value) {
      // 需以算法名开头，后续可追加下划线分段（如 distribdqn_500 / priorddqn_500 / rainbow_500）
      return /^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$/.test(value)
    },
    async uploadBaselineModel (stage) {
      const algorithm = this.normalizeBaselineAlgorithm(stage.baselineUploadAlgorithm)
      const file = stage.baselineFile

      if (!algorithm) {
        ElMessage.warning(`请输入 ${stage.stageId} 算法名`)
        return
      }
      if (!this.isValidBaselineAlgorithm(algorithm)) {
        ElMessage.warning('算法名格式不正确：需以实际算法名开头，例如 distribdqn_500 / priorddqn_500 / rainbow_500')
        return
      }
      if (!file) {
        ElMessage.warning(`请选择 ${stage.stageId} 的 baseline.pth 文件`)
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      try {
        const formData = new FormData()
        formData.append('environment', this.taskForm.environmentCode || 'tictactoe_v3')
        formData.append('taskId', stage.stageId)
        formData.append('algorithm', algorithm)
        formData.append('model', file)

        const response = await fetch(`${API_BASE}/baselines/upload`, {
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

        await this.loadBaselineCatalog(token)

        const baselineId = `${stage.stageId}-${algorithm}`
        this.addBaselineToStage(stage, baselineId)

        this.resetBaselineUpload(stage)
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

        ElMessage.success('baseline 已删除')
        if (baselineId) {
          this.curriculumStages.forEach(s => {
            if (s.stageId === taskId && s.selectedBaselineId === baselineId) {
              s.selectedBaselineId = null
            }
          })
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
      const base = {
        overview: this.taskForm.intro,
        rules: this.taskForm.rule,
        observationSpace: this.taskForm.observation,
        actionSpace: this.taskForm.actionSpace,
        rewardFunction: this.taskForm.reward,
        evaluationFunction: this.taskForm.evaluation,
        algorithmOptions: this.algorithmOptions,
        teamMaxMembers: Number(this.teamMax || 3)
      }
      if (this.taskMode !== 'single') {
        return base
      }

      const env = this.taskForm.environmentCode || 'tictactoe_v3'
      const curriculumStages = this.curriculumStages.map(s => {
        let envSpec
        try {
          envSpec = JSON.parse(s.envSpecText || '{}')
        } catch (e) {
          throw new Error('envSpec JSON 解析失败：' + (s.stageId || ''))
        }
        const selectedId = s.selectedBaselineId
        if (!selectedId) {
          throw new Error('关卡未选择 baseline：' + (s.stageId || ''))
        }
        const catalogList = this.globalBaselineCatalog[s.stageId] || []
        const fromCatalog = catalogList.find(opt => opt && opt.id === selectedId)
        let baseline
        if (fromCatalog) {
          baseline = fromCatalog
        } else {
          const parts = String(selectedId || '').split('-')
          const algoKey = parts.slice(1).join('-') || ''
          baseline = {
            id: selectedId,
            label: algoKey ? String(algoKey).toUpperCase() : selectedId,
            algorithm: algoKey,
            modelPath: `${env}/${s.stageId}/${algoKey}/baseline.pth`
          }
        }
        return {
          stageId: String(s.stageId || '').trim(),
          title: s.title || '',
          envSpec,
          baseline
        }
      })

      return { ...base, curriculumStages }
    },
    validatePublishForm () {
      if (!this.taskForm.name.trim()) {
        ElMessage.warning('请输入任务名称')
        return false
      }
      if (!this.taskForm.classId) {
        ElMessage.warning('请选择适用班级')
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
        if (!this.curriculumStages || this.curriculumStages.length === 0) {
          ElMessage.warning('请至少配置一关')
          return false
        }
        for (const s of this.curriculumStages) {
          try {
            JSON.parse(s.envSpecText || '{}')
          } catch (e) {
            ElMessage.warning(`关卡 ${s.stageId} 的 envSpec 不是合法 JSON`)
            return false
          }
          if (!s.selectedBaselineId) {
            ElMessage.warning(`请为关卡 ${s.stageId} 选择 baseline`)
            return false
          }
        }
      }
      return true
    },
    async handlePublishTask () {
      if (!this.validatePublishForm()) {
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
        teamGroupDeadline: this.taskMode === 'tournament' ? this.taskForm.teamGroupDeadline : null
      }

      let bodyPayload
      try {
        bodyPayload = { ...payload, config: this.buildConfigPayload() }
      } catch (e) {
        ElMessage.warning(e.message || '配置构建失败')
        return
      }

      this.publishing = true
      try {
        const response = await fetch(`${API_BASE}/class/${this.taskForm.classId}/assignments`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify(bodyPayload)
        })

        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '发布任务失败')
          return
        }

        const assignmentId = result.data
        if (assignmentId != null) {
          const pubResp = await fetch(`${API_BASE}/assignments/${assignmentId}/publish`, {
            method: 'POST',
            headers: {
              Authorization: `Bearer ${token}`
            }
          })
          const pubJson = await pubResp.json()
          if (!pubResp.ok || pubJson.code !== 0) {
            ElMessage.error(pubJson.message || '任务已创建为草稿，但发布失败，请在任务管理中手动发布')
            this.$router.push('/teacher/tasks')
            return
          }
        }

        if (this.taskMode === 'battle' && assignmentId) {
          await this.uploadSystemBotFiles(assignmentId, token)
        }

        ElMessage.success('任务创建并发布成功')
        this.$router.push('/teacher/tasks')
      } catch (error) {
        ElMessage.error('发布任务失败，请检查后端是否已启动')
      } finally {
        this.publishing = false
      }
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

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
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

.upload-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-label {
  width: 48px;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
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

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.top-gap {
  margin-top: 12px;
}

.stage-toolbar {
  margin-bottom: 12px;
}

.stage-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
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

.algorithm-btn:hover {
  background: #1f2d3d;
}

.algorithm-btn-active {
  background: #1f4e8c;
}

.algorithm-btn-active:hover {
  background: #173b69;
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

.primary-btn:hover {
  background: #173b69;
}

.primary-btn:disabled {
  background: #90a4c3;
  cursor: not-allowed;
}

.secondary-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
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
