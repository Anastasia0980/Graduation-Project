<template>
  <div class="home-page">
    <AppTopbar
      :logged-in="isLoggedIn"
      :user-name="userName"
      :current-role="currentRole"
      active-nav="home"
      @platform-click="goHome"
      @user-click="handleUserClick"
      @logout="logout"
      @login="goLogin"
      @register="goRegister"
    />

    <div class="layout">
      <StudentSidebar
        v-if="isLoggedIn && currentRole === 'student'"
        :logged-in="true"
        :active-menu="currentMenu"
        :task-menu-open="taskMenuOpen"
        @profile-click="goProfile"
        @class-click="goStudentClass"
        @toggle-task-menu="toggleTaskMenu"
        @open-task-click="selectOpenMenu"
        @ended-task-click="selectEndedMenu"
        @history-click="goHistory"
      />

      <StudentSidebar
        v-else
        :logged-in="false"
        :active-menu="''"
        :task-menu-open="false"
      />

      <main class="content-area">
        <template v-if="isLoggedIn && currentRole === 'student'">
          <div v-if="currentMenu === 'task-open'" class="page-block">
            <div class="page-header">
              <h2>开放中的任务</h2>
            </div>

            <div v-if="loading" class="task-empty-card">任务加载中...</div>

            <template v-else>
              <div v-if="pagedOpenTasks.length > 0" class="task-grid three-columns">
                <div
                  v-for="task in pagedOpenTasks"
                  :key="task.id"
                  class="task-card"
                >
                  <div class="task-image-box" @click="goTaskDetail(task)">
                    <img :src="task.image" class="task-image" alt="任务图片">
                  </div>

                  <div class="task-body">
                    <div class="task-type-badge">{{ task.modeLabel }}</div>
                    <div class="task-title" @click="goTaskDetail(task)">{{ task.title }}</div>
                    <div class="task-desc">{{ task.desc }}</div>

                    <div class="task-meta">
                      <div class="task-meta-item">教师：{{ task.teacher }}</div>
                      <div class="task-meta-item">截止时间：{{ task.deadline }}</div>
                    </div>

                    <button class="submit-btn" @click="goTaskDetail(task)">进入任务</button>
                  </div>
                </div>
              </div>

              <div v-else class="task-empty-card">当前暂无开放中的任务</div>
            </template>

            <CommonPagination
              v-model:currentPage="openCurrentPage"
              v-model:pageSize="openPageSize"
              :total="openTasks.length"
              :page-size-options="[3, 6, 9]"
            />
          </div>

          <div v-if="currentMenu === 'task-ended'" class="page-block">
            <div class="page-header">
              <h2>已结束的任务</h2>
            </div>

            <div v-if="loading" class="task-empty-card">任务加载中...</div>

            <template v-else>
              <div v-if="pagedEndedTasks.length > 0" class="task-grid three-columns">
                <div
                  v-for="task in pagedEndedTasks"
                  :key="task.id"
                  class="task-card"
                >
                  <div class="task-image-box" @click="goTaskDetail(task)">
                    <img :src="task.image" class="task-image" alt="任务图片">
                  </div>

                  <div class="task-body">
                    <div class="task-type-badge ended-badge">{{ task.modeLabel }}</div>
                    <div class="task-title" @click="goTaskDetail(task)">{{ task.title }}</div>
                    <div class="task-desc">{{ task.desc }}</div>

                    <div class="task-meta">
                      <div class="task-meta-item">教师：{{ task.teacher }}</div>
                      <div class="task-meta-item">截止时间：{{ task.deadline }}</div>
                    </div>

                    <button class="submit-btn ended-btn" @click="goTaskDetail(task)">查看</button>
                  </div>
                </div>
              </div>

              <div v-else class="task-empty-card">当前暂无已结束的任务</div>
            </template>

            <CommonPagination
              v-model:currentPage="endedCurrentPage"
              v-model:pageSize="endedPageSize"
              :total="endedTasks.length"
              :page-size-options="[3, 6, 9]"
            />
          </div>
        </template>

        <template v-else>
          <div class="page-block empty-home-block">
            <div class="empty-home-card">
              <h2>主页</h2>
              <p>当前未登录，请先登录或注册后再进入任务广场、提交历史和个人主页。</p>
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import tictactoeImage from '../assets/tictactoe.png'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'HomeView',
  components: {
    AppTopbar,
    StudentSidebar,
    CommonPagination
  },
  data () {
    return {
      isLoggedIn: false,
      currentRole: '',
      userName: '',
      taskMenuOpen: true,
      currentMenu: 'task-open',
      openCurrentPage: 1,
      openPageSize: 3,
      endedCurrentPage: 1,
      endedPageSize: 3,
      loading: false,
      taskList: []
    }
  },
  created () {
    this.syncLoginState()
  },
  watch: {
    '$route.query': {
      handler () {
        this.syncLoginState()
      },
      deep: true
    }
  },
  computed: {
    openTasks () {
      return this.taskList.filter(task => task.isOpen)
    },
    endedTasks () {
      return this.taskList.filter(task => !task.isOpen)
    },
    pagedOpenTasks () {
      const start = (this.openCurrentPage - 1) * this.openPageSize
      const end = start + this.openPageSize
      return this.openTasks.slice(start, end)
    },
    pagedEndedTasks () {
      const start = (this.endedCurrentPage - 1) * this.endedPageSize
      const end = start + this.endedPageSize
      return this.endedTasks.slice(start, end)
    }
  },
  methods: {
    async syncLoginState () {
      const token = localStorage.getItem('auth_token')
      const role = localStorage.getItem('auth_role')
      const userName = localStorage.getItem('auth_name')

      this.isLoggedIn = !!token
      this.currentRole = role === 'TEACHER' ? 'teacher' : (token ? 'student' : '')
      this.userName = userName || ''

      if (this.isLoggedIn && this.currentRole === 'student') {
        this.taskMenuOpen = true
        this.currentMenu = this.$route.query.tab === 'ended' ? 'task-ended' : 'task-open'
        await this.loadTaskList()
      } else {
        this.taskList = []
      }
    },
    async loadTaskList () {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.taskList = []
        return
      }

      this.loading = true
      try {
        const response = await fetch(`${API_BASE}/me/assignments?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '任务列表加载失败')
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.taskList = content.map(item => this.mapTaskItem(item))
        this.resetPaginationIfNeeded()
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
        teacher: item.teacherName || item.teacherUsername || item.teacher || '--',
        deadline: this.formatDateTime(item.deadline),
        image: tictactoeImage,
        taskMode: this.mapTaskMode(evaluationMode),
        modeLabel: this.mapModeLabel(evaluationMode),
        hasEasyBot: false,
        hasMediumBot: false,
        hasHardBot: false,
        isOpen
      }
    },
    getTaskDescription (item) {
      const config = this.parseTaskConfig(item)
      if (config.overview) {
        return config.overview
      }

      const evaluationMode = (item.evaluationMode || '').toUpperCase()
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') {
        return '学生提交模型后可参与对战任务。'
      }
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') {
        return '学生进入任务后可进行分组对战模式相关操作。'
      }
      return '学生提交模型后，在预设环境下独立完成测评。'
    },
    parseTaskConfig (item) {
      if (item.config && typeof item.config === 'object') {
        return item.config
      }
      if (item.configJson) {
        try {
          return JSON.parse(item.configJson)
        } catch (error) {
          return {}
        }
      }
      return {}
    },
    mapTaskMode (evaluationMode) {
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') {
        return 'battle'
      }
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') {
        return 'tournament'
      }
      return 'single'
    },
    mapModeLabel (evaluationMode) {
      if (evaluationMode === 'VERSUS' || evaluationMode === 'BATTLE') {
        return '对战模式'
      }
      if (evaluationMode === 'TEAM' || evaluationMode === 'TOURNAMENT') {
        return '分组对战模式'
      }
      return '单人模式'
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
    resetPaginationIfNeeded () {
      const maxOpenPage = Math.max(1, Math.ceil(this.openTasks.length / this.openPageSize))
      const maxEndedPage = Math.max(1, Math.ceil(this.endedTasks.length / this.endedPageSize))

      if (this.openCurrentPage > maxOpenPage) {
        this.openCurrentPage = maxOpenPage
      }
      if (this.endedCurrentPage > maxEndedPage) {
        this.endedCurrentPage = maxEndedPage
      }
    },
    goHome () {
      this.$router.replace('/')
    },
    goLogin () {
      this.$router.push('/login')
    },
    goRegister () {
      this.$router.push('/register')
    },
    handleUserClick () {
      if (this.currentRole === 'teacher') {
        this.$router.push('/teacher/home')
      } else if (this.currentRole === 'student') {
        this.$router.push('/student/profile')
      }
    },
    logout () {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
      this.isLoggedIn = false
      this.currentRole = ''
      this.userName = ''
      this.taskList = []
      this.$router.push('/')
    },
    toggleTaskMenu () {
      this.taskMenuOpen = !this.taskMenuOpen
    },
    selectOpenMenu () {
      this.taskMenuOpen = true
      this.currentMenu = 'task-open'
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    selectEndedMenu () {
      this.taskMenuOpen = true
      this.currentMenu = 'task-ended'
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    goStudentClass () {
      this.$router.push('/student/class')
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    goTaskDetail (task) {
      this.$router.push({
        path: '/task-detail',
        query: {
          assignmentId: task.id,
          title: task.title,
          taskMode: task.taskMode,
          hasEasyBot: task.hasEasyBot ? 'true' : 'false',
          hasMediumBot: task.hasMediumBot ? 'true' : 'false',
          hasHardBot: task.hasHardBot ? 'true' : 'false'
        }
      })
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.home-page {
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

.page-block {
  width: 100%;
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

.empty-home-block {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.empty-home-card {
  width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 28px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
}

.empty-home-card h2 {
  margin: 0 0 12px;
  font-size: 22px;
  color: #1f2d3d;
}

.empty-home-card p {
  margin: 0;
  color: #606266;
  line-height: 1.8;
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
