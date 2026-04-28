<template>
  <div class="page">
    <AppTopbar
      :logged-in="isLoggedIn"
      :user-name="teacherName"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @switch-role="switchRole"
      @logout="logout"
    />

    <div class="layout">
      <TeacherSidebar active-menu="task-manage" />

      <main class="content-area">
        <div class="page-header page-header-between">
          <h2>任务管理</h2>
          <button class="primary-btn publish-btn" @click="goPublishTask">发布任务</button>
        </div>

        <div class="card">
          <table class="common-table">
            <thead>
              <tr>
                <th>任务名称</th>
                <th>所属班级</th>
                <th>开始时间</th>
                <th>截止时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="table-empty-cell">任务加载中...</td>
              </tr>

              <tr v-else-if="pagedTaskList.length === 0">
                <td colspan="6" class="table-empty-cell">当前暂无任务</td>
              </tr>

              <tr v-else v-for="item in pagedTaskList" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.className }}</td>
                <td>{{ item.startTime }}</td>
                <td>{{ item.deadline }}</td>
                <td>{{ item.status }}</td>
                <td>
                  <div class="action-group">
                    <button class="primary-btn small-btn" @click="goEditTask(item)">修改</button>
                    <button class="danger-btn small-btn" @click="openDeleteDialog(item)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-model:currentPage="taskPage"
            v-model:pageSize="taskPageSize"
            :total="taskList.length"
            :page-size-options="[5, 10, 20]"
          />
        </div>
      </main>
    </div>

    <div v-if="showDeleteDialog" class="dialog-mask" @click="showDeleteDialog = false">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">删除任务</div>
          <button class="close-btn" @click="showDeleteDialog = false">关闭</button>
        </div>

        <div class="dialog-body">
          <div>确定要删除任务“{{ currentTask ? currentTask.name : '' }}”吗？</div>
          <div class="dialog-tip">删除后学生端将无法再看到该任务，但已有提交记录不会被删除。</div>
        </div>

        <div class="dialog-footer">
          <button class="secondary-btn" @click="showDeleteDialog = false">取消</button>
          <button class="danger-btn" @click="confirmDelete">确定删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest, getApiBaseUrl } from '../utils/http'

const API_BASE = getApiBaseUrl()

export default {
  name: 'TeacherTaskManageView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      taskPage: 1,
      taskPageSize: 5,
      showDeleteDialog: false,
      currentTask: null,
      loading: false,
      taskList: [],
      classMap: {}
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    pagedTaskList () {
      const start = (this.taskPage - 1) * this.taskPageSize
      const end = start + this.taskPageSize
      return this.taskList.slice(start, end)
    }
  },
  created () {
    this.loadClassMap()
      .then(() => this.loadTaskList())
      .catch(() => this.loadTaskList())
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadClassMap () {
      const result = await this.requestApi(`${API_BASE}/class?pageNum=0&pageSize=500`, {
        method: 'GET'
      })
      if (!result) return
      if (result.code !== 0) {
        throw new Error(result.message || '班级数据加载失败')
      }
      const pageData = result.data || {}
      const content = Array.isArray(pageData.content) ? pageData.content : []
      this.classMap = {}
      content.forEach(item => {
        this.classMap[item.id] = item.name
      })
    },
    async loadTaskList () {
      this.loading = true
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/assignments?pageNum=0&pageSize=500`, {
          method: 'GET'
        })
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []

        this.taskList = content.map(item => {
          const nowTime = new Date().getTime()
          const deadlineTime = item.deadline ? new Date(item.deadline).getTime() : nowTime
          const createTime = item.createTime || item.updateTime || item.deadline

          return {
            id: item.id,
            name: item.title,
            classId: item.studentClass ? item.studentClass.id : null,
            className: item.studentClass ? item.studentClass.name : (this.classMap[item.classId] || '未分配班级'),
            startTime: this.formatDateTime(createTime),
            deadline: this.formatDateTime(item.deadline),
            status: deadlineTime >= nowTime ? '进行中' : '已结束'
          }
        })
      } catch (error) {
        this.taskList = []
        ElMessage.error(error.message || '任务数据加载失败')
      } finally {
        this.loading = false
      }
    },
    formatDateTime (value) {
      if (!value) return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '--'
      const Y = date.getFullYear()
      const M = String(date.getMonth() + 1).padStart(2, '0')
      const D = String(date.getDate()).padStart(2, '0')
      const h = String(date.getHours()).padStart(2, '0')
      const m = String(date.getMinutes()).padStart(2, '0')
      return `${Y}-${M}-${D} ${h}:${m}`
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goPublishTask () {
      this.$router.push('/teacher/publish')
    },
    goEditTask (task) {
      this.$router.push(`/teacher/task-edit/${task.id}`)
    },
    openDeleteDialog (task) {
      this.currentTask = task
      this.showDeleteDialog = true
    },
    async confirmDelete () {
      if (!this.currentTask) return
      try {
        const result = await this.requestApi(`${API_BASE}/assignments/${this.currentTask.id}`, {
          method: 'DELETE'
        })
        if (!result) return
        if (result.code !== 0) {
          throw new Error(result.message || '删除任务失败')
        }
        ElMessage.success('删除任务成功')
        this.showDeleteDialog = false
        this.currentTask = null
        this.loadTaskList()
      } catch (error) {
        ElMessage.error(error.message || '删除任务失败')
      }
    },
    switchRole () {
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      clearAuthState()
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

.page-header-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.publish-btn {
  min-width: 96px;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
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

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.primary-btn,
.secondary-btn,
.danger-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.small-btn {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
}

.primary-btn {
  background: #1f4e8c;
  color: #ffffff;
}

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

.danger-btn {
  background: #f56c6c;
  color: #ffffff;
}

.danger-btn:hover {
  background: #dd6161;
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
  line-height: 1.7;
}

.dialog-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.dialog-footer {
  padding: 0 20px 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .page-header-between {
    flex-direction: column;
    align-items: stretch;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 900px;
  }
}
</style>
