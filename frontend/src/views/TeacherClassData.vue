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
        active-menu='class-data'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>班级数据</h2>
        </div>

        <div class='summary-grid'>
          <div class='summary-card'>
            <div class='summary-value'>4</div>
            <div class='summary-label'>班级总数</div>
          </div>
          <div class='summary-card'>
            <div class='summary-value'>156</div>
            <div class='summary-label'>学生总数</div>
          </div>
          <div class='summary-card'>
            <div class='summary-value'>128</div>
            <div class='summary-label'>有效提交</div>
          </div>
        </div>

        <div class='card'>
          <div class='card-title'>班级统计</div>
          <table class='common-table'>
            <thead>
              <tr>
                <th>班级名称</th>
                <th>学生人数</th>
                <th>提交人数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for='item in classList' :key='item.id'>
                <td>{{ item.name }}</td>
                <td>{{ item.studentCount }}</td>
                <td>{{ item.submitCount }}</td>
                <td>
                  <button class='table-btn' type='button'>查看详情</button>
                </td>
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
  name: 'TeacherClassDataView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      classList: [
        { id: 1, name: '人工智能 2201', studentCount: 42, submitCount: 36 },
        { id: 2, name: '人工智能 2202', studentCount: 40, submitCount: 28 },
        { id: 3, name: '软件工程 2201', studentCount: 38, submitCount: 31 },
        { id: 4, name: '数据科学 2201', studentCount: 36, submitCount: 33 }
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-bottom: 20px;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 22px 18px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f4e8c;
  margin-bottom: 8px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1f2d3d;
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

.table-btn {
  height: 34px;
  min-width: 88px;
  padding: 0 14px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  cursor: pointer;
  font-size: 13px;
}

.table-btn:hover {
  background: #173b69;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
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
    min-width: 760px;
  }
}
</style>
