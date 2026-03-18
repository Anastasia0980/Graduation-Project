<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      :user-name="teacherName"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @switch-role="switchRole"
      @logout="logout"
    />

    <div class="layout">
      <TeacherSidebar
        active-menu="task-manage"
        @teacher-home-click="goTeacherHome"
        @task-hall-click="goTaskHall"
        @history-click="goHistory"
        @publish-click="goPublishTask"
        @manage-click="goTaskManage"
        @class-data-click="goClassData"
        @export-click="goExportScore"
      />

      <main class="content-area">
        <div class="page-header">
          <h2>任务管理</h2>
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
                    <button class="secondary-btn small-btn" @click="goViewTask(item)">查看</button>
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

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherTaskManageView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '王老师',
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
    pagedTaskList () {
      const start = (this.taskPage - 1) * this.taskPageSize
      const end = start + this.taskPageSize
      return this.taskList.slice(start, end)
    }
  },
  created () {
    this.loadTaskList()
  },
  methods: {
    async loadTaskList () {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      this.loading = true
      try {
        await this.loadClassMap(token)

        const response = await fetch(`${API_BASE}/teacher/assignments?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '任务列表加载失败')
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []

        this.taskList = content.map(item => this.mapTaskItem(item))
      } catch (error) {
        this.taskList = []
        ElMessage.error(error.message || '任务列表加载失败')
      } finally {
        this.loading = false
      }
    },
    async loadClassMap (token) {
      try {
        const response = await fetch(`${API_BASE}/class?pageNum=0&pageSize=100`, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          this.classMap = {}
          return
        }

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        const map = {}

        content.forEach(item => {
          map[item.id] = item.name
        })

        this.classMap = map
      } catch (error) {
        this.classMap = {}
      }
    },
    mapTaskItem (item) {
      const classId = item.studentClass && item.studentClass.id ? item.studentClass.id : null
      const className = classId && this.classMap[classId]
        ? this.classMap[classId]
        : '未命名班级'

      return {
        id: item.id,
        name: item.title || '未命名任务',
        className,
        startTime: this.formatDateTime(item.createTime),
        deadline: this.formatDateTime(item.deadline),
        status: this.computeTaskStatus(item.deadline),
        raw: item
      }
    },
    formatDateTime (value) {
      if (!value) {
        return '--'
      }

      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')

      return `${year}-${month}-${day} ${hour}:${minute}`
    },
    computeTaskStatus (deadline) {
      if (!deadline) {
        return '进行中'
      }

      const deadlineDate = new Date(deadline)
      if (Number.isNaN(deadlineDate.getTime())) {
        return '进行中'
      }

      return deadlineDate.getTime() < Date.now() ? '已结束' : '进行中'
    },
    goEditTask (item) {
      this.$router.push(`/teacher/task-edit/${item.id}`)
    },
    goViewTask (item) {
      this.$router.push(`/teacher/task-submissions/${item.id}`)
    },
    openDeleteDialog (item) {
      this.currentTask = item
      this.showDeleteDialog = true
    },
    async confirmDelete () {
      if (!this.currentTask || !this.currentTask.id) {
        this.showDeleteDialog = false
        return
      }

      const token = localStorage.getItem('auth_token')
      if (!token) {
        this.showDeleteDialog = false
        ElMessage.error('登录信息已失效，请重新登录')
        return
      }

      const assignmentId = this.currentTask.id
      this.showDeleteDialog = false

      try {
        const response = await fetch(`${API_BASE}/assignments/${assignmentId}`, {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '删除任务失败')
          return
        }

        // 从本地列表中移除该任务，避免重新请求
        this.taskList = this.taskList.filter(item => item.id !== assignmentId)
        ElMessage.success('删除任务成功')
      } catch (error) {
        ElMessage.error(error.message || '删除任务失败，请检查后端是否已启动')
      } finally {
        this.currentTask = null
      }
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    goHistory () {
      this.$router.push('/teacher/history')
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

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
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

.small-btn {
  min-width: 64px;
  height: 34px;
  padding: 0 12px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  background: #1f4e8c;
  color: #ffffff;
}

.primary-btn:hover {
  background: #173b69;
}

.secondary-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.danger-btn {
  border: none;
  background: #d9534f;
  color: #ffffff;
}

.danger-btn:hover {
  background: #c9302c;
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
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
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

.dialog-body {
  padding: 20px;
  font-size: 14px;
  line-height: 1.8;
}

.dialog-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 920px;
  }
}
</style>
