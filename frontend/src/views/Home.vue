<template>
  <div class='home-page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='userName'
      :current-role='currentRole'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='handleUserClick'
      @switch-role='switchRole'
      @logout='logout'
      @login='goLogin'
      @register='goRegister'
    />

    <div class='layout'>
      <StudentSidebar
        v-if='!loggedOutView && isLoggedIn && currentRole === "student"'
        :logged-in='true'
        :active-menu='currentMenu'
        :task-menu-open='taskMenuOpen'
        @profile-click='goProfile'
        @class-click='goStudentClass'
        @toggle-task-menu='toggleTaskMenu'
        @open-task-click='selectOpenMenu'
        @ended-task-click='selectEndedMenu'
        @history-click='goHistory'
      />

      <StudentSidebar
        v-if='loggedOutView || !isLoggedIn'
        :logged-in='false'
        :active-menu='""'
        :task-menu-open='false'
      />

      <main class='content-area'>
        <template v-if='!loggedOutView && isLoggedIn && currentRole === "student"'>
          <div v-if='currentMenu === "task-open"' class='page-block'>
            <div class='page-header'>
              <h2>开放中的任务</h2>
            </div>

            <div class='task-grid three-columns'>
              <div
                v-for='task in pagedOpenTasks'
                :key='task.id'
                class='task-card'
              >
                <div class='task-image-box' @click='goTaskDetail(task)'>
                  <img :src='task.image' class='task-image' alt='任务图片' />
                </div>

                <div class='task-body'>
                  <div class='task-type-badge'>{{ task.modeLabel }}</div>
                  <div class='task-title' @click='goTaskDetail(task)'>{{ task.title }}</div>
                  <div class='task-desc'>{{ task.desc }}</div>

                  <div class='task-meta'>
                    <div class='task-meta-item'>教师：{{ task.teacher }}</div>
                    <div class='task-meta-item'>截止时间：{{ task.deadline }}</div>
                  </div>

                  <button class='submit-btn' @click='goTaskDetail(task)'>进入任务</button>
                </div>
              </div>
            </div>

            <CommonPagination
              v-model:currentPage='openCurrentPage'
              v-model:pageSize='openPageSize'
              :total='openTasks.length'
              :page-size-options='[3, 6, 9]'
            />
          </div>

          <div v-if='currentMenu === "task-ended"' class='page-block'>
            <div class='page-header'>
              <h2>已结束的任务</h2>
            </div>

            <div class='task-grid three-columns'>
              <div
                v-for='task in pagedEndedTasks'
                :key='task.id'
                class='task-card'
              >
                <div class='task-image-box' @click='goTaskDetail(task)'>
                  <img :src='task.image' class='task-image' alt='任务图片' />
                </div>

                <div class='task-body'>
                  <div class='task-type-badge ended-badge'>{{ task.modeLabel }}</div>
                  <div class='task-title' @click='goTaskDetail(task)'>{{ task.title }}</div>
                  <div class='task-desc'>{{ task.desc }}</div>

                  <div class='task-meta'>
                    <div class='task-meta-item'>教师：{{ task.teacher }}</div>
                    <div class='task-meta-item'>截止时间：{{ task.deadline }}</div>
                  </div>

                  <button class='submit-btn ended-btn' @click='goTaskDetail(task)'>查看</button>
                </div>
              </div>
            </div>

            <CommonPagination
              v-model:currentPage='endedCurrentPage'
              v-model:pageSize='endedPageSize'
              :total='endedTasks.length'
              :page-size-options='[3, 6, 9]'
            />
          </div>
        </template>

        <template v-if='loggedOutView || !isLoggedIn'>
          <div class='page-block empty-home-block'>
            <div class='empty-home-card'>
              <h2>主页</h2>
              <p>当前未登录，请先登录后进入任务广场、提交历史和个人主页。</p>
            </div>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import tictactoeImage from '../assets/tictactoe.png'

export default {
  name: 'HomeView',
  components: {
    AppTopbar,
    StudentSidebar,
    CommonPagination
  },
  data () {
    return {
      isLoggedIn: true,
      loggedOutView: false,
      currentRole: 'student',
      userName: '张三',
      taskMenuOpen: true,
      currentMenu: 'task-open',
      openCurrentPage: 1,
      openPageSize: 3,
      endedCurrentPage: 1,
      endedPageSize: 3,
      openTasks: [
        {
          id: 1,
          title: '井字棋单人测评任务',
          desc: '学生提交单模型后，在预设环境下独立完成测评。',
          teacher: '王老师',
          deadline: '2026-07-10 23:59',
          image: tictactoeImage,
          taskMode: 'single',
          modeLabel: '单人模式'
        },
        {
          id: 2,
          title: '井字棋对战任务',
          desc: '学生提交模型后可参与真人对战；若教师配置了人机模型，也可选择人机对战。',
          teacher: '王老师',
          deadline: '2026-07-12 23:59',
          image: tictactoeImage,
          taskMode: 'battle',
          modeLabel: '对战模式',
          hasEasyBot: true,
          hasMediumBot: true,
          hasHardBot: false
        },
        {
          id: 3,
          title: '井字棋团队锦标赛任务',
          desc: '学生进入任务后可在任务详情页中创建队伍、加入队伍并查看当前淘汰赛安排。',
          teacher: '李老师',
          deadline: '2026-07-18 23:59',
          image: tictactoeImage,
          taskMode: 'tournament',
          modeLabel: '团队锦标赛'
        },
        {
          id: 4,
          title: '离散动作基础任务',
          desc: '用于课堂基础训练的单人模式任务。',
          teacher: '周老师',
          deadline: '2026-07-20 23:59',
          image: tictactoeImage,
          taskMode: 'single',
          modeLabel: '单人模式'
        },
        {
          id: 5,
          title: '课堂对战练习任务',
          desc: '学生在任务详情页提交后可进入对战流程。',
          teacher: '刘老师',
          deadline: '2026-07-22 23:59',
          image: tictactoeImage,
          taskMode: 'battle',
          modeLabel: '对战模式',
          hasEasyBot: true,
          hasMediumBot: false,
          hasHardBot: false
        },
        {
          id: 6,
          title: '阶段锦标赛任务',
          desc: '用于课堂展示的团队锦标赛任务。',
          teacher: '孙老师',
          deadline: '2026-07-25 23:59',
          image: tictactoeImage,
          taskMode: 'tournament',
          modeLabel: '团队锦标赛'
        }
      ],
      endedTasks: [
        {
          id: 7,
          title: '往期井字棋单人任务',
          desc: '历史单人测评任务。',
          teacher: '刘老师',
          deadline: '2026-06-10 23:59',
          image: tictactoeImage,
          taskMode: 'single',
          modeLabel: '单人模式'
        },
        {
          id: 8,
          title: '往期井字棋对战任务',
          desc: '历史对战模式任务。',
          teacher: '周老师',
          deadline: '2026-06-12 23:59',
          image: tictactoeImage,
          taskMode: 'battle',
          modeLabel: '对战模式',
          hasEasyBot: true,
          hasMediumBot: false,
          hasHardBot: false
        },
        {
          id: 9,
          title: '往期团队锦标赛任务',
          desc: '历史团队锦标赛任务。',
          teacher: '李老师',
          deadline: '2026-06-16 23:59',
          image: tictactoeImage,
          taskMode: 'tournament',
          modeLabel: '团队锦标赛'
        },
        {
          id: 10,
          title: '往期课堂测评任务',
          desc: '已结束的课堂测评任务。',
          teacher: '王老师',
          deadline: '2026-06-18 23:59',
          image: tictactoeImage,
          taskMode: 'single',
          modeLabel: '单人模式'
        }
      ]
    }
  },
  created () {
    this.syncAppState()
  },
  mounted () {
    window.addEventListener('storage', this.syncAppState)
    window.addEventListener('beforeunload', this.clearLoggedOutViewFlag)
  },
  beforeUnmount () {
    window.removeEventListener('storage', this.syncAppState)
    window.removeEventListener('beforeunload', this.clearLoggedOutViewFlag)
  },
  watch: {
    '$route.query': {
      handler () {
        this.syncAppState()
      },
      deep: true
    }
  },
  computed: {
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
    clearLoggedOutViewFlag () {
      sessionStorage.removeItem('mock_logged_out_view')
    },
    syncAppState () {
      const loggedOutFlag = sessionStorage.getItem('mock_logged_out_view')

      if (loggedOutFlag === 'true') {
        this.isLoggedIn = false
        this.loggedOutView = true
        this.currentRole = 'student'
        this.userName = ''
        this.taskMenuOpen = false
        this.currentMenu = ''
        return
      }

      const storedRole = localStorage.getItem('mock_login_role')
      const validRole = storedRole === 'teacher' ? 'teacher' : 'student'

      this.isLoggedIn = true
      this.loggedOutView = false
      this.currentRole = validRole
      this.userName = validRole === 'teacher' ? '王老师' : '张三'

      if (validRole === 'teacher') {
        this.$router.replace('/teacher/home')
        return
      }

      this.taskMenuOpen = true
      this.currentMenu = this.$route.query.tab === 'ended' ? 'task-ended' : 'task-open'
      this.openCurrentPage = 1
      this.endedCurrentPage = 1
    },
    goLogin () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.currentRole = 'student'
      this.userName = '张三'
      this.isLoggedIn = true
      this.loggedOutView = false
      this.taskMenuOpen = true
      this.currentMenu = 'task-open'
      this.$router.push('/login')
    },
    goRegister () {
      this.$router.push('/register')
    },
    handleUserClick () {
      if (this.currentRole === 'teacher') {
        this.goTeacherHome()
      } else {
        this.goProfile()
      }
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      const nextRole = this.currentRole === 'teacher' ? 'student' : 'teacher'
      localStorage.setItem('mock_login_role', nextRole)

      if (nextRole === 'teacher') {
        this.$router.push('/teacher/home')
      } else {
        this.$router.push({ path: '/', query: { tab: 'open' } })
      }
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    },
    toggleTaskMenu () {
      this.taskMenuOpen = !this.taskMenuOpen
      if (!this.taskMenuOpen) {
        return
      }
      if (this.currentMenu !== 'task-open' && this.currentMenu !== 'task-ended') {
        this.currentMenu = 'task-open'
        this.openCurrentPage = 1
        this.$router.push({ path: '/', query: { tab: 'open' } })
      }
    },
    selectOpenMenu () {
      this.taskMenuOpen = true
      this.currentMenu = 'task-open'
      this.openCurrentPage = 1
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    selectEndedMenu () {
      this.taskMenuOpen = true
      this.currentMenu = 'task-ended'
      this.endedCurrentPage = 1
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goHomeOpenTasks () {
      this.taskMenuOpen = true
      this.currentMenu = 'task-open'
      this.openCurrentPage = 1
      this.$router.push({ path: '/', query: { tab: 'open' } })
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
  margin-bottom: 8px;
  cursor: pointer;
}

.task-title:hover {
  color: #1f4e8c;
}

.task-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin-bottom: 14px;
  min-height: 72px;
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
  height: 40px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.submit-btn:hover {
  background: #173b69;
}

.ended-btn {
  background: #606266;
}

.ended-btn:hover {
  background: #4c4f53;
}

.empty-home-block {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 420px;
}

.empty-home-card {
  width: 100%;
  max-width: 560px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 36px;
  text-align: center;
}

.empty-home-card h2 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #1f2d3d;
}

.empty-home-card p {
  margin: 0;
  font-size: 14px;
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
