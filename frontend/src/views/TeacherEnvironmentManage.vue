<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='teacherName'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTaskHall'
      @user-click='goTaskHall'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar active-menu='environment-manage' />

      <main class='content-area'>
        <div class='page-header'>
          <h2>环境管理</h2>
        </div>

        <div class='card'>
          <div class='card-title'>环境列表</div>
          <div class='empty-box'>当前页面作为环境管理入口保留，具体上传与管理逻辑后续接入。</div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'

export default {
  name: 'TeacherEnvironmentManageView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师'
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    }
  },
  methods: {
    goTaskHall () {
      this.$router.push('/teacher/hall')
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

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1f2d3d;
}

.empty-box {
  min-height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background: #fafafa;
  text-align: center;
  padding: 20px;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }
}
</style>
