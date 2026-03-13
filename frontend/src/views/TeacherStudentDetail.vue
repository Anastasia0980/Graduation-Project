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
        active-menu="class-data"
        @teacher-home-click="goTeacherHome"
        @publish-click="goPublishTask"
        @manage-click="goTaskManage"
        @class-data-click="goClassData"
        @export-click="goExportScore"
      />

      <main class="content-area">
        <div class="page-header">
          <div class="header-left">
            <button class="back-btn" @click="goBack">返回</button>
            <h2>学生详情</h2>
          </div>
        </div>

        <div class="profile-grid">
          <div class="card">
            <div class="card-title">基本信息</div>
            <div class="info-list">
              <div class="info-row avatar-row">
                <span class="label">头像</span>
                <div class="avatar-box">
                  <img :src="student.avatar" alt="头像" class="avatar-image">
                </div>
              </div>

              <div class="info-row">
                <span class="label">姓名</span>
                <span class="value">{{ student.name }}</span>
              </div>

              <div class="info-row">
                <span class="label">Email</span>
                <span class="value">{{ student.email }}</span>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-title">学习概览</div>
            <div class="summary-grid">
              <div class="summary-item">
                <div class="summary-value">12</div>
                <div class="summary-label">总提交次数</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">7</div>
                <div class="summary-label">已完成测评</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">3</div>
                <div class="summary-label">获胜次数</div>
              </div>
              <div class="summary-item">
                <div class="summary-value">2</div>
                <div class="summary-label">测评中</div>
              </div>
            </div>
          </div>

          <div class="card full-width">
            <div class="card-title">提交历史</div>
            <table class="history-table">
              <thead>
                <tr>
                  <th>任务名称</th>
                  <th>提交模型</th>
                  <th>提交时间</th>
                  <th>测评状态</th>
                  <th>胜负关系</th>
                  <th>录像</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedHistoryList" :key="item.id">
                  <td>{{ item.taskName }}</td>
                  <td>{{ item.modelName }}</td>
                  <td>{{ item.submitTime }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.result }}</td>
                  <td>
                    <button
                      v-if="item.hasVideo"
                      class="table-btn"
                      @click="openVideo(item)"
                    >
                      录像回放
                    </button>

                    <button
                      v-else
                      class="table-btn disabled-btn"
                      disabled
                    >
                      暂无
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage="historyPage"
              v-model:pageSize="historyPageSize"
              :total="historyList.length"
              :page-size-options="[5, 10, 20]"
            />
          </div>
        </div>
      </main>
    </div>

    <div v-if="videoVisible" class="video-mask" @click="closeVideo">
      <div class="video-dialog" @click.stop>
        <div class="video-dialog-header">
          <div class="video-dialog-title">录像回放</div>
          <button class="close-btn" @click="closeVideo">关闭</button>
        </div>

        <div class="video-dialog-body">
          <div class="video-meta">
            <div class="video-task-name">{{ currentVideo.taskName }}</div>
            <div class="video-model-name">模型文件：{{ currentVideo.modelName }}</div>
          </div>

          <video
            v-if="videoVisible"
            ref="videoPlayer"
            class="video-player"
            controls
            preload="metadata"
          >
            <source :src="currentVideo.videoUrl" type="video/mp4">
            当前浏览器不支持视频播放。
          </video>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import defaultAvatar from '../assets/logo.png'
import gameVideo from '../assets/game.mp4'

export default {
  name: 'TeacherStudentDetailView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      student: {
        avatar: defaultAvatar,
        name: '张三',
        email: 'zhangsan@bjtu.edu.cn'
      },
      historyPage: 1,
      historyPageSize: 5,
      videoVisible: false,
      currentVideo: {
        taskName: '',
        modelName: '',
        videoUrl: ''
      },
      historyList: [
        {
          id: 1,
          taskName: '井字棋对战游戏',
          modelName: 'model_v1.pt',
          submitTime: '2026-07-01 20:15',
          status: '已完成',
          result: '获胜',
          hasVideo: true,
          videoUrl: gameVideo
        },
        {
          id: 2,
          taskName: '井字棋对战游戏',
          modelName: 'model_v2.pt',
          submitTime: '2026-07-03 18:42',
          status: '已完成',
          result: '失败',
          hasVideo: true,
          videoUrl: gameVideo
        },
        {
          id: 3,
          taskName: '井字棋对战游戏',
          modelName: 'model_v3.pt',
          submitTime: '2026-07-05 14:10',
          status: '测评中',
          result: '-',
          hasVideo: false,
          videoUrl: ''
        }
      ]
    }
  },
  computed: {
    pagedHistoryList () {
      const start = (this.historyPage - 1) * this.historyPageSize
      const end = start + this.historyPageSize
      return this.historyList.slice(start, end)
    }
  },
  methods: {
    openVideo (item) {
      this.currentVideo = {
        taskName: item.taskName,
        modelName: item.modelName,
        videoUrl: item.videoUrl
      }
      this.videoVisible = true
    },
    closeVideo () {
      const player = this.$refs.videoPlayer
      if (player) {
        player.pause()
        player.currentTime = 0
      }
      this.videoVisible = false
    },
    goBack () {
      this.$router.push('/teacher/classes')
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

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.back-btn {
  height: 36px;
  min-width: 72px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
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

.avatar-row {
  align-items: center;
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

.avatar-box {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #ffffff;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.summary-item {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
  text-align: center;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f4e8c;
  margin-bottom: 6px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
}

.history-table th,
.history-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
  color: #303133;
  vertical-align: middle;
}

.history-table th {
  background: #f8fafc;
  font-weight: 700;
  color: #606266;
}

.table-btn {
  height: 34px;
  min-width: 86px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
}

.table-btn:hover {
  background: #173b69;
}

.disabled-btn {
  background: #c0c4cc;
  cursor: not-allowed;
}

.video-mask {
  position: fixed;
  inset: 0;
  background: rgba(31, 45, 61, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 20px;
}

.video-dialog {
  width: 860px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.video-dialog-header {
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.video-dialog-title {
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

.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.video-dialog-body {
  padding: 20px;
}

.video-meta {
  margin-bottom: 16px;
}

.video-task-name {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.video-model-name {
  font-size: 14px;
  color: #606266;
}

.video-player {
  width: 100%;
  max-height: 520px;
  background: #000000;
  border-radius: 6px;
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

  .card {
    overflow-x: auto;
  }

  .history-table {
    min-width: 760px;
  }
}

@media (max-width: 700px) {
  .video-dialog-body {
    padding: 16px;
  }
}
</style>
