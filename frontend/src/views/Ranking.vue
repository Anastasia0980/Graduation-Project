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
        :active-menu='""'
        :task-menu-open='false'
        @profile-click='goProfile'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>排行榜</h2>
        </div>

        <div class='table-card'>
          <table class='ranking-table'>
            <thead>
              <tr>
                <th>排名</th>
                <th>姓名</th>
                <th>积分</th>
                <th>胜场</th>
                <th>负场</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for='item in rankingList' :key='item.rank'>
                <td>{{ item.rank }}</td>
                <td>{{ item.name }}</td>
                <td>{{ item.score }}</td>
                <td>{{ item.win }}</td>
                <td>{{ item.lose }}</td>
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
import StudentSidebar from '../components/StudentSidebar.vue'

export default {
  name: 'RankingView',
  components: {
    AppTopbar,
    StudentSidebar
  },
  data () {
    return {
      rankingList: [
        { rank: 1, name: '李四', score: 1280, win: 18, lose: 4 },
        { rank: 2, name: '张三', score: 1205, win: 16, lose: 6 },
        { rank: 3, name: '王五', score: 1150, win: 14, lose: 7 },
        { rank: 4, name: '赵六', score: 1088, win: 12, lose: 8 },
        { rank: 5, name: '陈七', score: 1020, win: 10, lose: 9 }
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
    goProfile () {
      this.$router.push('/student/profile')
    },
    goHistory () {
      this.$router.push('/student/history')
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

.table-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;
}

.ranking-table th,
.ranking-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
  color: #303133;
}

.ranking-table th {
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

  .table-card {
    overflow-x: auto;
  }

  .ranking-table {
    min-width: 680px;
  }
}
</style>
