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

            <div class='task-grid'>
              <div
                v-for='task in openTasks'
                :key='task.id'
                class='task-card'
              >
                <div class='task-image-box' @click='goTaskDetail(task)'>
                  <img :src='task.image' class='task-image' alt='任务图片' />
                </div>

                <div class='task-body'>
                  <div class='task-title' @click='goTaskDetail(task)'>{{ task.title }}</div>
                  <div class='task-desc'>{{ task.desc }}</div>

                  <div class='task-meta'>
                    <div class='task-meta-item'>教师：{{ task.teacher }}</div>
                    <div class='task-meta-item'>截止时间：{{ task.deadline }}</div>
                  </div>

                  <button class='submit-btn' @click='goTaskDetail(task)'>提交</button>
                </div>
              </div>
            </div>
          </div>

          <div v-if='currentMenu === "task-ended"' class='page-block'>
            <div class='page-header'>
              <h2>已结束的任务</h2>
            </div>

            <div class='task-grid'>
              <div
                v-for='task in endedTasks'
                :key='task.id'
                class='task-card'
              >
                <div class='task-image-box' @click='goTaskDetail(task)'>
                  <img :src='task.image' class='task-image' alt='任务图片' />
                </div>

                <div class='task-body'>
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
import tictactoeImage from '../assets/tictactoe.png'

export default {
  name: 'HomeView',
  components: {
    AppTopbar,
    StudentSidebar
  },
  data () {
    return {
      isLoggedIn: true,
      loggedOutView: false,
      currentRole: 'student',
      userName: '张三',
      taskMenuOpen: true,
      currentMenu: 'task-open',
      openTasks: [
        {
          id: 1,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '王老师',
          deadline: '2026-07-10 23:59',
          image: tictactoeImage
        },
        {
          id: 2,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '李老师',
          deadline: '2026-07-12 23:59',
          image: tictactoeImage
        },
        {
          id: 3,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '赵老师',
          deadline: '2026-07-15 23:59',
          image: tictactoeImage
        },
        {
          id: 4,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '陈老师',
          deadline: '2026-07-18 23:59',
          image: tictactoeImage
        }
      ],
      endedTasks: [
        {
          id: 5,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '刘老师',
          deadline: '2026-06-10 23:59',
          image: tictactoeImage
        },
        {
          id: 6,
          title: '井字棋对战游戏',
          desc: '基于井字棋环境完成智能体对战测评任务。',
          teacher: '周老师',
          deadline: '2026-06-12 23:59',
          image: tictactoeImage
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
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.isLoggedIn = false
      this.loggedOutView = true
      this.taskMenuOpen = false
      this.currentMenu = ''
      this.userName = ''
      this.$router.push('/')
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
    goHomeOpenTasks () {
      if (this.loggedOutView || !this.isLoggedIn) {
        this.$router.push('/')
        return
      }

      this.taskMenuOpen = true
      this.currentMenu = 'task-open'
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    toggleTaskMenu () {
      this.taskMenuOpen = !this.taskMenuOpen
      if (this.taskMenuOpen && this.currentMenu !== 'task-open' && this.currentMenu !== 'task-ended') {
        this.currentMenu = 'task-open'
      }
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
    goTaskDetail (task) {
      this.$router.push({
        path: '/task-detail',
        query: {
          taskId: task.id,
          title: task.title
        }
      })
    },
    handleUserClick () {
      this.goProfile()
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    goHistory () {
      this.$router.push('/student/history')
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
  background: #f5f7fa;
  padding: 20px;
}

.page-block {
  min-height: calc(100vh - 104px);
}

.empty-home-block {
  min-height: calc(100vh - 104px);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}

.empty-home-card {
  width: 100%;
  min-height: 220px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 24px;
}

.empty-home-card h2 {
  margin: 0 0 14px;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.empty-home-card p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.task-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.task-image-box {
  width: 100%;
  height: 220px;
  background: #f2f6fc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.task-body {
  padding: 16px;
}

.task-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
  line-height: 1.4;
  cursor: pointer;
  margin-bottom: 10px;
}

.task-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  min-height: 48px;
  margin-bottom: 16px;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.task-meta-item {
  font-size: 14px;
  color: #606266;
}

.submit-btn {
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

.submit-btn:hover {
  background: #173b69;
}

.ended-btn {
  background: #606266;
}

.ended-btn:hover {
  background: #4d4f53;
}

@media (max-width: 1400px) {
  .task-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .task-grid {
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
}

@media (max-width: 700px) {
  .task-grid {
    grid-template-columns: 1fr;
  }
}
</style>
