<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      user-name='王老师'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTeacherHome'
      @user-click='goTeacherHome'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar
        active-menu='export-score'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>导出成绩</h2>
        </div>

        <div class='card'>
          <div class='card-title'>导出条件</div>

          <div class='form-grid'>
            <div class='form-item'>
              <label>选择班级</label>
              <select>
                <option>人工智能 2201</option>
                <option>人工智能 2202</option>
                <option>软件工程 2201</option>
              </select>
            </div>

            <div class='form-item'>
              <label>选择任务</label>
              <select>
                <option>井字棋对战游戏</option>
                <option>Connect Four 对战任务</option>
              </select>
            </div>
          </div>

          <div class='action-row'>
            <button class='primary-btn'>导出 Excel</button>
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
                <th>导出状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for='item in exportList' :key='item.id'>
                <td>{{ item.className }}</td>
                <td>{{ item.taskName }}</td>
                <td>{{ item.exportTime }}</td>
                <td>{{ item.status }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

export default {
  name: 'TeacherExportScoreView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      exportList: [
        { id: 1, className: '人工智能 2201', taskName: '井字棋对战游戏', exportTime: '2026-07-06 10:20', status: '已完成' },
        { id: 2, className: '人工智能 2202', taskName: '井字棋对战游戏', exportTime: '2026-07-05 16:40', status: '已完成' }
      ]
    }
  },
  methods: {
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

  .table-section {
    overflow-x: auto;
  }

  .common-table {
    min-width: 680px;
  }
}
</style>
