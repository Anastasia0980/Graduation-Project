<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      :user-name="displayUserName"
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
          <div class="header-left">
            <button class="back-btn" @click="goBack">返回</button>
            <h2>学生提交情况</h2>
          </div>
        </div>

        <div class="card">
          <table class="common-table">
            <thead>
              <tr>
                <th>学生姓名</th>
                <th>任务名称</th>
                <th>提交模型</th>
                <th>提交时间</th>
                <th>测评状态</th>
                <th>胜负关系</th>
                <th>录像</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="table-empty-cell">提交记录加载中...</td>
              </tr>
              <tr v-else-if="pagedSubmissionList.length === 0">
                <td colspan="7" class="table-empty-cell">当前暂无提交记录</td>
              </tr>
              <tr v-else v-for="item in pagedSubmissionList" :key="item.id">
                <td>
                  <span class="student-link" @click="goStudentDetail(item.studentId)">
                    {{ item.studentName }}
                  </span>
                </td>
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
            v-model:currentPage="currentPage"
            v-model:pageSize="pageSize"
            :total="submissionList.length"
            :page-size-options="[5, 10, 20]"
          />
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
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherTaskSubmissionsView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      displayUserName: localStorage.getItem('auth_name') || '教师',
      currentPage: 1,
      pageSize: 5,
      loading: false,
      videoVisible: false,
      currentVideo: {
        taskName: '',
        modelName: '',
        videoUrl: ''
      },
      submissionList: []
    }
  },
  computed: {
    pagedSubmissionList () {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.submissionList.slice(start, end)
    }
  },
  created () {
    this.loadSubmissionList()
  },
  methods: {
    getAuthHeaders () {
      const token = localStorage.getItem('auth_token') || ''
      return {
        Authorization: `Bearer ${token}`
      }
    },
    async loadSubmissionList () {
      const taskId = this.$route.params.taskId
      if (!taskId) {
        this.submissionList = []
        return
      }

      this.loading = true
      try {
        const response = await fetch(`${API_BASE}/assignments/${taskId}/submissions`, {
          method: 'GET',
          headers: this.getAuthHeaders()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '提交记录加载失败')
        }

        const list = Array.isArray(result.data) ? result.data : []
        this.submissionList = list.map(item => ({
          id: item.evaluationId,
          evaluationId: item.evaluationId,
          evaluationResultId: item.evaluationResultId,
          studentId: item.studentId,
          studentName: item.studentName || '未知学生',
          taskName: item.taskTitle || '未知任务',
          modelName: item.modelName || '--',
          submitTime: item.submitTime || '--',
          status: item.status || '--',
          result: item.resultText || '-',
          hasVideo: !!item.hasVideo
        }))
      } catch (error) {
        this.submissionList = []
        ElMessage.error(error.message || '提交记录加载失败')
      } finally {
        this.loading = false
      }
    },
    goStudentDetail (studentId) {
      this.$router.push(`/teacher/student-detail/${studentId}`)
    },
    async openVideo (item) {
      if (!item.evaluationResultId) {
        ElMessage.warning('暂无可播放录像')
        return
      }

      try {
        const response = await fetch(`${API_BASE}/evaluation-results/${item.evaluationResultId}/video`, {
          method: 'GET',
          headers: this.getAuthHeaders()
        })

        if (!response.ok) {
          throw new Error(`视频加载失败（${response.status}）`)
        }

        const blob = await response.blob()
        if (!blob || blob.size === 0) {
          throw new Error('视频文件为空')
        }

        const objectUrl = URL.createObjectURL(blob)
        this.currentVideo = {
          taskName: item.taskName,
          modelName: item.modelName,
          videoUrl: objectUrl
        }
        this.videoVisible = true
      } catch (error) {
        ElMessage.error(error.message || '视频加载失败')
      }
    },
    closeVideo () {
      const player = this.$refs.videoPlayer
      if (player) {
        player.pause()
        player.currentTime = 0
      }
      if (this.currentVideo.videoUrl && this.currentVideo.videoUrl.startsWith('blob:')) {
        URL.revokeObjectURL(this.currentVideo.videoUrl)
      }
      this.currentVideo = {
        taskName: '',
        modelName: '',
        videoUrl: ''
      }
      this.videoVisible = false
    },
    goBack () {
      this.$router.push('/teacher/tasks')
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
  color: #1f2d3d;
}

.back-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.card {
  background: #fff;
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

.table-empty-cell {
  text-align: center;
  color: #909399;
}

.student-link {
  color: #1f4e8c;
  cursor: pointer;
  font-weight: 600;
}

.student-link:hover {
  text-decoration: underline;
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

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 980px;
  }
}
</style>
