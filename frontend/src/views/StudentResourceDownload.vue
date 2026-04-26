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
          <div v-if='loading' class='empty-cell loading-box'>正在加载可下载内容...</div>
          <table v-else class='resource-table'>
            <thead>
              <tr>
                <th>名称</th>
                <th>内容说明</th>
                <th>发布时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if='pagedResourceList.length === 0'>
                <td colspan='4' class='empty-cell'>当前暂无可下载内容</td>
              </tr>
              <tr v-else v-for='item in pagedResourceList' :key='`${item.itemType}_${item.id}`'>
                <td>{{ item.name }}</td>
                <td>
                  <span class='resource-subtitle'>{{ item.subtitle || '--' }}</span>
                </td>
                <td>
                  <span class='publish-time'>{{ formatDateTime(item.createTime) }}</span>
                </td>
                <td>
                  <button class='table-btn' @click='downloadItem(item)'>下载</button>
                </td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-if='!loading'
            v-model:currentPage='currentPage'
            v-model:pageSize='pageSize'
            :total='resourceList.length'
            :page-size-options='[5, 10, 20]'
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'

const API_BASE = (process.env.VUE_APP_API_BASE && process.env.VUE_APP_API_BASE.trim()) ||
  (typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : 'http://localhost:8080')

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
      loading: false,
      resourceList: []
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
  created () {
    this.loadResourceList()
  },
  methods: {
    tokenHeader () {
      const token = localStorage.getItem('auth_token')
      return token ? { Authorization: `Bearer ${token}` } : {}
    },
    async loadResourceList () {
      this.loading = true
      try {
        const response = await fetch(`${API_BASE}/battle-environments/student-downloads`, {
          method: 'GET',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '可下载内容加载失败')
        }
        this.resourceList = Array.isArray(result.data) ? result.data : []
      } catch (error) {
        this.resourceList = []
        ElMessage.error(error.message || '可下载内容加载失败')
      } finally {
        this.loading = false
      }
    },
    async downloadItem (item) {
      try {
        const response = await fetch(`${API_BASE}${item.downloadUrl}`, {
          method: 'GET',
          headers: this.tokenHeader()
        })
        if (!response.ok) {
          let message = '下载失败'
          try {
            const result = await response.json()
            message = result.message || message
          } catch (_) {}
          throw new Error(message)
        }
        const blob = await response.blob()
        this.triggerBrowserDownload(blob, item.downloadName || item.name || 'download')
      } catch (error) {
        ElMessage.error(error.message || '下载失败')
      }
    },
    triggerBrowserDownload (blob, filename) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    formatDateTime (value) {
      if (!value) {
        return '--'
      }
      const text = String(value).replace('T', ' ')
      return text.length > 19 ? text.slice(0, 19) : text
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

.publish-time,
.resource-subtitle {
  font-size: 13px;
  color: #606266;
}

.loading-box,
.empty-cell {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}

.table-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 4px;
  border: 1px solid #1f4e8c;
  background: #1f4e8c;
  color: #ffffff;
  cursor: pointer;
}
</style>
