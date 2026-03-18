<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      :user-name='userName'
      :current-role='currentRole'
      active-nav='home'
      @platform-click='goHome'
      @user-click='goUserHome'
      @logout='logout'
    />

    <div class='layout'>
      <StudentSidebar
        v-if='isStudent'
        :logged-in='true'
        active-menu='ranking'
        :task-menu-open='false'
        @profile-click='goProfile'
        @class-click='goStudentClass'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <TeacherSidebar
        v-else
        active-menu='ranking'
        @teacher-home-click='goTeacherHome'
        @task-hall-click='goTaskHall'
        @history-click='goTeacherHistory'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>排行榜</h2>
        </div>

        <div class='table-card'>
          <div class='toolbar'>
            <select v-model='selectedTaskId' class='task-select'>
              <option
                v-for='task in taskOptions'
                :key='task.id'
                :value='task.id'
              >
                {{ task.name }}
              </option>
            </select>
          </div>

          <div class='score-note'>注：分数依据为近10轮次的平均分</div>

          <div class='table-wrapper'>
            <table class='ranking-table'>
              <thead>
                <tr>
                  <th>名次</th>
                  <th>学生姓名</th>
                  <th>分数</th>
                  <th>最后提交时间</th>
                  <th>对局详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in pagedRankingList' :key='item.id'>
                  <td>{{ item.rank }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.score }}</td>
                  <td>{{ item.lastSubmitTime }}</td>
                  <td>
                    <button class='view-btn' @click='openDetailDialog(item)'>查看</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <CommonPagination
            v-model:currentPage='currentPage'
            v-model:pageSize='pageSize'
            :total='currentRankingList.length'
            :page-size-options='[5, 10, 20]'
          />
        </div>
      </main>
    </div>

    <div v-if='showDetailDialog' class='dialog-mask' @click='closeDetailDialog'>
      <div class='dialog-box detail-dialog' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>近10轮详情</div>
          <button class='close-btn' @click='closeDetailDialog'>关闭</button>
        </div>

        <div class='dialog-body'>
          <div class='dialog-subtitle'>{{ currentDetailTitle }}</div>
          <div class='detail-table-wrapper'>
            <table class='detail-table'>
              <thead>
                <tr>
                  <th>序号</th>
                  <th>学生姓名</th>
                  <th>对手</th>
                  <th>胜负</th>
                  <th>积分</th>
                  <th>时间</th>
                  <th>测评详情</th>
                  <th>日志</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in currentDetailList' :key='item.id'>
                  <td>{{ item.index }}</td>
                  <td>{{ item.studentName }}</td>
                  <td>{{ item.opponent }}</td>
                  <td>{{ item.result }}</td>
                  <td>{{ item.score }}</td>
                  <td>{{ item.time }}</td>
                  <td>
                    <button class='disabled-btn' disabled>录像回放</button>
                  </td>
                  <td>
                    <button class='disabled-btn' disabled>下载</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'

const rankingDataMap = {
  task1: [
    { id: 1, rank: 1, name: '张三', score: '0.90', lastSubmitTime: '2026-03-18 10:25:11' },
    { id: 2, rank: 2, name: '李四', score: '0.80', lastSubmitTime: '2026-03-18 09:58:24' },
    { id: 3, rank: 3, name: '王五', score: '0.70', lastSubmitTime: '2026-03-18 09:20:05' }
  ],
  task2: [
    { id: 11, rank: 1, name: '张三', score: '95.60', lastSubmitTime: '2026-03-18 11:12:30' },
    { id: 12, rank: 2, name: '李四', score: '91.25', lastSubmitTime: '2026-03-18 10:43:12' },
    { id: 13, rank: 3, name: '王五', score: '88.40', lastSubmitTime: '2026-03-18 09:36:14' }
  ],
  task3: [
    { id: 21, rank: 1, name: '张三', score: '1.00', lastSubmitTime: '2026-03-18 08:56:06' },
    { id: 22, rank: 2, name: '李四', score: '0.90', lastSubmitTime: '2026-03-18 08:20:31' },
    { id: 23, rank: 3, name: '王五', score: '0.80', lastSubmitTime: '2026-03-17 22:42:10' }
  ]
}

const detailDataMap = {
  张三: [
    { id: 1, index: 1, studentName: '张三', opponent: '李四', result: '胜', score: 1, time: '2026-03-18 10:25:11' },
    { id: 2, index: 2, studentName: '张三', opponent: '王五', result: '胜', score: 1, time: '2026-03-18 09:58:11' },
    { id: 3, index: 3, studentName: '张三', opponent: '李四', result: '平', score: 0, time: '2026-03-18 09:26:48' },
    { id: 4, index: 4, studentName: '张三', opponent: '王五', result: '胜', score: 1, time: '2026-03-17 20:45:06' },
    { id: 5, index: 5, studentName: '张三', opponent: '李四', result: '负', score: 0, time: '2026-03-17 19:22:18' }
  ],
  李四: [
    { id: 11, index: 1, studentName: '李四', opponent: '张三', result: '负', score: 0, time: '2026-03-18 10:12:31' },
    { id: 12, index: 2, studentName: '李四', opponent: '王五', result: '胜', score: 1, time: '2026-03-18 09:42:19' },
    { id: 13, index: 3, studentName: '李四', opponent: '张三', result: '平', score: 0, time: '2026-03-18 09:10:44' },
    { id: 14, index: 4, studentName: '李四', opponent: '王五', result: '胜', score: 1, time: '2026-03-17 21:05:03' }
  ],
  王五: [
    { id: 21, index: 1, studentName: '王五', opponent: '张三', result: '负', score: 0, time: '2026-03-18 09:36:14' },
    { id: 22, index: 2, studentName: '王五', opponent: '李四', result: '负', score: 0, time: '2026-03-18 08:48:52' },
    { id: 23, index: 3, studentName: '王五', opponent: '张三', result: '胜', score: 1, time: '2026-03-17 22:42:10' }
  ],
  default: [
    { id: 101, index: 1, studentName: '张三', opponent: '李四', result: '胜', score: 1, time: '2026-03-18 11:06:31' },
    { id: 102, index: 2, studentName: '张三', opponent: '王五', result: '平', score: 0, time: '2026-03-18 10:31:22' },
    { id: 103, index: 3, studentName: '张三', opponent: '李四', result: '负', score: 0, time: '2026-03-18 09:48:16' }
  ]
}

export default {
  name: 'RankingView',
  components: {
    AppTopbar,
    StudentSidebar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      userName: localStorage.getItem('auth_name') || '',
      currentRole: localStorage.getItem('auth_role') === 'TEACHER' ? 'teacher' : 'student',
      selectedTaskId: 'task1',
      currentPage: 1,
      pageSize: 5,
      showDetailDialog: false,
      currentDetailTitle: '',
      currentDetailList: [],
      taskOptions: [
        { id: 'task1', name: '井字棋对战任务' },
        { id: 'task2', name: '小车控制单人任务' },
        { id: 'task3', name: '多轮博弈对战任务' }
      ]
    }
  },
  computed: {
    isStudent () {
      return !this.$route.path.startsWith('/teacher')
    },
    currentRankingList () {
      return rankingDataMap[this.selectedTaskId] || []
    },
    pagedRankingList () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.currentRankingList.slice(start, end)
    }
  },
  watch: {
    selectedTaskId () {
      this.currentPage = 1
    }
  },
  created () {
    const taskId = this.$route.query.taskId
    if (taskId && this.taskOptions.some(item => item.id === taskId)) {
      this.selectedTaskId = taskId
    }
  },
  methods: {
    openDetailDialog (item) {
      const source = detailDataMap[item.name] || detailDataMap.default
      this.currentDetailTitle = item.name + ' - 近10轮详情'
      this.currentDetailList = source.map(detail => ({
        ...detail,
        studentName: item.name
      }))
      this.showDetailDialog = true
    },
    closeDetailDialog () {
      this.showDetailDialog = false
    },
    goHome () {
      if (this.isStudent) {
        this.goHomeOpenTasks()
      } else {
        this.goTeacherHome()
      }
    },
    goUserHome () {
      if (this.isStudent) {
        this.goProfile()
      } else {
        this.goTeacherHome()
      }
    },
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
    goStudentClass () {
      this.$router.push('/student/class')
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    goTeacherHistory () {
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
    logout () {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
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
  padding: 18px 18px 0;
}

.toolbar {
  margin-bottom: 10px;
}

.task-select {
  min-width: 240px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #303133;
  background: #ffffff;
}

.score-note {
  margin-bottom: 14px;
  font-size: 13px;
  color: #909399;
}

.table-wrapper {
  overflow-x: auto;
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
  white-space: nowrap;
}

.ranking-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.view-btn,
.close-btn,
.disabled-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 13px;
}

.view-btn {
  border: 1px solid #1f4e8c;
  background: #ffffff;
  color: #1f4e8c;
  cursor: pointer;
}

.view-btn:hover {
  background: #ecf5ff;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.dialog-box {
  width: 100%;
  max-width: 1100px;
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.18);
}

.detail-dialog {
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  height: 56px;
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
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  cursor: pointer;
}

.dialog-body {
  padding: 18px 20px 20px;
  overflow: auto;
}

.dialog-subtitle {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.detail-table-wrapper {
  overflow-x: auto;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 960px;
}

.detail-table th,
.detail-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 12px 14px;
  text-align: left;
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
}

.detail-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.disabled-btn {
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #bfc4cd;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .table-card {
    padding: 16px 16px 0;
  }

  .task-select {
    width: 100%;
    min-width: 0;
  }
}
</style>
