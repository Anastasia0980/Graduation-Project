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
        active-menu='task-manage'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>任务管理</h2>
        </div>

        <div class='card'>
          <div class='card-title'>任务列表</div>
          <table class='common-table'>
            <thead>
              <tr>
                <th>任务名称</th>
                <th>班级</th>
                <th>开始时间</th>
                <th>截止时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for='item in taskList' :key='item.id'>
                <td>{{ item.name }}</td>
                <td>{{ item.className }}</td>
                <td>{{ item.startTime }}</td>
                <td>{{ item.deadline }}</td>
                <td>{{ item.status }}</td>
                <td>
                  <button class='table-btn' type='button'>修改</button>
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
  name: 'TeacherTaskManageView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      taskList: [
        { id: 1, name: '井字棋对战游戏', className: '人工智能 2201', startTime: '2026-07-01 08:00', deadline: '2026-07-10 23:59', status: '进行中' },
        { id: 2, name: '井字棋对战游戏', className: '人工智能 2202', startTime: '2026-07-02 08:00', deadline: '2026-07-15 23:59', status: '进行中' },
        { id: 3, name: '井字棋对战游戏', className: '软件工程 2201', startTime: '2026-06-01 08:00', deadline: '2026-06-18 23:59', status: '已结束' }
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
  min-width: 72px;
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
    min-width: 860px;
  }
}
</style>
