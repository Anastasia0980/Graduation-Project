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

    <div class='top-nav-bar'>
      <button class='back-btn' @click='goBack'>返回任务详情</button>
      <div class='page-title'>当前对战安排</div>
    </div>

    <main class='content-area'>
      <div class='card info-card'>
        <div class='card-title'>任务说明</div>
        <div class='info-text'>
          当前页面展示团队锦标赛任务的对战树示意。后续接入后端后，可根据真实队伍与轮次结果动态渲染淘汰赛安排。
        </div>
      </div>

      <div class='card bracket-card'>
        <div class='card-title'>对战树</div>

        <div class='bracket-wrapper'>
          <div class='round-column'>
            <div class='round-title'>第一轮</div>
            <div class='match-box'>
              <div class='team-line win'>Alpha 队</div>
              <div class='team-line'>Beta 队</div>
            </div>
            <div class='match-box'>
              <div class='team-line'>Gamma 队</div>
              <div class='team-line win'>Delta 队</div>
            </div>
            <div class='match-box'>
              <div class='team-line win'>Epsilon 队</div>
              <div class='team-line'>Zeta 队</div>
            </div>
            <div class='match-box'>
              <div class='team-line'>Theta 队</div>
              <div class='team-line win'>你的队伍</div>
            </div>
          </div>

          <div class='round-column'>
            <div class='round-title'>半决赛</div>
            <div class='match-box middle-box'>
              <div class='team-line win'>Alpha 队</div>
              <div class='team-line'>Delta 队</div>
            </div>
            <div class='match-box middle-box'>
              <div class='team-line'>Epsilon 队</div>
              <div class='team-line win'>你的队伍</div>
            </div>
          </div>

          <div class='round-column'>
            <div class='round-title'>决赛</div>
            <div class='match-box final-box'>
              <div class='team-line'>Alpha 队</div>
              <div class='team-line win'>你的队伍</div>
            </div>
          </div>

          <div class='round-column champion-column'>
            <div class='round-title'>冠军</div>
            <div class='champion-box'>你的队伍</div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'

export default {
  name: 'StudentTournamentView',
  components: {
    AppTopbar
  },
  methods: {
    goBack () {
      this.$router.back()
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
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

.top-nav-bar {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.back-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.content-area {
  padding: 20px;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.info-card {
  margin-bottom: 20px;
}

.card-title {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.info-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.bracket-card {
  overflow-x: auto;
}

.bracket-wrapper {
  display: flex;
  gap: 28px;
  min-width: 980px;
  align-items: flex-start;
}

.round-column {
  width: 220px;
}

.round-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  text-align: center;
}

.match-box {
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #ffffff;
  margin-bottom: 24px;
}

.middle-box {
  margin-top: 60px;
  margin-bottom: 80px;
}

.final-box {
  margin-top: 140px;
}

.team-line {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  background: #f8fafc;
}

.team-line:last-child {
  margin-bottom: 0;
}

.team-line.win {
  color: #1f4e8c;
  font-weight: 700;
  background: #ecf5ff;
  border-color: #bfd7f4;
}

.champion-column {
  width: 180px;
}

.champion-box {
  margin-top: 170px;
  padding: 18px 16px;
  border-radius: 8px;
  background: #ecf5ff;
  border: 1px solid #bfd7f4;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #1f4e8c;
}

@media (max-width: 700px) {
  .top-nav-bar {
    height: auto;
    padding: 14px 16px;
    flex-wrap: wrap;
  }

  .content-area {
    padding: 16px;
  }
}
</style>
