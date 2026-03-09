<template>
  <div class='page'>
    <div class='auth-container'>
      <div class='platform-title'>强化学习智能体测评平台</div>

      <div class='auth-card'>
        <div class='card-header'>
          <h1>学生注册</h1>
          <p>请填写学生信息完成注册</p>
        </div>

        <div class='form-item'>
          <label>学号</label>
          <input
            v-model='registerForm.studentId'
            placeholder='请输入学号'
          />
        </div>

        <div class='form-item'>
          <label>班级</label>
          <select v-model='registerForm.className'>
            <option disabled value=''>请选择班级</option>
            <option value='计科2201'>计科2201</option>
            <option value='计科2202'>计科2202</option>
            <option value='软工2201'>软工2201</option>
            <option value='软工2202'>软工2202</option>
          </select>
        </div>

        <div class='form-item'>
          <label>姓名</label>
          <input
            v-model='registerForm.name'
            placeholder='请输入姓名'
          />
        </div>

        <div class='form-item'>
          <label>密码</label>
          <input
            v-model='registerForm.password'
            type='password'
            placeholder='请输入密码'
          />
        </div>

        <div class='form-item'>
          <label>确认密码</label>
          <input
            v-model='registerForm.confirmPassword'
            type='password'
            placeholder='请再次输入密码'
          />
        </div>

        <button class='primary-btn' @click='handleRegister'>
          注册
        </button>

        <div class='bottom-text'>
          已有账号？
          <span class='link-text' @click='goLogin'>返回登录</span>
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
  name: 'RegisterView',
  data () {
    return {
      message: '',
      messageType: 'success',
      registerForm: {
        studentId: '',
        className: '',
        name: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  methods: {
    handleRegister () {
      if (!this.registerForm.studentId) {
        this.showMessage('请输入学号', 'error')
        return
      }

      if (!this.registerForm.className) {
        this.showMessage('请选择班级', 'error')
        return
      }

      if (!this.registerForm.name) {
        this.showMessage('请输入姓名', 'error')
        return
      }

      if (!this.registerForm.password || !this.registerForm.confirmPassword) {
        this.showMessage('请输入密码并确认密码', 'error')
        return
      }

      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        this.showMessage('两次输入的密码不一致', 'error')
        return
      }

      this.showMessage('注册表单校验通过', 'success')
    },
    goLogin () {
      this.$router.push('/login')
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

.form-item input,
.form-item select {
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

.form-item input:focus,
.form-item select:focus {
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
