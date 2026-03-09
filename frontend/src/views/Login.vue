<template>
  <div class='page'>
    <div class='auth-container'>
      <div class='platform-title'>强化学习智能体测评平台</div>

      <div class='auth-card'>
        <div class='card-header'>
          <h1>用户登录</h1>
          <p>请选择身份后输入账号和密码</p>
        </div>

        <div class='role-switch'>
          <button
            class='role-btn'
            :class='{ active: loginRole === "student" }'
            @click='switchRole("student")'
          >
            学生登录
          </button>
          <button
            class='role-btn'
            :class='{ active: loginRole === "teacher" }'
            @click='switchRole("teacher")'
          >
            教师登录
          </button>
        </div>

        <div class='form-item'>
          <label>{{ loginRole === 'student' ? '学号' : '教师号' }}</label>
          <input
            v-model='loginForm.account'
            :placeholder='loginRole === "student" ? "请输入学号" : "请输入教师号"'
          />
        </div>

        <div class='form-item'>
          <label>密码</label>
          <input
            v-model='loginForm.password'
            type='password'
            placeholder='请输入密码'
          />
        </div>

        <button class='primary-btn' @click='handleLogin'>
          登录
        </button>

        <div class='bottom-text'>
          还没有账号？
          <span class='link-text' @click='goRegister'>学生注册</span>
        </div>

        <div v-if='message' class='message-box' :class='messageType'>
          {{ message }}
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
      loginRole: 'student',
      message: '',
      messageType: 'success',
      loginForm: {
        account: '',
        password: ''
      }
    }
  },
  methods: {
    switchRole (role) {
      this.loginRole = role
      this.message = ''
      this.loginForm = {
        account: '',
        password: ''
      }
    },
    handleLogin () {
      if (!this.loginForm.account || !this.loginForm.password) {
        this.showMessage('请完整填写账号和密码', 'error')
        return
      }

      this.showMessage('登录表单校验通过', 'success')
    },
    goRegister () {
      this.$router.push('/register')
    },
    showMessage (text, type) {
      this.message = text
      this.messageType = type
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
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  padding: 24px;
}

.auth-container {
  width: 100%;
  max-width: 460px;
}

.platform-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 28px;
  letter-spacing: 1px;
}

.auth-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 32px 30px 28px;
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.06);
}

.card-header {
  margin-bottom: 24px;
  text-align: center;
}

.card-header h1 {
  margin: 0 0 10px;
  font-size: 24px;
  color: #1f2d3d;
}

.card-header p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.role-switch {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.role-btn {
  flex: 1;
  height: 40px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.role-btn.active {
  color: #1f4e8c;
  border-color: #1f4e8c;
  background: #edf3fb;
  font-weight: 600;
}

.form-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 18px;
}

.form-item label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.form-item input {
  width: 100%;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 12px;
  font-size: 14px;
  color: #303133;
  outline: none;
  background: #ffffff;
}

.form-item input:focus {
  border-color: #1f4e8c;
}

.primary-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 6px;
}

.primary-btn:hover {
  background: #173b69;
}

.bottom-text {
  margin-top: 18px;
  text-align: center;
  font-size: 14px;
  color: #606266;
}

.link-text {
  color: #1f4e8c;
  cursor: pointer;
  font-weight: 600;
}

.message-box {
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 14px;
}

.message-box.success {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.message-box.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}
</style>
