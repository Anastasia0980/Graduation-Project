<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      user-name="王老师"
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
              <tr v-for="item in pagedTaskList" :key="item.id">
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
          确定要删除任务“{{ currentTask ? currentTask.name : '' }}”吗？
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
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'

export default {
  name: 'TeacherTaskManageView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      taskPage: 1,
      taskPageSize: 5,
      showDeleteDialog: false,
      currentTask: null,
      taskList: [
        { id: 1, name: '井字棋对战游戏', className: '人工智能 2201', startTime: '2026-07-01 08:00', deadline: '2026-07-10 23:59', status: '进行中' },
        { id: 2, name: '井字棋单人测评任务', className: '人工智能 2202', startTime: '2026-07-02 08:00', deadline: '2026-07-15 23:59', status: '进行中' },
        { id: 3, name: '井字棋团队锦标赛任务', className: '软件工程 2201', startTime: '2026-06-01 08:00', deadline: '2026-06-18 23:59', status: '已结束' }
      ]
    }
  },
  computed: {
    pagedTaskList () {
      const start = (this.taskPage - 1) * this.taskPageSize
      const end = start + this.taskPageSize
      return this.taskList.slice(start, end)
    }
  },
  methods: {
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
    confirmDelete () {
      if (!this.currentTask) return
      this.taskList = this.taskList.filter(item => item.id !== this.currentTask.id)
      this.currentTask = null
      this.showDeleteDialog = false
    },
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
