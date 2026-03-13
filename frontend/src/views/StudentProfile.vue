<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      user-name="张三"
      current-role="student"
      active-nav="home"
      @platform-click="goHomeOpenTasks"
      @user-click="goProfile"
      @switch-role="switchRole"
      @logout="logout"
    />

    <div class="layout">
      <StudentSidebar
        :logged-in="true"
        active-menu="profile"
        :task-menu-open="false"
        @profile-click="goProfile"
        @class-click="goStudentClass"
        @toggle-task-menu="goHomeOpenTasks"
        @open-task-click="goHomeOpenTasks"
        @ended-task-click="goHomeEndedTasks"
        @history-click="goHistory"
      />

      <main class="content-area">
        <div class="page-header">
          <h2>个人主页</h2>
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
            <div class="card-title">任务概览</div>
            <div class="summary-grid">
              <div class="summary-item">
                <div class="summary-value">4</div>
                <div class="summary-label">开放任务</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">12</div>
                <div class="summary-label">历史提交</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">7</div>
                <div class="summary-label">已完成测评</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">3</div>
                <div class="summary-label">获胜次数</div>
              </div>
            </div>
          </div>

          <div class="card full-width">
            <div class="card-title">近期记录</div>
            <table class="record-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>提交时间</th>
                  <th>状态</th>
                  <th>结果</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedRecentRecords" :key="item.id">
                  <td>{{ item.taskName }}</td>
                  <td>{{ item.submitTime }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.result }}</td>
                </tr>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage="recordPage"
              v-model:pageSize="recordPageSize"
              :total="recentRecords.length"
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
            <label>班级</label>
            <input :value="profile.className" type="text" disabled>
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
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import defaultAvatar from '../assets/logo.png'

export default {
  name: 'StudentProfileView',
  components: {
    AppTopbar,
    StudentSidebar,
    CommonPagination
  },
  data () {
    return {
      profile: {
        avatar: defaultAvatar,
        name: '张三',
        email: 'zhangsan@bjtu.edu.cn',
        className: '人工智能 2201',
        role: '学生',
        createTime: '2026-03-01 10:25:18',
        updateTime: '2026-03-12 14:08:33'
      },
      editForm: {
        avatar: defaultAvatar,
        name: '',
        email: ''
      },
      showEditDialog: false,
      showDeleteDialog: false,
      showPwdDialog: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      recordPage: 1,
      recordPageSize: 10,
      recentRecords: [
        {
          id: 1,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-05 14:10',
          status: '测评中',
          result: '-'
        },
        {
          id: 2,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-03 18:42',
          status: '已完成',
          result: '失败'
        },
        {
          id: 3,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-01 20:15',
          status: '已完成',
          result: '获胜'
        }
      ]
    }
  },
  computed: {
    pagedRecentRecords () {
      const start = (this.recordPage - 1) * this.recordPageSize
      const end = start + this.recordPageSize
      return this.recentRecords.slice(start, end)
    }
  },
  methods: {
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
    },
    confirmChangePassword () {
      // 逻辑暂不写
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goHomeEndedTasks () {
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    goStudentClass () {
      this.$router.push('/student/class')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
    },
    logout () {
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
  color: #1f2d3d;
  margin-bottom: 16px;
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

.record-table {
  width: 100%;
  border-collapse: collapse;
}

.record-table th,
.record-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  font-size: 14px;
}

.record-table th {
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
}

@media (max-width: 700px) {
  .record-table {
    min-width: 640px;
  }

  .card {
    overflow-x: auto;
  }
}
</style>
