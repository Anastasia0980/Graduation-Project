<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      user-name='张三'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <StudentSidebar
        :logged-in='true'
        active-menu='profile'
        :task-menu-open='false'
        @profile-click='goProfile'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>个人主页</h2>
        </div>

        <div class='profile-grid'>
          <div class='card'>
            <div class='card-title'>基本信息</div>
            <div class='info-list'>
              <div class='info-row'>
                <span class='label'>姓名</span>
                <span class='value'>张三</span>
              </div>
              <div class='info-row'>
                <span class='label'>学号</span>
                <span class='value'>2023123456</span>
              </div>
              <div class='info-row'>
                <span class='label'>班级</span>
                <span class='value'>人工智能 2201</span>
              </div>
              <div class='info-row'>
                <span class='label'>身份</span>
                <span class='value'>学生</span>
              </div>
            </div>

            <div class='profile-action'>
              <button class='edit-btn' type='button'>修改</button>
            </div>
          </div>

          <div class='card'>
            <div class='card-title'>任务概览</div>
            <div class='summary-grid'>
              <div class='summary-item'>
                <div class='summary-value'>4</div>
                <div class='summary-label'>开放任务</div>
              </div>
              <div class='summary-item'>
                <div class='summary-value'>12</div>
                <div class='summary-label'>历史提交</div>
              </div>
              <div class='summary-item'>
                <div class='summary-value'>7</div>
                <div class='summary-label'>已完成测评</div>
              </div>
              <div class='summary-item'>
                <div class='summary-value'>3</div>
                <div class='summary-label'>获胜次数</div>
              </div>
            </div>
          </div>

          <div class='card full-width'>
            <div class='card-title'>近期记录</div>
            <table class='record-table'>
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>提交时间</th>
                  <th>状态</th>
                  <th>结果</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in recentRecords' :key='item.id'>
                  <td>{{ item.taskName }}</td>
                  <td>{{ item.submitTime }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.result }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'

export default {
  name: 'StudentProfileView',
  components: {
    AppTopbar,
    StudentSidebar
  },
  data () {
    return {
      recentRecords: [
        {
          id: 1,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-05 14:10',
          status: '测评中',
          result: '-'
        },
        {
          id: 2,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-03 18:42',
          status: '已完成',
          result: '失败'
        },
        {
          id: 3,
          taskName: '井字棋对战游戏',
          submitTime: '2026-07-01 20:15',
          status: '已完成',
          result: '获胜'
        }
      ]
    }
  },
  methods: {
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goHomeEndedTasks () {
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/')
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

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.full-width {
  grid-column: 1 / -1;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.label {
  font-size: 14px;
  color: #606266;
}

.value {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.profile-action {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.edit-btn {
  height: 36px;
  min-width: 88px;
  padding: 0 18px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.edit-btn:hover {
  background: #173b69;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.summary-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 18px 16px;
  background: #f8fafc;
}

.summary-value {
  font-size: 26px;
  font-weight: 700;
  color: #1f4e8c;
  margin-bottom: 8px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.record-table {
  width: 100%;
  border-collapse: collapse;
}

.record-table th,
.record-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 12px;
  text-align: left;
  font-size: 14px;
  color: #303133;
}

.record-table th {
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

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: auto;
  }
}

@media (max-width: 700px) {
  .record-table {
    min-width: 640px;
  }

  .card {
    overflow-x: auto;
  }
}
</style>
