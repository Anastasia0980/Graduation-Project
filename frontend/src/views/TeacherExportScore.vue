<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='displayUserName'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTeacherHome'
      @user-click='goTeacherHome'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar active-menu='export-score' />

      <main class='content-area'>
        <div class='page-header'>
          <h2>导出成绩</h2>
        </div>

        <div class='card'>
          <div class='form-grid'>
            <div class='form-item'>
              <label>选择班级</label>
              <select v-model='selectedClassId'>
                <option value=''>请选择班级</option>
                <option v-for='item in classOptions' :key='item.id' :value='item.id'>
                  {{ item.name }}
                </option>
              </select>
            </div>

            <div class='form-item'>
              <label>选择任务</label>
              <select v-model='selectedTaskId'>
                <option value=''>请选择任务</option>
                <option
                  v-for='item in filteredTaskOptions'
                  :key='item.id'
                  :value='item.id'
                >
                  {{ item.title }}
                </option>
              </select>
            </div>
          </div>

          <div class='action-row'>
            <button class='primary-btn' :disabled='exporting' @click='handleExport'>
              {{ exporting ? '导出中...' : '导出成绩' }}
            </button>
          </div>
        </div>

        <div class='card table-section'>
          <div class='card-title'>最近导出记录</div>
          <table class='common-table'>
            <thead>
              <tr>
                <th>班级</th>
                <th>任务名称</th>
                <th>导出时间</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if='recordLoading'>
                <td colspan='4' class='table-empty-cell'>记录加载中...</td>
              </tr>
              <tr v-else-if='pagedExportList.length === 0'>
                <td colspan='4' class='table-empty-cell'>当前暂无导出记录</td>
              </tr>
              <tr v-else v-for='item in pagedExportList' :key='item.id'>
                <td>{{ item.className }}</td>
                <td>{{ item.taskName }}</td>
                <td>{{ item.exportTime }}</td>
                <td>{{ item.status }}</td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-model:currentPage='exportPage'
            v-model:pageSize='exportPageSize'
            :total='exportList.length'
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
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest } from '../utils/http'

const API_BASE = (process.env.VUE_APP_API_BASE && process.env.VUE_APP_API_BASE.trim()) ||
  (typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : 'http://localhost:8080')

export default {
  name: 'TeacherExportScoreView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      exportPage: 1,
      exportPageSize: 5,
      exporting: false,
      recordLoading: false,
      classOptions: [],
      taskOptions: [],
      exportList: [],
      selectedClassId: '',
      selectedTaskId: ''
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    displayUserName () {
      return localStorage.getItem('auth_name') || '教师'
    },
    filteredTaskOptions () {
      if (!this.selectedClassId) return []
      return this.taskOptions.filter(item => String(item.classId) === String(this.selectedClassId))
    },
    pagedExportList () {
      const start = (this.exportPage - 1) * this.exportPageSize
      const end = start + this.exportPageSize
      return this.exportList.slice(start, end)
    }
  },
  watch: {
    selectedClassId () {
      if (!this.filteredTaskOptions.find(item => String(item.id) === String(this.selectedTaskId))) {
        this.selectedTaskId = ''
      }
    }
  },
  created () {
    this.loadClassOptions()
    this.loadTaskOptions()
    this.loadExportRecords()
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadClassOptions () {
      try {
        const result = await this.requestApi(`${API_BASE}/class?pageNum=0&pageSize=500`, { method: 'GET' })
        if (!result) return
        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.classOptions = content.map(item => ({
          id: item.id,
          name: item.name
        }))
      } catch (error) {
        this.classOptions = []
        ElMessage.error(error.message || '班级数据加载失败')
      }
    },
    async loadTaskOptions () {
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/assignments?pageNum=0&pageSize=500`, { method: 'GET' })
        if (!result) return
        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.taskOptions = content.map(item => ({
          id: item.id,
          title: item.title,
          classId: item.studentClass ? item.studentClass.id : '',
          className: item.studentClass ? item.studentClass.name : '未分配班级',
          mode: item.evaluationMode
        }))
      } catch (error) {
        this.taskOptions = []
        ElMessage.error(error.message || '任务数据加载失败')
      }
    },
    async loadExportRecords () {
      this.recordLoading = true
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/exports`, { method: 'GET' })
        if (!result) return
        const list = Array.isArray(result.data) ? result.data : []
        this.exportList = list.map(item => ({
          id: item.id,
          className: item.className || '未分配班级',
          taskName: item.taskName || '未知任务',
          exportTime: this.formatDateTime(item.exportTime),
          status: item.status || '--'
        }))
      } catch (error) {
        this.exportList = []
        ElMessage.error(error.message || '导出记录加载失败')
      } finally {
        this.recordLoading = false
      }
    },
    formatDateTime (value) {
      if (!value) return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return String(value).replace('T', ' ').slice(0, 19)
      }
      const Y = date.getFullYear()
      const M = String(date.getMonth() + 1).padStart(2, '0')
      const D = String(date.getDate()).padStart(2, '0')
      const h = String(date.getHours()).padStart(2, '0')
      const m = String(date.getMinutes()).padStart(2, '0')
      return `${Y}-${M}-${D} ${h}:${m}`
    },
    async handleExport () {
      if (!this.selectedClassId) {
        ElMessage.warning('请先选择班级')
        return
      }
      if (!this.selectedTaskId) {
        ElMessage.warning('请先选择任务')
        return
      }

      this.exporting = true
      try {
        const result = await this.requestApi(`${API_BASE}/teacher/exports`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            assignmentId: this.selectedTaskId
          })
        })
        if (!result) return
        if (result.code !== 0 || !result.data) {
          throw new Error(result.message || '导出失败')
        }

        this.exportExcel(result.data)
        ElMessage.success('成绩导出成功')
        this.loadExportRecords()
      } catch (error) {
        ElMessage.error(error.message || '导出失败')
      } finally {
        this.exporting = false
      }
    },
    exportExcel (data) {
      const filename = `${data.className || '未分配班级'}${data.taskName || '任务'}成绩汇总.xls`

      let headerHtml = ''
      let bodyHtml = ''

      if (data.mode === 'SINGLE') {
        headerHtml = `
          <tr>
            <th>排名</th>
            <th>姓名</th>
            <th>闯过关卡数</th>
            <th>闯关时间</th>
          </tr>
        `
        bodyHtml = (data.rows || []).map(item => `
          <tr>
            <td>${item.rank ?? ''}</td>
            <td>${item.name ?? ''}</td>
            <td>${item.levelCount ?? ''}</td>
            <td>${item.clearTime ?? ''}</td>
          </tr>
        `).join('')
      } else if (data.mode === 'TEAM') {
        headerHtml = `
          <tr>
            <th>排名</th>
            <th>队伍名</th>
            <th>队长姓名</th>
            <th>队员1姓名</th>
            <th>队员2姓名</th>
            <th>天梯分</th>
            <th>总对战场次</th>
            <th>获胜场次</th>
            <th>失败场次</th>
            <th>平局场次</th>
          </tr>
        `
        bodyHtml = (data.rows || []).map(item => `
          <tr>
            <td>${item.rank ?? ''}</td>
            <td>${item.name ?? ''}</td>
            <td>${item.captainName ?? ''}</td>
            <td>${item.member1Name ?? ''}</td>
            <td>${item.member2Name ?? ''}</td>
            <td>${item.ladderScore ?? ''}</td>
            <td>${item.matchCount ?? ''}</td>
            <td>${item.winCount ?? ''}</td>
            <td>${item.loseCount ?? ''}</td>
            <td>${item.drawCount ?? ''}</td>
          </tr>
        `).join('')
      } else {
        headerHtml = `
          <tr>
            <th>排名</th>
            <th>姓名</th>
            <th>天梯分</th>
            <th>总对战场次</th>
            <th>获胜场次</th>
            <th>失败场次</th>
            <th>平局场次</th>
          </tr>
        `
        bodyHtml = (data.rows || []).map(item => `
          <tr>
            <td>${item.rank ?? ''}</td>
            <td>${item.name ?? ''}</td>
            <td>${item.ladderScore ?? ''}</td>
            <td>${item.matchCount ?? ''}</td>
            <td>${item.winCount ?? ''}</td>
            <td>${item.loseCount ?? ''}</td>
            <td>${item.drawCount ?? ''}</td>
          </tr>
        `).join('')
      }

      const html = `
        <html>
          <head>
            <meta charset="UTF-8" />
          </head>
          <body>
            <table border="1">
              ${headerHtml}
              ${bodyHtml}
            </table>
          </body>
        </html>
      `

      const blob = new Blob(['\ufeff' + html], {
        type: 'application/vnd.ms-excel;charset=utf-8;'
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
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

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1f2d3d;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item select {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 14px;
  background: #ffffff;
  color: #303133;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
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

.primary-btn:disabled {
  background: #a0b3c9;
  cursor: not-allowed;
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

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 680px;
  }
}
</style>
