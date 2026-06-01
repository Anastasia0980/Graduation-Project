<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>强化学习智能体测评平台</h1>
        <p>请使用邮箱登录系统</p>
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

        <label class="remember-row">
          <input v-model="rememberPassword" type="checkbox">
          <span>记住密码</span>
        </label>

        <div class="action-row">
          <button class="primary-btn" :disabled="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登录' }}
          </button>
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
import { getApiBaseUrl } from '../utils/http'
import { ElMessage } from 'element-plus'

const API_BASE = getApiBaseUrl()
const REMEMBER_ACCOUNT_KEY = 'remember_login_account'
const REMEMBER_PASSWORD_KEY = 'remember_login_password'

export default {
  name: 'LoginView',
  data () {
    return {
      loading: false,
      rememberPassword: false,
      loginForm: {
        account: '',
        password: ''
      }
    }
  },
  created () {
    this.loadRememberedLogin()
  },
  methods: {
    async handleLogin () {
      if (!this.loginForm.account || !this.loginForm.password) {
        ElMessage.error('请填写完整登录信息')
        return
      }

      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
      this.loading = true
      try {
        const params = new URLSearchParams()
        params.append('email', this.loginForm.account)
        params.append('password', this.loginForm.password)

        const response = await fetch(`${API_BASE}/user/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
          },
          body: params.toString()
        })

        const result = await response.json()

        if (!response.ok || result.code !== 0 || !result.data) {
          ElMessage.error(result.message || '登录失败')
          return
        }

        const token = result.data
        localStorage.setItem('auth_token', token)

        const userResponse = await fetch(`${API_BASE}/user/userInfo`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })

        const userResult = await userResponse.json()

        if (!userResponse.ok || userResult.code !== 0 || !userResult.data) {
          ElMessage.error(userResult.message || '获取用户信息失败')
          localStorage.removeItem('auth_token')
          return
        }

        const userInfo = userResult.data
        localStorage.setItem('auth_name', userInfo.username || '')
        localStorage.setItem('auth_email', userInfo.email || '')
        localStorage.setItem('auth_role', userInfo.role || '')
        this.saveRememberedLogin()

        if (userInfo.role === 'ADMIN') {
          this.$router.push('/admin/home')
        } else if (userInfo.role === 'TEACHER') {
          this.$router.push('/teacher/home')
        } else {
          this.$router.push('/')
        }
      } catch (error) {
        ElMessage.error('登录请求失败，请检查后端是否已启动')
      } finally {
        this.loading = false
      }
    },
    loadRememberedLogin () {
      const account = localStorage.getItem(REMEMBER_ACCOUNT_KEY)
      const password = localStorage.getItem(REMEMBER_PASSWORD_KEY)
      if (account && password) {
        this.loginForm.account = account
        this.loginForm.password = password
        this.rememberPassword = true
      }
    },
    saveRememberedLogin () {
      if (this.rememberPassword) {
        localStorage.setItem(REMEMBER_ACCOUNT_KEY, this.loginForm.account)
        localStorage.setItem(REMEMBER_PASSWORD_KEY, this.loginForm.password)
        return
      }

      localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
      localStorage.removeItem(REMEMBER_PASSWORD_KEY)
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

.remember-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: -6px 0 14px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.remember-row input {
  width: 14px;
  height: 14px;
  cursor: pointer;
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

.primary-btn:disabled {
  background: #90a4c3;
  cursor: not-allowed;
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
