<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>强化学习智能体测评平台</h1>
        <p>请选择身份并使用邮箱登录系统</p>
      </div>

      <div class="role-tabs">
        <button
          class="role-tab"
          :class="{ active: activeRole === 'student' }"
          @click="activeRole = 'student'"
        >
          学生登录
        </button>
        <button
          class="role-tab"
          :class="{ active: activeRole === 'teacher' }"
          @click="activeRole = 'teacher'"
        >
          教师登录
        </button>
      </div>

      <div class="form-area">
        <div class="form-item">
          <label>邮箱</label>
          <input
            v-model="loginForm.account"
            type="text"
            placeholder="请输入邮箱"
          >
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
          >
        </div>

        <div class="action-row">
          <button class="primary-btn" @click="handleLogin">登录</button>
        </div>

        <div class="bottom-row">
          <span>还没有账号？</span>
          <span class="link-text" @click="goRegister">立即注册</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginView',
  data () {
    return {
      activeRole: 'student',
      loginForm: {
        account: '',
        password: ''
      }
    }
  },
  methods: {
    handleLogin () {
      if (!this.loginForm.account || !this.loginForm.password) {
        alert('请填写完整登录信息')
        return
      }

      if (this.activeRole === 'teacher') {
        localStorage.setItem('mock_login_role', 'teacher')
        this.$router.push('/teacher/home')
      } else {
        localStorage.setItem('mock_login_role', 'student')
        this.$router.push({ path: '/', query: { tab: 'open' } })
      }
    },
    goRegister () {
      this.$router.push('/register')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.login-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

.login-card {
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.08);
  overflow: hidden;
}

.login-header {
  padding: 28px 28px 20px;
  border-bottom: 1px solid #ebeef5;
}

.login-header h1 {
  margin: 0 0 10px;
  font-size: 24px;
  color: #1f2d3d;
}

.login-header p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.role-tabs {
  display: flex;
  padding: 16px 20px 0;
  gap: 12px;
}

.role-tab {
  flex: 1;
  height: 40px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.role-tab.active {
  color: #1f4e8c;
  border-color: #1f4e8c;
  background: #ecf5ff;
  font-weight: 600;
}

.form-area {
  padding: 20px 28px 28px;
}

.form-item {
  margin-bottom: 18px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.form-item input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
  outline: none;
}

.form-item input:focus {
  border-color: #1f4e8c;
}

.action-row {
  margin-top: 8px;
}

.primary-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 6px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn:hover {
  background: #173b69;
}

.bottom-row {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.link-text {
  margin-left: 6px;
  color: #1f4e8c;
  cursor: pointer;
}

.link-text:hover {
  text-decoration: underline;
}
</style>
