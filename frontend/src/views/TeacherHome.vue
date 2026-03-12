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
        active-menu='teacher-home'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>教师主页</h2>
        </div>

        <div class='summary-grid'>
          <div class='summary-card'>
            <div class='summary-value'>6</div>
            <div class='summary-label'>已发布任务</div>
          </div>
          <div class='summary-card'>
            <div class='summary-value'>128</div>
            <div class='summary-label'>学生提交次数</div>
          </div>
          <div class='summary-card'>
            <div class='summary-value'>4</div>
            <div class='summary-label'>班级数量</div>
          </div>
          <div class='summary-card'>
            <div class='summary-value'>3</div>
            <div class='summary-label'>待处理任务</div>
          </div>
        </div>

        <div class='content-grid'>
          <div class='card'>
            <div class='card-title'>近期任务概览</div>
            <table class='common-table'>
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>截止时间</th>
                  <th>提交人数</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in pagedRecentTasks' :key='item.id'>
                  <td>{{ item.name }}</td>
                  <td>{{ item.deadline }}</td>
                  <td>{{ item.submitCount }}</td>
                  <td>{{ item.status }}</td>
                </tr>
              </tbody>
            </table>
            <CommonPagination
              v-model:currentPage='taskPage'
              v-model:pageSize='taskPageSize'
              :total='recentTasks.length'
              :page-size-options='[5, 10, 20]'
            />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'

export default {
  name: 'TeacherHomeView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      taskPage: 1,
      taskPageSize: 5,
      recentTasks: [
        { id: 1, name: '井字棋对战游戏', deadline: '2026-07-10 23:59', submitCount: 36, status: '进行中' },
        { id: 2, name: '井字棋对战游戏', deadline: '2026-07-15 23:59', submitCount: 28, status: '进行中' },
        { id: 3, name: '井字棋对战游戏', deadline: '2026-06-18 23:59', submitCount: 42, status: '已结束' }
      ]
    }
  },
  computed: {
    pagedRecentTasks () {
      const start = (this.taskPage - 1) * this.taskPageSize
      const end = start + this.taskPageSize
      return this.recentTasks.slice(start, end)
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
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
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

.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
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

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }
}

@media (max-width: 700px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 640px;
  }
}
</style>
