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
      <TeacherSidebar active-menu="teacher-home" />

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
                <div class="summary-value">{{ summary.publishedTaskCount }}</div>
                <div class="summary-label">已发布任务</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">{{ summary.totalSubmissionCount }}</div>
                <div class="summary-label">学生提交次数</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">{{ summary.classCount }}</div>
                <div class="summary-label">班级数量</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">{{ summary.pendingTaskCount }}</div>
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
                <tr v-if="recentLoading">
                  <td colspan="4" class="table-empty-cell">数据加载中...</td>
                </tr>
                <tr v-else-if="pagedRecentTasks.length === 0">
                  <td colspan="4" class="table-empty-cell">当前暂无任务</td>
                </tr>
                <tr v-else v-for="item in pagedRecentTasks" :key="item.assignmentId">
                  <td>{{ item.taskName }}</td>
                  <td>{{ item.deadline }}</td>
                  <td>{{ item.submittedCount }}</td>
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

function normalizeFileUrl (url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  if (url.startsWith('/')) return `${API_BASE}${url}`
  return `${API_BASE}/${url}`
}

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
      selectedAvatarFile: null,
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
      recentLoading: false,
      recentTasks: [],
      summary: {
        publishedTaskCount: 0,
        totalSubmissionCount: 0,
        classCount: 0,
        pendingTaskCount: 0
      }
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
    this.loadDashboardData()
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
        this.profile.avatar = userInfo.userPic ? normalizeFileUrl(userInfo.userPic) : defaultAvatar
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
    async loadDashboardData () {
      this.recentLoading = true
      try {
        const [overviewRes, classRes] = await Promise.all([
          fetch(`${API_BASE}/teacher/assignments/overview`, {
            method: 'GET',
            headers: this.getAuthHeaders()
          }),
          fetch(`${API_BASE}/class?pageNum=0&pageSize=500`, {
            method: 'GET',
            headers: this.getAuthHeaders()
          })
        ])

        const overviewResult = await overviewRes.json()
        const classResult = await classRes.json()

        const overviewList =
          overviewRes.ok && overviewResult.code === 0 && Array.isArray(overviewResult.data)
            ? overviewResult.data
            : []

        const classContent =
          classRes.ok && classResult.code === 0 && classResult.data && Array.isArray(classResult.data.content)
            ? classResult.data.content
            : []

        this.recentTasks = overviewList
          .map(item => ({
            assignmentId: item.assignmentId,
            taskName: item.taskName || '未知任务',
            deadline: this.formatDateTime(item.deadline) || '--',
            submittedCount: item.submittedCount || 0,
            status: item.status || '--',
            sortTime: item.deadline ? new Date(item.deadline).getTime() : 0
          }))
          .sort((a, b) => b.sortTime - a.sortTime)

        this.summary = {
          publishedTaskCount: overviewList.length,
          totalSubmissionCount: overviewList.reduce((sum, item) => sum + (item.totalSubmissionCount || 0), 0),
          classCount: classContent.length,
          pendingTaskCount: overviewList.filter(item => item.status === '进行中').length
        }
      } catch (error) {
        this.recentTasks = []
        this.summary = {
          publishedTaskCount: 0,
          totalSubmissionCount: 0,
          classCount: 0,
          pendingTaskCount: 0
        }
      } finally {
        this.recentLoading = false
      }
    },
    openEditDialog () {
      this.editForm = {
        avatar: this.profile.avatar,
        name: this.profile.name,
        email: this.profile.email
      }
      this.selectedAvatarFile = null
      this.showEditDialog = true
    },
    closeEditDialog () {
      this.showEditDialog = false
      this.showDeleteDialog = false
      this.selectedAvatarFile = null
    },
    handleAvatarChange (event) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      this.selectedAvatarFile = file
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

        if (this.selectedAvatarFile) {
          const avatarFormData = new FormData()
          avatarFormData.append('file', this.selectedAvatarFile)
          const avatarResponse = await fetch(`${API_BASE}/user/avatar`, {
            method: 'POST',
            headers: this.getAuthHeaders(),
            body: avatarFormData
          })
          const avatarResult = await avatarResponse.json()
          if (!avatarResponse.ok || avatarResult.code !== 0) {
            this.resultMessage = avatarResult.message || '头像上传失败。'
            this.showResultDialog = true
            return
          }
          this.profile.avatar = avatarResult.data ? normalizeFileUrl(avatarResult.data) : this.editForm.avatar
        }

        this.profile.name = this.editForm.name
        this.profile.email = this.editForm.email
        localStorage.setItem('auth_name', this.profile.name)
        localStorage.setItem('auth_email', this.profile.email)
        await this.loadProfile()
        this.selectedAvatarFile = null
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
  margin-bottom: 18px;
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
  min-height: 28px;
}

.avatar-row {
  align-items: flex-start;
}

.label {
  width: 72px;
  color: #606266;
  font-size: 14px;
  flex-shrink: 0;
}

.value {
  font-size: 14px;
  color: #303133;
}

.avatar-box {
  width: 88px;
  height: 88px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-action {
  margin-top: 18px;
}

.edit-btn,
.primary-btn,
.secondary-btn,
.save-btn,
.danger-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.edit-btn,
.primary-btn {
  background: #1f4e8c;
  color: #ffffff;
}

.edit-btn:hover,
.primary-btn:hover {
  background: #173b69;
}

.secondary-btn {
  background: #ffffff;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.save-btn {
  background: #67c23a;
  color: #ffffff;
}

.save-btn:hover {
  background: #5daf34;
}

.danger-btn {
  background: #f56c6c;
  color: #ffffff;
}

.danger-btn:hover {
  background: #dd6161;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.summary-item {
  min-height: 96px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f4e8c;
  line-height: 1;
}

.summary-label {
  margin-top: 10px;
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

.table-empty-cell {
  text-align: center !important;
  color: #909399;
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
  width: 560px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  overflow: hidden;
}

.small-dialog {
  width: 420px;
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
  font-size: 14px;
  color: #303133;
}

.scrollable-dialog-body {
  max-height: 60vh;
  overflow-y: auto;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.form-item label {
  font-size: 14px;
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
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.dialog-avatar-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
}

.dialog-footer {
  padding: 0 20px 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.right-btn-footer {
  flex-wrap: wrap;
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

  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 720px;
  }
}
</style>
