<template>
  <div class="page">
    <AppTopbar
      :logged-in="isLoggedIn"
      :user-name="teacherName"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @switch-role="switchRole"
      @logout="logout"
    />

    <div class="layout">
      <TeacherSidebar
        active-menu="task-hall"
        @teacher-home-click="goTeacherHome"
        @history-click="goHistory"
        @publish-click="goPublishTask"
        @manage-click="goTaskManage"
        @class-data-click="goClassData"
        @export-click="goExportScore"
        @task-hall-click="goTaskHall"
      />

      <main class="content-area">
        <div class="page-header">
          <h2>任务大厅</h2>
        </div>

        <div class="tab-row">
          <button
            class="tab-btn"
            :class="{ active: currentTab === 'open' }"
            @click="currentTab = 'open'"
          >
            进行中
          </button>
          <button
            class="tab-btn"
            :class="{ active: currentTab === 'ended' }"
            @click="currentTab = 'ended'"
          >
            已结束
          </button>
        </div>

        <div v-if="loading" class="task-empty-card">任务加载中...</div>

        <template v-else>
          <div v-if="pagedTasks.length > 0" class="task-grid three-columns">
            <div
              v-for="task in pagedTasks"
              :key="task.id"
              class="task-card"
            >
              <div class="task-image-box" @click="goTaskDetail(task)">
                <img :src="task.image" class="task-image" alt="任务图片">
              </div>

              <div class="task-body">
                <div class="task-type-badge" :class="{ 'ended-badge': !task.isOpen }">
                  {{ task.modeLabel }}
                </div>
                <div class="task-title" @click="goTaskDetail(task)">{{ task.title }}</div>
                <div class="task-desc">{{ task.desc }}</div>

                <div class="task-meta">
                  <div class="task-meta-item">截止时间：{{ task.deadline }}</div>
                </div>

                <button class="submit-btn" :class="{ 'ended-btn': !task.isOpen }" @click="goTaskDetail(task)">
                  {{ task.isOpen ? '进入任务' : '查看' }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class="task-empty-card">当前暂无任务</div>
        </template>

        <CommonPagination
          v-model:currentPage="currentPage"
          v-model:pageSize="pageSize"
          :total="filteredTasks.length"
          :page-size-options="[3, 6, 9]"
        />
      </main>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import tictactoeImage from '../assets/tictactoe.png'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest } from '../utils/http'

const API_BASE = 'http://localhost:8080'

function normalizeFileUrl (url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  if (url.startsWith('/')) return `${API_BASE}${url}`
  return `${API_BASE}/${url}`
}

export default {
  name: 'TeacherTaskHallView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      loading: false,
      currentTab: 'open',
      currentPage: 1,
      pageSize: 6,
      taskList: []
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    filteredTasks () {
      return this.currentTab === 'ended'
        ? this.taskList.filter(t => !t.isOpen)
        : this.taskList.filter(t => t.isOpen)
    },
    pagedTasks () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredTasks.slice(start, end)
    }
  },
  watch: {
    currentTab () {
      this.currentPage = 1
    }
  },
  created () {
    this.loadTaskList()
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadTaskList () {
      this.loading = true
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/assignments?pageNum=0&pageSize=100`, {
          method: 'GET'
        })
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.taskList = content.map(item => this.mapTaskItem(item))
      } catch (error) {
        this.taskList = []
        ElMessage.error(error.message || '任务列表加载失败')
      } finally {
        this.loading = false
      }
    },
    mapTaskItem (item) {
      const now = Date.now()
      const deadlineTime = item.deadline ? new Date(item.deadline).getTime() : NaN
      const isOpen = Number.isNaN(deadlineTime) ? true : deadlineTime >= now
      const evaluationMode = (item.evaluationMode || '').toUpperCase()

      return {
        id: item.id,
        title: item.title || '未命名任务',
        desc: this.getTaskDescription(item),
        deadline: this.formatDateTime(item.deadline),
        image: item.taskIcon ? normalizeFileUrl(item.taskIcon) : tictactoeImage,
        taskMode: this.mapTaskMode(evaluationMode),
        modeLabel: this.mapModeLabel(evaluationMode),
        isOpen
      }
    },
    getTaskDescription (item) {
      const config = this.parseTaskConfig(item)
      if (config && config.overview) return config.overview

      const evaluationMode = (item.evaluationMode || '').toUpperCase()
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') {
        return '进入任务后可提交模型并参与对战。'
      }
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') {
        return '进入任务后可进行分组对战模式相关操作。'
      }
      return '进入任务后可提交模型并完成测评。'
    },
    parseTaskConfig (item) {
      if (item.config && typeof item.config === 'object') return item.config
      if (item.configJson) {
        try {
          return JSON.parse(item.configJson)
        } catch (e) {
          return {}
        }
      }
      return {}
    },
    mapTaskMode (evaluationMode) {
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') return 'battle'
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') return 'tournament'
      return 'single'
    },
    mapModeLabel (evaluationMode) {
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') return '对战模式'
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') return '分组对战模式'
      return '单人模式'
    },
    formatDateTime (value) {
      if (!value) return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}`
    },
    goTaskDetail (task) {
      this.$router.push({
        path: '/task-detail',
        query: {
          assignmentId: task.id,
          title: task.title,
          taskMode: task.taskMode
        }
      })
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
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    goHistory () {
      this.$router.push('/teacher/history')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      clearAuthState()
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
  margin-bottom: 14px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.tab-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.tab-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.tab-btn.active {
  border-color: #1f4e8c;
  background: #ecf5ff;
  color: #1f4e8c;
  font-weight: 700;
}

.task-grid {
  display: grid;
  gap: 20px;
}

.three-columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.task-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
}

.task-image-box {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f3f6fa;
  cursor: pointer;
}

.task-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.task-body {
  padding: 16px;
}

.task-type-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #1f4e8c;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
}

.ended-badge {
  background: #f4f4f5;
  color: #606266;
}

.task-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 10px;
  cursor: pointer;
}

.task-title:hover {
  color: #1f4e8c;
}

.task-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  min-height: 52px;
  margin-bottom: 12px;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.task-meta-item {
  font-size: 13px;
  color: #909399;
}

.submit-btn {
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 6px;
  background: #1f4e8c;
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
}

.submit-btn:hover {
  background: #173b69;
}

.ended-btn {
  background: #606266;
}

.ended-btn:hover {
  background: #4b4f56;
}

.task-empty-card {
  width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 24px;
  color: #606266;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .three-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .three-columns {
    grid-template-columns: 1fr;
  }
}
</style>
