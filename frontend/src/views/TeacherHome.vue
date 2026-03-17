<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      :user-name="profile.name"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @switch-role="switchRole"
      @logout="logout"
    />

    <div class="layout">
      <TeacherSidebar
        active-menu="teacher-home"
        @teacher-home-click="goTeacherHome"
        @publish-click="goPublishTask"
        @manage-click="goTaskManage"
        @class-data-click="goClassData"
        @export-click="goExportScore"
      />

      <main class="content-area">
        <div class="page-header">
          <h2>教师主页</h2>
        </div>

        <div class="profile-grid">
          <div class="card">
            <div class="card-title">基本信息</div>
            <div class="info-list">
              <div class="info-row avatar-row">
                <span class="label">头像</span>
                <div class="avatar-box">
                  <img :src="profile.avatar" alt="头像" class="avatar-image">
                </div>
              </div>

              <div class="info-row">
                <span class="label">姓名</span>
                <span class="value">{{ profile.name }}</span>
              </div>

              <div class="info-row">
                <span class="label">Email</span>
                <span class="value">{{ profile.email }}</span>
              </div>
            </div>

            <div class="profile-action">
              <button class="edit-btn" type="button" @click="openEditDialog">修改</button>
            </div>
          </div>

          <div class="card">
            <div class="card-title">任务总览</div>
            <div class="summary-grid">
              <div class="summary-item">
                <div class="summary-value">6</div>
                <div class="summary-label">已发布任务</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">128</div>
                <div class="summary-label">学生提交次数</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">4</div>
                <div class="summary-label">班级数量</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">3</div>
                <div class="summary-label">待处理任务</div>
              </div>
            </div>
          </div>

          <div class="card full-width">
            <div class="card-title">近期任务概览</div>
            <table class="common-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>截止时间</th>
                  <th>提交人数</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedRecentTasks" :key="item.id">
                  <td>{{ item.name }}</td>
                  <td>{{ item.deadline }}</td>
                  <td>{{ item.submitCount }}</td>
                  <td>{{ item.status }}</td>
                </tr>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage="taskPage"
              v-model:pageSize="taskPageSize"
              :total="recentTasks.length"
              :page-size-options="[5, 10, 20]"
            />
          </div>
        </div>
      </main>
    </div>

    <div v-if="showEditDialog" class="dialog-mask" @click="closeEditDialog">
      <div class="dialog-box" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">修改个人信息</div>
          <button class="close-btn" @click="closeEditDialog">关闭</button>
        </div>

        <div class="dialog-body scrollable-dialog-body">
          <div class="form-item">
            <label>头像</label>
            <div class="avatar-upload-row">
              <img :src="editForm.avatar" alt="头像" class="dialog-avatar-image">
              <input type="file" accept="image/*" @change="handleAvatarChange">
            </div>
          </div>

          <div class="form-item">
            <label>姓名</label>
            <input v-model="editForm.name" type="text" placeholder="请输入姓名">
          </div>

          <div class="form-item">
            <label>Email</label>
            <input v-model="editForm.email" type="text" placeholder="请输入邮箱">
          </div>

          <div class="form-item disabled-item">
            <label>角色</label>
            <input :value="profile.role" type="text" disabled>
          </div>

          <div class="form-item disabled-item">
            <label>创建时间</label>
            <input :value="profile.createTime" type="text" disabled>
          </div>

          <div class="form-item disabled-item">
            <label>最近更新时间</label>
            <input :value="profile.updateTime" type="text" disabled>
          </div>
        </div>

        <div class="dialog-footer right-btn-footer">
          <button class="danger-btn" @click="showDeleteDialog = true">注销</button>
          <button class="primary-btn" @click="showPwdDialog = true">修改密码</button>
          <button class="save-btn" @click="saveProfileChanges">保存</button>
          <button class="secondary-btn" @click="closeEditDialog">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteDialog" class="dialog-mask" @click="showDeleteDialog = false">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">注销确认</div>
          <button class="close-btn" @click="showDeleteDialog = false">关闭</button>
        </div>

        <div class="dialog-body">
          确定要注销当前账号吗？
        </div>

        <div class="dialog-footer">
          <button class="secondary-btn" @click="showDeleteDialog = false">否</button>
          <button class="danger-btn" @click="confirmDelete">是</button>
        </div>
      </div>
    </div>

    <div v-if="showPwdDialog" class="dialog-mask" @click="closePwdDialog">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">修改密码</div>
          <button class="close-btn" @click="closePwdDialog">关闭</button>
        </div>

        <div class="dialog-body">
          <div class="form-item">
            <label>请输入当前密码</label>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码">
          </div>

          <div class="form-item">
            <label>请输入新密码</label>
            <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码">
          </div>

          <div class="form-item">
            <label>请再次输入新密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码">
          </div>
        </div>

        <div class="dialog-footer">
          <button class="primary-btn" @click="confirmChangePassword">确定</button>
          <button class="secondary-btn" @click="closePwdDialog">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showResultDialog" class="dialog-mask" @click="showResultDialog = false">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">提示</div>
          <button class="close-btn" @click="showResultDialog = false">关闭</button>
        </div>

        <div class="dialog-body">
          {{ resultMessage }}
        </div>

        <div class="dialog-footer">
          <button class="primary-btn" @click="showResultDialog = false">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import defaultAvatar from '../assets/logo.png'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherHomeView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      profile: {
        avatar: defaultAvatar,
        name: '教师',
        email: '',
        role: '教师',
        createTime: '',
        updateTime: ''
      },
      editForm: {
        avatar: defaultAvatar,
        name: '',
        email: ''
      },
      showEditDialog: false,
      showDeleteDialog: false,
      showPwdDialog: false,
      showResultDialog: false,
      resultMessage: '',
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      taskPage: 1,
      taskPageSize: 5,
      recentTasks: [
        { id: 1, name: '井字棋对战游戏', deadline: '2026-07-10 23:59', submitCount: 36, status: '进行中' },
        { id: 2, name: '井字棋对战游戏', deadline: '2026-07-15 23:59', submitCount: 28, status: '进行中' },
        { id: 3, name: '井字棋对战游戏', deadline: '2026-06-18 23:59', submitCount: 42, status: '已结束' }
      ]
    }
  },
  computed: {
    pagedRecentTasks () {
      const start = (this.taskPage - 1) * this.taskPageSize
      const end = start + this.taskPageSize
      return this.recentTasks.slice(start, end)
    }
  },
  created () {
    this.loadProfile()
  },
  methods: {
    getAuthHeaders (withJson = false) {
      const token = localStorage.getItem('auth_token') || ''
      const headers = {
        Authorization: `Bearer ${token}`
      }
      if (withJson) {
        headers['Content-Type'] = 'application/json'
      }
      return headers
    },
    formatRole (role) {
      if (role === 'ADMIN') return '管理员'
      if (role === 'STUDENT') return '学生'
      return '教师'
    },
    formatDateTime (value) {
      if (!value) return ''
      return String(value).replace('T', ' ').slice(0, 19)
    },
    async loadProfile () {
      try {
        const response = await fetch(`${API_BASE}/user/userInfo`, {
          method: 'GET',
          headers: this.getAuthHeaders()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0 || !result.data) {
          return
        }

        const userInfo = result.data
        this.profile.avatar = userInfo.userPic || defaultAvatar
        this.profile.name = userInfo.username || '教师'
        this.profile.email = userInfo.email || ''
        this.profile.role = this.formatRole(userInfo.role)
        this.profile.createTime = this.formatDateTime(userInfo.createTime)
        this.profile.updateTime = this.formatDateTime(userInfo.updateTime)

        localStorage.setItem('auth_name', this.profile.name)
        localStorage.setItem('auth_email', this.profile.email)
      } catch (error) {
      }
    },
    openEditDialog () {
      this.editForm = {
        avatar: this.profile.avatar,
        name: this.profile.name,
        email: this.profile.email
      }
      this.showEditDialog = true
    },
    closeEditDialog () {
      this.showEditDialog = false
      this.showDeleteDialog = false
    },
    handleAvatarChange (event) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      this.editForm.avatar = URL.createObjectURL(file)
    },
    async saveProfileChanges () {
      if (!this.editForm.name || !this.editForm.email) {
        this.resultMessage = '请填写完整的姓名和邮箱。'
        this.showResultDialog = true
        return
      }

      try {
        const response = await fetch(`${API_BASE}/user/update`, {
          method: 'PUT',
          headers: this.getAuthHeaders(true),
          body: JSON.stringify({
            username: this.editForm.name,
            email: this.editForm.email
          })
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          this.resultMessage = result.message || '修改失败。'
          this.showResultDialog = true
          return
        }

        this.profile.name = this.editForm.name
        this.profile.email = this.editForm.email
        localStorage.setItem('auth_name', this.profile.name)
        localStorage.setItem('auth_email', this.profile.email)
        await this.loadProfile()
        this.showEditDialog = false
        this.resultMessage = '个人信息修改完成。'
        this.showResultDialog = true
      } catch (error) {
        this.resultMessage = '个人信息修改请求失败，请检查后端服务。'
        this.showResultDialog = true
      }
    },
    closePwdDialog () {
      this.showPwdDialog = false
      this.passwordForm = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    },
    confirmDelete () {
      this.showDeleteDialog = false
      this.resultMessage = '当前后端暂未提供账号注销接口，暂不能完成注销操作。'
      this.showResultDialog = true
    },
    async confirmChangePassword () {
      if (!this.passwordForm.oldPassword || !this.passwordForm.newPassword || !this.passwordForm.confirmPassword) {
        this.resultMessage = '请填写完整的密码信息。'
        this.showResultDialog = true
        return
      }

      try {
        const response = await fetch(`${API_BASE}/user/updatePwd`, {
          method: 'PATCH',
          headers: this.getAuthHeaders(true),
          body: JSON.stringify({
            oldPwd: this.passwordForm.oldPassword,
            newPwd: this.passwordForm.newPassword,
            rePwd: this.passwordForm.confirmPassword
          })
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          this.resultMessage = result.message || '密码修改失败。'
          this.showResultDialog = true
          return
        }

        this.closePwdDialog()
        this.resultMessage = '密码修改完成。'
        this.showResultDialog = true
      } catch (error) {
        this.resultMessage = '密码修改请求失败，请检查后端服务。'
        this.showResultDialog = true
      }
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
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
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

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.full-width {
  grid-column: 1 / -1;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1f2d3d;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.avatar-row {
  align-items: center;
}

.label {
  font-size: 14px;
  color: #606266;
}

.value {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.avatar-box {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #ffffff;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-action {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.edit-btn {
  height: 38px;
  min-width: 92px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.edit-btn:hover {
  background: #173b69;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.summary-item {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f4e8c;
  margin-bottom: 6px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.common-table {
  width: 100%;
  border-collapse: collapse;
}

.common-table th,
.common-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  font-size: 14px;
}

.common-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(31, 45, 61, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 20px;
}

.dialog-box {
  width: 520px;
  max-width: 100%;
  max-height: calc(100vh - 40px);
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.small-dialog {
  width: 460px;
}

.dialog-header {
  min-height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.close-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.dialog-body {
  padding: 20px;
}

.scrollable-dialog-body {
  overflow-y: auto;
  max-height: calc(100vh - 220px);
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.disabled-item input {
  background: #f5f7fa;
  color: #909399;
  cursor: not-allowed;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.dialog-avatar-image {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #dcdfe6;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.right-btn-footer {
  justify-content: flex-end;
}

.primary-btn {
  height: 38px;
  min-width: 96px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn:hover {
  background: #173b69;
}

.save-btn {
  height: 38px;
  min-width: 96px;
  border: none;
  border-radius: 4px;
  background: #3f8f5f;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.save-btn:hover {
  background: #32724c;
}

.secondary-btn {
  height: 38px;
  min-width: 96px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.danger-btn {
  height: 38px;
  min-width: 96px;
  border: none;
  border-radius: 4px;
  background: #d9534f;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.danger-btn:hover {
  background: #c9302c;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: auto;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 640px;
  }
}
</style>
