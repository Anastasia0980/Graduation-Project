<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='displayUserName'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <StudentSidebar
        :logged-in='isLoggedIn'
        active-menu='resource'
        :task-menu-open='false'
        @profile-click='goProfile'
        @class-click='goStudentClass'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>环境下载</h2>
        </div>

        <div class='table-card'>
          <table class='resource-table'>
            <thead>
              <tr>
                <th>环境名称</th>
                <th>发布时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if='pagedResourceList.length === 0'>
                <td colspan='3' class='empty-cell'>当前暂无可下载环境</td>
              </tr>
              <tr v-else v-for='item in pagedResourceList' :key='item.id'>
                <td>{{ item.envName }}</td>
                <td>
                  <span class='publish-time'>{{ item.publishTime }}</span>
                </td>
                <td>
                  <button class='table-btn' @click='openDownloadDialog(item)'>下载</button>
                </td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-model:currentPage='currentPage'
            v-model:pageSize='pageSize'
            :total='resourceList.length'
            :page-size-options='[5, 10, 20]'
          />
        </div>
      </main>
    </div>

    <div v-if='dialogVisible' class='dialog-mask' @click='closeDialog'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>{{ currentResource.envName }}</div>
          <button class='close-btn' @click='closeDialog'>关闭</button>
        </div>

        <div class='dialog-body'>
          <div class='resource-item'>
            <div class='resource-main'>
              <div class='resource-name'>环境依赖（requirements.txt）</div>
            </div>
            <button class='download-btn' @click='handleDownload("requirements")'>下载</button>
          </div>

          <div class='resource-item'>
            <div class='resource-main'>
              <div class='resource-name'>算法模板文件（template.py）</div>
            </div>
            <button class='download-btn' @click='handleDownload("template")'>下载</button>
          </div>

          <div class='resource-item'>
            <div class='resource-main'>
              <div class='resource-name'>简单 AI</div>
              <div class='resource-sub'>包含 model.pt、config.json</div>
            </div>
            <button class='download-btn' @click='handleDownload("easy")'>下载</button>
          </div>

          <div class='resource-item'>
            <div class='resource-main'>
              <div class='resource-name'>中等 AI</div>
              <div class='resource-sub'>包含 model.pt、config.json</div>
            </div>
            <button class='download-btn' @click='handleDownload("medium")'>下载</button>
          </div>

          <div class='resource-item'>
            <div class='resource-main'>
              <div class='resource-name'>困难 AI</div>
              <div class='resource-sub'>包含 model.pt、config.json</div>
            </div>
            <button class='download-btn' @click='handleDownload("hard")'>下载</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'

export default {
  name: 'StudentResourceDownloadView',
  components: {
    AppTopbar,
    StudentSidebar,
    CommonPagination
  },
  data () {
    return {
      currentPage: 1,
      pageSize: 5,
      dialogVisible: false,
      currentResource: {
        id: null,
        envName: ''
      },
      resourceList: [
        {
          id: 1,
          envName: '井字棋对战环境（tictactoe_v3）',
          publishTime: '2026-03-30 10:00'
        }
      ]
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    displayUserName () {
      return localStorage.getItem('auth_name') || '学生'
    },
    pagedResourceList () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.resourceList.slice(start, end)
    }
  },
  methods: {
    openDownloadDialog (item) {
      this.currentResource = { ...item }
      this.dialogVisible = true
    },
    closeDialog () {
      this.dialogVisible = false
    },
    handleDownload () {
      ElMessage.info('下载功能后续接入')
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goHomeEndedTasks () {
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    goStudentClass () {
      this.$router.push('/student/class')
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
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

.table-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.resource-table {
  width: 100%;
  border-collapse: collapse;
}

.resource-table th,
.resource-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  font-size: 14px;
  color: #303133;
}

.resource-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 600;
}

.publish-time {
  font-size: 13px;
  color: #909399;
}

.empty-cell {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}

.table-btn,
.download-btn,
.close-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 4px;
  border: 1px solid #1f4e8c;
  background: #1f4e8c;
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
}

.table-btn:hover,
.download-btn:hover,
.close-btn:hover {
  opacity: 0.92;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 3000;
}

.dialog-box {
  width: 560px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.16);
}

.dialog-header {
  padding: 16px 20px;
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

.dialog-body {
  padding: 20px;
}

.resource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}

.resource-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.resource-main {
  min-width: 0;
}

.resource-name {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.resource-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .resource-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
