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
      <TeacherSidebar active-menu="task-overview-summary" />

      <main class="content-area">
        <div class="page-header">
          <h2>提交数据总览</h2>
        </div>

        <div class="card">
          <table class="common-table">
            <thead>
              <tr>
                <th>任务名称</th>
                <th>所属班级</th>
                <th>结束时间</th>
                <th>状态</th>
                <th>已提交人数</th>
                <th>未提交人数</th>
                <th>总提交次数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="table-empty-cell">数据加载中...</td>
              </tr>

              <tr v-else-if="pagedOverviewList.length === 0">
                <td colspan="8" class="table-empty-cell">当前暂无任务数据</td>
              </tr>

              <tr v-else v-for="item in pagedOverviewList" :key="item.assignmentId">
                <td>{{ item.taskName }}</td>
                <td>{{ item.className }}</td>
                <td>{{ item.deadline }}</td>
                <td>{{ item.status }}</td>
                <td>
                  <span class="count-link" @click="openStudentDialog('submitted', item)">
                    {{ item.submittedCount }}
                  </span>
                </td>
                <td>
                  <span class="count-link" @click="openStudentDialog('unsubmitted', item)">
                    {{ item.unsubmittedCount }}
                  </span>
                </td>
                <td>{{ item.totalSubmissionCount }}</td>
                <td>
                  <button class="primary-btn small-btn" @click="goSubmissionDetail(item)">
                    查看提交详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-model:currentPage="currentPage"
            v-model:pageSize="pageSize"
            :total="overviewList.length"
            :page-size-options="[5, 10, 20]"
          />
        </div>
      </main>
    </div>

    <div v-if="showStudentDialog" class="dialog-mask" @click="showStudentDialog = false">
      <div class="dialog-box" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">{{ studentDialogTitle }}</div>
          <button class="close-btn" @click="showStudentDialog = false">关闭</button>
        </div>

        <div class="dialog-body">
          <div v-if="currentStudentNames.length === 0" class="empty-student-box">当前暂无学生</div>
          <ul v-else class="student-name-list">
            <li v-for="(name, index) in currentStudentNames" :key="index">
              {{ name }}
            </li>
          </ul>
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
import { apiRequest } from '../utils/http'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherTaskOverviewView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      loading: false,
      currentPage: 1,
      pageSize: 5,
      overviewList: [],
      showStudentDialog: false,
      studentDialogTitle: '',
      currentStudentNames: []
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    pagedOverviewList () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.overviewList.slice(start, end)
    }
  },
  created () {
    this.loadOverviewList()
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadOverviewList () {
      this.loading = true
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/assignments/overview`, {
          method: 'GET'
        })
        if (!result) return

        const list = Array.isArray(result.data) ? result.data : []
        this.overviewList = list.map(item => ({
          assignmentId: item.assignmentId,
          taskName: item.taskName || '未知任务',
          className: item.className || '未分配班级',
          deadline: this.formatDateTime(item.deadline),
          status: item.status || '--',
          submittedCount: item.submittedCount || 0,
          unsubmittedCount: item.unsubmittedCount || 0,
          totalSubmissionCount: item.totalSubmissionCount || 0,
          submittedStudentNames: Array.isArray(item.submittedStudentNames) ? item.submittedStudentNames : [],
          unsubmittedStudentNames: Array.isArray(item.unsubmittedStudentNames) ? item.unsubmittedStudentNames : []
        }))
      } catch (error) {
        this.overviewList = []
        ElMessage.error(error.message || '提交概览加载失败')
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
    openStudentDialog (type, item) {
      this.studentDialogTitle = type === 'submitted' ? '已提交学生名单' : '未提交学生名单'
      this.currentStudentNames = type === 'submitted'
        ? item.submittedStudentNames
        : item.unsubmittedStudentNames
      this.showStudentDialog = true
    },
    goSubmissionDetail (item) {
      this.$router.push({
        path: `/teacher/task-submissions/${item.assignmentId}`,
        query: { source: 'overview' }
      })
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
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

.count-link {
  color: #1f4e8c;
  cursor: pointer;
  text-decoration: underline;
}

.primary-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  background: #1f4e8c;
  color: #ffffff;
}

.primary-btn:hover {
  background: #173b69;
}

.small-btn {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
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
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
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

.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.dialog-body {
  padding: 20px;
  font-size: 14px;
  color: #303133;
}

.student-name-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
}

.empty-student-box {
  text-align: center;
  color: #909399;
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
    min-width: 980px;
  }
}
</style>
