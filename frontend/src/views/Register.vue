<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h1>学生注册</h1>
        <p>请填写基础信息完成账号注册</p>
      </div>

      <div class="form-area">
        <div class="form-item">
          <label>姓名</label>
          <input
            v-model="registerForm.name"
            type="text"
            placeholder="请输入姓名"
          >
        </div>

        <div class="form-item">
          <label>邮箱</label>
          <input
            v-model="registerForm.email"
            type="text"
            placeholder="请输入邮箱"
          >
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
          >
        </div>

        <div class="form-item">
          <label>确认密码</label>
          <input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
          >
        </div>

        <div class="action-row">
          <button class="primary-btn" :disabled="loading" @click="handleRegister">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </div>

        <div class="bottom-row">
          <span>已有账号？</span>
          <span class="link-text" @click="goLogin">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'RegisterView',
  data () {
    return {
      loading: false,
      registerForm: {
        name: '',
        email: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  methods: {
    async handleRegister () {
      const { name, email, password, confirmPassword } = this.registerForm

      if (!name || !email || !password || !confirmPassword) {
        ElMessage.warning('请填写完整注册信息')
        return
      }

      if (password !== confirmPassword) {
        ElMessage.warning('两次输入的密码不一致')
        return
      }

      this.loading = true
      try {
        const params = new URLSearchParams()
        params.append('name', name)
        params.append('email', email)
        params.append('password', password)

        const response = await fetch(`${API_BASE}/user/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
          },
          body: params.toString()
        })

        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '注册失败')
          return
        }

        ElMessage.success('注册成功，请使用邮箱登录')
        this.$router.push('/login')
      } catch (error) {
        ElMessage.error('注册请求失败，请检查后端是否已启动')
      } finally {
        this.loading = false
      }
    },
    goLogin () {
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.register-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

.register-card {
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.08);
  overflow: hidden;
}

.register-header {
  padding: 28px 28px 20px;
  border-bottom: 1px solid #ebeef5;
}

.register-header h1 {
  margin: 0 0 10px;
  font-size: 24px;
  color: #1f2d3d;
}

.register-header p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.form-area {
  padding: 24px 28px 28px;
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
