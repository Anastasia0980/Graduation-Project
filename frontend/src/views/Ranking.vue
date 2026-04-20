<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
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
        :logged-in='isLoggedIn'
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
                v-for='task in currentTaskOptions'
                :key='task.id'
                :value='task.id'
              >
                {{ task.name }}
              </option>
            </select>

            <div class='mode-switch'>
              <button
                class='mode-btn'
                :class='{ active: currentMode === "single" }'
                @click='switchMode("single")'
              >
                单人模式
              </button>
              <button
                class='mode-btn'
                :class='{ active: currentMode === "versus" }'
                @click='switchMode("versus")'
              >
                对战模式
              </button>
              <button
                class='mode-btn'
                :class='{ active: currentMode === "team" }'
                @click='switchMode("team")'
              >
                分组对战
              </button>
            </div>
          </div>

          <div class='score-note'>{{ currentRuleText }}</div>

          <div class='table-wrapper'>
            <table class='ranking-table'>
              <thead>
                <tr v-if='currentMode === "single"'>
                  <th>排名</th>
                  <th>姓名</th>
                  <th>闯过关卡数</th>
                  <th>闯关时间</th>
                </tr>
                <tr v-else-if='currentMode === "team"'>
                  <th>排名</th>
                  <th>队伍名</th>
                  <th>队长姓名</th>
                  <th>队员1姓名</th>
                  <th>队员2姓名</th>
                  <th>天梯分</th>
                  <th>战绩</th>
                  <th>对战场次</th>
                </tr>
                <tr v-else>
                  <th>排名</th>
                  <th>姓名</th>
                  <th>天梯分</th>
                  <th>战绩</th>
                  <th>对战场次</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if='loading'>
                  <td :colspan='currentColspan' class='empty-cell'>加载中...</td>
                </tr>

                <tr v-else-if='pagedRankingList.length === 0'>
                  <td :colspan='currentColspan' class='empty-cell'>当前暂无排行榜数据</td>
                </tr>

                <tr
                  v-else-if='currentMode === "single"'
                  v-for='item in pagedRankingList'
                  :key='item.id'
                >
                  <td>{{ item.rank }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.levelCount }}</td>
                  <td>{{ item.clearTime }}</td>
                </tr>

                <tr
                  v-else-if='currentMode === "team"'
                  v-for='item in pagedRankingList'
                  :key='item.teamId || item.rank'
                >
                  <td>{{ item.rank }}</td>
                  <td>{{ item.teamName }}</td>
                  <td>{{ item.captainName }}</td>
                  <td>{{ item.member1Name }}</td>
                  <td>{{ item.member2Name }}</td>
                  <td>{{ item.ladderScore }}</td>
                  <td>{{ item.record }}</td>
                  <td>{{ item.matchCount }}</td>
                </tr>

                <tr v-else v-for='item in pagedRankingList' :key='item.studentId'>
                  <td>{{ item.rank }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.ladderScore }}</td>
                  <td>{{ item.record }}</td>
                  <td>{{ item.matchCount }}</td>
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
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest } from '../utils/http'

const API_BASE = 'http://localhost:8080'

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
      currentMode: 'single',
      selectedTaskId: '',
      currentPage: 1,
      pageSize: 5,
      loading: false,
      allTaskOptions: [],
      singleRankingList: [],
      versusRankingList: [],
      teamRankingList: [],
      singleRankingMockList: []
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    isStudent () {
      return !this.$route.path.startsWith('/teacher')
    },
    currentTaskOptions () {
      if (this.currentMode === 'single') {
        return this.allTaskOptions.filter(item => item.mode === 'SINGLE')
      }
      if (this.currentMode === 'team') {
        return this.allTaskOptions.filter(item => item.mode === 'TEAM')
      }
      return this.allTaskOptions.filter(item => item.mode === 'VERSUS')
    },
    currentRankingList () {
      if (this.currentMode === 'single') {
        if (!this.selectedTaskId) return []
        return this.singleRankingList
      }
      if (this.currentMode === 'team') {
        return this.teamRankingList
      }
      return this.versusRankingList
    },
    pagedRankingList () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.currentRankingList.slice(start, end)
    },
    currentRuleText () {
      if (this.currentMode === 'single') {
        return '注：单人模式排行榜先按闯过关卡数降序排序；若闯过关卡数相同，则按闯关时间升序排序。'
      }
      if (this.currentMode === 'team') {
        return '注：分组对战排行榜按队伍天梯分降序排列；战绩与对战场次按一次挑战=一场统计，并在同一视图展示队长与队员信息。'
      }
      return '注：对战模式排行榜按天梯分降序排列；天梯分综合考虑基础战绩、对手强度、对战多样性与重复挑战惩罚。战绩与对战场次按一次挑战=一场统计。'
    },
    currentColspan () {
      if (this.currentMode === 'single') return 4
      if (this.currentMode === 'team') return 8
      return 5
    }
  },
  watch: {
    selectedTaskId () {
      this.currentPage = 1
      if (this.currentMode === 'single' && this.selectedTaskId) {
        this.loadSingleRanking()
      }
      if (this.currentMode === 'versus' && this.selectedTaskId) {
        this.loadVersusRanking()
      }
      if (this.currentMode === 'team' && this.selectedTaskId) {
        this.loadTeamRanking()
      }
    },
    '$route.query': {
      deep: true,
      handler () {
        this.loadTaskOptions()
      }
    }
  },
  created () {
    this.loadTaskOptions()
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadTaskOptions () {
      this.loading = true
      try {
        const url = this.isStudent
          ? `${API_BASE}/me/assignments?pageNum=0&pageSize=200`
          : `${API_BASE}/teacher/assignments?pageNum=0&pageSize=200`

        const result = await this.requestApi(url, { method: 'GET' })
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []

        this.allTaskOptions = content.map(item => ({
          id: item.id,
          name: item.title,
          mode: item.evaluationMode
        }))

        const queryTaskId = this.$route.query.taskId
        const queryMode = this.$route.query.mode

        if (queryMode === 'versus' || queryMode === 'single' || queryMode === 'team') {
          this.currentMode = queryMode
        }

        this.resetSelectedTask(queryTaskId)
      } catch (error) {
        this.allTaskOptions = []
        ElMessage.error(error.message || '任务列表加载失败')
      } finally {
        this.loading = false
      }
    },
    resetSelectedTask (preferredTaskId) {
      const options = this.currentTaskOptions
      if (options.length === 0) {
        this.selectedTaskId = ''
        this.singleRankingList = []
        this.versusRankingList = []
        this.teamRankingList = []
        return
      }

      const preferred = preferredTaskId && options.find(item => String(item.id) === String(preferredTaskId))
      this.selectedTaskId = preferred ? preferred.id : options[0].id

      if (this.currentMode === 'single') {
        this.loadSingleRanking()
      }
      if (this.currentMode === 'versus') {
        this.loadVersusRanking()
      }
      if (this.currentMode === 'team') {
        this.loadTeamRanking()
      }
      this.syncRouteQuery()
    },
    switchMode (mode) {
      if (this.currentMode === mode) return
      this.currentMode = mode
      this.currentPage = 1
      this.resetSelectedTask()
      this.syncRouteQuery()
    },
    syncRouteQuery () {
      const nextQuery = {
        ...this.$route.query,
        mode: this.currentMode,
        taskId: this.selectedTaskId || undefined
      }
      this.$router.replace({
        path: this.$route.path,
        query: nextQuery
      })
    },
    async loadVersusRanking () {
      if (!this.selectedTaskId) {
        this.versusRankingList = []
        return
      }

      this.loading = true
      try {
        const result = await this.requestApi(
          `${API_BASE}/assignments/${this.selectedTaskId}/leaderboard?pageNum=0&pageSize=200`,
          {
            method: 'GET'
          }
        )
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []

        this.versusRankingList = content.map(item => ({
          rank: item.rank,
          studentId: item.studentId,
          name: item.nickname || '未知学生',
          ladderScore: item.ladderScore ?? item.bestScore ?? 0,
          record: `${item.winCount || 0}胜${item.loseCount || 0}负${item.drawCount || 0}平`,
          matchCount: item.matchCount || 0
        }))
      } catch (error) {
        this.versusRankingList = []
        ElMessage.error(error.message || '排行榜加载失败')
      } finally {
        this.loading = false
      }
    },
    async loadSingleRanking () {
      if (!this.selectedTaskId) {
        this.singleRankingList = []
        return
      }

      this.loading = true
      try {
        const result = await this.requestApi(
          `${API_BASE}/assignments/${this.selectedTaskId}/leaderboard?pageNum=0&pageSize=200`,
          {
            method: 'GET'
          }
        )
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []
        this.singleRankingList = content.map(item => ({
          rank: item.rank || 0,
          studentId: item.studentId,
          name: item.nickname || '未知学生',
          levelCount: item.levelCount || 0,
          clearTime: this.formatClearTime(item.clearTime)
        }))
      } catch (error) {
        this.singleRankingList = []
        ElMessage.error(error.message || '单人排行榜加载失败')
      } finally {
        this.loading = false
      }
    },
    formatClearTime (value) {
      if (!value || value === '--') return '--'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    },
    async loadTeamRanking () {
      if (!this.selectedTaskId) {
        this.teamRankingList = []
        return
      }

      this.loading = true
      try {
        const result = await this.requestApi(
          `${API_BASE}/assignments/${this.selectedTaskId}/team-leaderboard?pageNum=0&pageSize=200`,
          {
            method: 'GET'
          }
        )
        if (!result) return

        const pageData = result.data || {}
        const content = Array.isArray(pageData.content) ? pageData.content : []

        this.teamRankingList = content.map(item => ({
          rank: item.rank,
          teamId: item.teamId,
          teamName: item.teamName || '未知队伍',
          captainName: item.captainName || '--',
          member1Name: item.member1Name || '--',
          member2Name: item.member2Name || '--',
          ladderScore: item.ladderScore ?? item.bestScore ?? 0,
          record: `${item.winCount || 0}胜${item.loseCount || 0}负${item.drawCount || 0}平`,
          matchCount: item.matchCount || 0
        }))
      } catch (error) {
        this.teamRankingList = []
        ElMessage.error(error.message || '分组排行榜加载失败')
      } finally {
        this.loading = false
      }
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

.table-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  padding: 18px 18px 0;
}

.toolbar {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
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

.mode-switch {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mode-btn {
  height: 36px;
  padding: 0 18px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
}

.mode-btn:hover {
  border-color: #1f4e8c;
  color: #1f4e8c;
}

.mode-btn.active {
  background: #1f4e8c;
  border-color: #1f4e8c;
  color: #ffffff;
}

.score-note {
  margin-bottom: 14px;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
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

.empty-cell {
  text-align: center !important;
  color: #909399 !important;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .mode-switch {
    justify-content: flex-start;
  }
}
</style>
