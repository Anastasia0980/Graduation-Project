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
        active-menu='publish-task'
        @teacher-home-click='goTeacherHome'
        @task-hall-click='goTaskHall'
        @history-click='goHistory'
        @publish-click='goPublishTask'
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
                <option value='LunarLander-v3'>LunarLander-v3</option>
              </select>
            </div>

            <div class='form-item full-width'>
              <label>学生可选模型</label>
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
              </div>
            </div>
          </div>
        </div>

        <div v-if='taskMode === "single"' class='card section-space'>
          <div class='card-title'>Baseline 配置</div>

          <div class='form-grid'>
            <div class='form-item full-width'>
              <label>简单难度</label>
              <div class='baseline-diff-block'>
                <div class='baseline-diff-subtitle'>已选</div>
                <div v-if='(baselineSelectedOptionsByDifficulty.easy || []).length === 0' class='baseline-empty'>未选择</div>
                <div v-else class='baseline-selected-list'>
                  <div
                    v-for='opt in baselineSelectedOptionsByDifficulty.easy'
                    :key='opt.id'
                    class='baseline-item'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <button type='button' class='baseline-x-btn' @click='removeBaselineFromSelection("easy", opt.id)'>X</button>
                  </div>
                </div>

                <div class='baseline-diff-subtitle'>可选</div>
                <div v-if='baselineAvailableOptionsByDifficulty.easy.length === 0' class='baseline-empty'>暂无可选</div>
                <div v-else class='baseline-available-list'>
                  <div
                    v-for='opt in baselineAvailableOptionsByDifficulty.easy'
                    :key='opt.id'
                    class='baseline-item baseline-item-available'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <div class='baseline-action-buttons'>
                      <button type='button' class='baseline-add-btn' @click='addBaselineToSelection("easy", opt.id)'>加入</button>
                      <button type='button' class='baseline-x-btn' @click='softDeleteBaselineFromCatalog("easy", opt)'>X</button>
                    </div>
                  </div>
                </div>

                <div class='baseline-upload-box'>
                  <div class='baseline-row'>
                    <span class='baseline-upload-label'>algorithm：</span>
                    <input v-model='baselineUpload.easy.algorithm' class='baseline-algo-input' placeholder='例如 DQN / DDQN' />
                  </div>
                  <div class='baseline-row'>
                    <input
                      type='file'
                      accept='.pth'
                      class='baseline-file-input'
                      @change='handleBaselineFileChange($event, "easy")'
                    />
                    <span class='baseline-file-name'>{{ baselineUpload.easy.fileName }}</span>
                    <button type='button' class='baseline-upload-btn' @click='uploadBaselineModel("easy")'>
                      上传并加入
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class='form-item full-width'>
              <label>中等难度</label>
              <div class='baseline-diff-block'>
                <div class='baseline-diff-subtitle'>已选</div>
                <div v-if='(baselineSelectedOptionsByDifficulty.medium || []).length === 0' class='baseline-empty'>未选择</div>
                <div v-else class='baseline-selected-list'>
                  <div
                    v-for='opt in baselineSelectedOptionsByDifficulty.medium'
                    :key='opt.id'
                    class='baseline-item'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <button type='button' class='baseline-x-btn' @click='removeBaselineFromSelection("medium", opt.id)'>X</button>
                  </div>
                </div>

                <div class='baseline-diff-subtitle'>可选</div>
                <div v-if='baselineAvailableOptionsByDifficulty.medium.length === 0' class='baseline-empty'>暂无可选</div>
                <div v-else class='baseline-available-list'>
                  <div
                    v-for='opt in baselineAvailableOptionsByDifficulty.medium'
                    :key='opt.id'
                    class='baseline-item baseline-item-available'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <div class='baseline-action-buttons'>
                      <button type='button' class='baseline-add-btn' @click='addBaselineToSelection("medium", opt.id)'>加入</button>
                      <button type='button' class='baseline-x-btn' @click='softDeleteBaselineFromCatalog("medium", opt)'>X</button>
                    </div>
                  </div>
                </div>

                <div class='baseline-upload-box'>
                  <div class='baseline-row'>
                    <span class='baseline-upload-label'>algorithm：</span>
                    <input v-model='baselineUpload.medium.algorithm' class='baseline-algo-input' placeholder='例如 DQN / DDQN' />
                  </div>
                  <div class='baseline-row'>
                    <input
                      type='file'
                      accept='.pth'
                      class='baseline-file-input'
                      @change='handleBaselineFileChange($event, "medium")'
                    />
                    <span class='baseline-file-name'>{{ baselineUpload.medium.fileName }}</span>
                    <button type='button' class='baseline-upload-btn' @click='uploadBaselineModel("medium")'>
                      上传并加入
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class='form-item full-width'>
              <label>困难难度</label>
              <div class='baseline-diff-block'>
                <div class='baseline-diff-subtitle'>已选</div>
                <div v-if='(baselineSelectedOptionsByDifficulty.hard || []).length === 0' class='baseline-empty'>未选择</div>
                <div v-else class='baseline-selected-list'>
                  <div
                    v-for='opt in baselineSelectedOptionsByDifficulty.hard'
                    :key='opt.id'
                    class='baseline-item'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <button type='button' class='baseline-x-btn' @click='removeBaselineFromSelection("hard", opt.id)'>X</button>
                  </div>
                </div>

                <div class='baseline-diff-subtitle'>可选</div>
                <div v-if='baselineAvailableOptionsByDifficulty.hard.length === 0' class='baseline-empty'>暂无可选</div>
                <div v-else class='baseline-available-list'>
                  <div
                    v-for='opt in baselineAvailableOptionsByDifficulty.hard'
                    :key='opt.id'
                    class='baseline-item baseline-item-available'
                  >
                    <span>{{ opt.label || opt.algorithm || opt.id }}</span>
                    <div class='baseline-action-buttons'>
                      <button type='button' class='baseline-add-btn' @click='addBaselineToSelection("hard", opt.id)'>加入</button>
                      <button type='button' class='baseline-x-btn' @click='softDeleteBaselineFromCatalog("hard", opt)'>X</button>
                    </div>
                  </div>
                </div>

                <div class='baseline-upload-box'>
                  <div class='baseline-row'>
                    <span class='baseline-upload-label'>algorithm：</span>
                    <input v-model='baselineUpload.hard.algorithm' class='baseline-algo-input' placeholder='例如 DQN / DDQN' />
                  </div>
                  <div class='baseline-row'>
                    <input
                      type='file'
                      accept='.pth'
                      class='baseline-file-input'
                      @change='handleBaselineFileChange($event, "hard")'
                    />
                    <span class='baseline-file-name'>{{ baselineUpload.hard.fileName }}</span>
                    <button type='button' class='baseline-upload-btn' @click='uploadBaselineModel("hard")'>
                      上传并加入
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
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherPublishTaskView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '王老师',
      publishing: false,
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
      classOptions: [],
      algorithmOptions: ['DDPG', 'DQN', 'Qlearning', 'PPO'],
      selectedAlgorithms: [],
      // baseline catalog（来自后端 /baselines/catalog）
      baselineCatalog: {
        easy: [],
        medium: [],
        hard: []
      },
      // baseline 选中项（存 baselineOption.id）
      baselineSelectedIds: {
        easy: [],
        medium: [],
        hard: []
      },
      // baseline 上传输入（一次上传一个 difficulty+algorithm）
      baselineUpload: {
        easy: { algorithm: '', file: null, fileName: '当前未选择文件' },
        medium: { algorithm: '', file: null, fileName: '当前未选择文件' },
        hard: { algorithm: '', file: null, fileName: '当前未选择文件' }
      },
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
  created () {
    this.loadClassOptions()
  },
  watch: {
    'taskForm.environmentCode': {
      immediate: true,
      handler () {
        const token = localStorage.getItem('auth_token')
        if (!token) return

        // 切换环境后 baseline 选项应重新拉取，同时清空已选项
        this.baselineSelectedIds.easy = []
        this.baselineSelectedIds.medium = []
        this.baselineSelectedIds.hard = []
        this.loadBaselineCatalog(token)
      }
    }
  },
  computed: {
    baselineSelectedOptionsByDifficulty () {
      const buildSelected = (diff) => {
        const ids = this.baselineSelectedIds?.[diff] || []
        return ids
          .map(id => this.getBaselineOptionById(diff, id))
          .filter(Boolean)
      }
      return {
        easy: buildSelected('easy'),
        medium: buildSelected('medium'),
        hard: buildSelected('hard')
      }
    },
    baselineAvailableOptionsByDifficulty () {
      const buildAvailable = (diff) => {
        const catalogList = this.baselineCatalog?.[diff] || []
        const selected = this.baselineSelectedIds?.[diff] || []
        return catalogList.filter(opt => opt && !selected.includes(opt.id))
      }
      return {
        easy: buildAvailable('easy'),
        medium: buildAvailable('medium'),
        hard: buildAvailable('hard')
      }
    }
  },
  methods: {
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

        const catalog = result.data || {}
        this.baselineCatalog = {
          easy: Array.isArray(catalog.easy) ? catalog.easy : [],
          medium: Array.isArray(catalog.medium) ? catalog.medium : [],
          hard: Array.isArray(catalog.hard) ? catalog.hard : []
        }
      } catch (error) {
        ElMessage.warning(error.message || 'Baseline catalog 加载失败')
      }
    },
    addBaselineToSelection (difficulty, baselineId) {
      const list = this.baselineSelectedIds[difficulty] || []
      if (!list.includes(baselineId)) {
        this.baselineSelectedIds[difficulty] = [...list, baselineId]
      }
    },
    removeBaselineFromSelection (difficulty, baselineId) {
      const list = this.baselineSelectedIds[difficulty] || []
      this.baselineSelectedIds[difficulty] = list.filter(id => id !== baselineId)
    },
    getBaselineOptionById (difficulty, baselineId) {
      const catalogList = this.baselineCatalog?.[difficulty] || []
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
        modelPath: algoKey ? `${env}/${difficulty}/${algoKey}/baseline.pth` : ''
      }
    },
    handleBaselineFileChange (event, difficulty) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      this.baselineUpload[difficulty].file = file
      this.baselineUpload[difficulty].fileName = file.name
    },
    async uploadBaselineModel (difficulty) {
      const upload = this.baselineUpload[difficulty] || {}
      const algorithm = String(upload.algorithm || '').trim()
      const file = upload.file

      if (!algorithm) {
        ElMessage.warning(`请输入${difficulty}算法名`)
        return
      }
      if (!file) {
        ElMessage.warning(`请选择${difficulty}的 baseline.pth 文件`)
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
        formData.append('difficulty', difficulty)
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

        const baselineId = `${difficulty}-${algorithm.toLowerCase()}`
        this.addBaselineToSelection(difficulty, baselineId)

        this.baselineUpload[difficulty].file = null
        this.baselineUpload[difficulty].fileName = '当前未选择文件'
      } catch (error) {
        ElMessage.error(error.message || 'baseline 上传失败，请检查后端是否已启动')
      }
    },
    async softDeleteBaselineFromCatalog (difficulty, opt) {
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
          '确认将该 baseline 资源删除吗？\n删除后将从可选列表中消失。',
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
        difficulty,
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
          this.removeBaselineFromSelection(difficulty, baselineId)
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

      const baselineOptions = {}
      const env = this.taskForm.environmentCode || 'tictactoe_v3'
      ;['easy', 'medium', 'hard'].forEach(diff => {
        const selectedIds = this.baselineSelectedIds[diff] || []
        const catalogList = this.baselineCatalog[diff] || []

        baselineOptions[diff] = selectedIds.map(id => {
          const fromCatalog = catalogList.find(opt => opt && opt.id === id)
          if (fromCatalog) return fromCatalog

          // 兜底：当 catalog 尚未加载/找不到该 id 时，通过 id 推断 modelPath
          const parts = String(id || '').split('-')
          const algoKey = parts[1] || ''

          return {
            id,
            label: algoKey ? String(algoKey).toUpperCase() : id,
            algorithm: algoKey,
            modelPath: `${env}/${diff}/${algoKey}/baseline.pth`
          }
        })
      })

      return {
        overview: this.taskForm.intro,
        rules: rulesText,
        observationSpace: this.taskForm.observation,
        actionSpace: this.taskForm.actionSpace,
        rewardFunction: this.taskForm.reward,
        evaluationFunction: this.taskForm.evaluation,
        baselineOptions
      }
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
      if (this.selectedAlgorithms.length === 0) {
        ElMessage.warning('请至少选择一个学生可选模型')
        return false
      }
      if (this.taskMode === 'single') {
        const totalBaselineSelected = ['easy', 'medium', 'hard'].reduce((sum, diff) => {
          return sum + ((this.baselineSelectedIds[diff] || []).length)
        }, 0)
        if (totalBaselineSelected === 0) {
          ElMessage.warning('请至少选择一个 baseline')
          return false
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
        config: this.buildConfigPayload()
      }

      this.publishing = true
      try {
        const response = await fetch(`${API_BASE}/class/${this.taskForm.classId}/assignments`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify(payload)
        })

        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '发布任务失败')
          return
        }

        const assignmentId = result.data && result.data.id ? result.data.id : result.data
        if (this.taskMode === 'battle' && assignmentId) {
          await this.uploadSystemBotFiles(assignmentId, token)
        }

        ElMessage.success('任务发布成功')
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

.top-gap {
  margin-top: 12px;
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
