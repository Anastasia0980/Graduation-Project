<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='displayUserName'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <StudentSidebar
        :logged-in='isLoggedIn'
        active-menu='history'
        :task-menu-open='false'
        @profile-click='goProfile'
        @class-click='goStudentClass'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>提交历史</h2>
        </div>

        <div class='table-card'>
          <table class='history-table'>
            <thead>
              <tr>
                <th>任务名称</th>
                <th>提交模型</th>
                <th>提交时间</th>
                <th>测评状态</th>
                <th>对手</th>
                <th>结果</th>
                <th>详细结果</th>
                <th>日志</th>
                <th>录像</th>
                <th>下载</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if='loading'>
                <td colspan='10' class='empty-cell'>加载中...</td>
              </tr>

              <tr v-else-if='pagedHistoryList.length === 0'>
                <td colspan='10' class='empty-cell'>当前暂无提交记录</td>
              </tr>

              <tr v-else v-for='item in pagedHistoryList' :key='item.evaluationId'>
                <td>{{ item.taskName }}</td>
                <td>{{ item.modelName }}</td>
                <td>{{ item.submitTime }}</td>
                <td>{{ item.status }}</td>
                <td>{{ item.opponent }}</td>
                <td>{{ item.result }}</td>
                <td class='detail-cell'>{{ item.detailedResult }}</td>
                <td>
                  <button
                    v-if='item.hasLog'
                    class='table-btn'
                    @click='downloadLog(item)'
                  >
                    下载日志
                  </button>

                  <button
                    v-else
                    class='table-btn disabled-btn'
                    disabled
                  >
                    暂无
                  </button>
                </td>
                <td>
                  <button
                    v-if='item.hasVideo'
                    class='table-btn'
                    @click='openVideo(item)'
                  >
                    录像回放
                  </button>

                  <button
                    v-else
                    class='table-btn disabled-btn'
                    disabled
                  >
                    暂无
                  </button>
                </td>
                <td>
                  <button
                    v-if='item.canDownloadModel'
                    class='table-btn'
                    @click='downloadModel(item)'
                  >
                    下载模型
                  </button>

                  <button
                    v-else
                    class='table-btn disabled-btn'
                    disabled
                  >
                    暂无
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <CommonPagination
            v-model:currentPage='historyPage'
            v-model:pageSize='historyPageSize'
            :total='historyList.length'
            :page-size-options='[5, 10, 20]'
          />
        </div>
      </main>
    </div>

    <div v-if='videoVisible' class='video-mask' @click='closeVideo'>
      <div class='video-dialog' @click.stop>
        <div class='video-dialog-header'>
          <div class='video-dialog-title'>录像回放</div>
          <button class='close-btn' @click='closeVideo'>关闭</button>
        </div>

        <div class='video-dialog-body'>
          <div class='video-meta'>
            <div class='video-task-name'>{{ currentVideo.taskName }}</div>
            <div class='video-model-name'>模型文件：{{ currentVideo.modelName }}</div>
            <div v-if='currentVideo.taskMode.includes("单人")' class='video-hint'>
              视频左侧为 student，右侧 baseline
            </div>
          </div>

          <div v-if='videoLoading' class='video-loading-box'>
            视频加载中...
          </div>

          <div v-else-if='videoError' class='video-error-box'>
            {{ videoError }}
          </div>

          <video
            v-else-if='videoVisible && currentVideo.videoUrl'
            ref='videoPlayer'
            class='video-player'
            controls
            preload='metadata'
          >
            <source :src='currentVideo.videoUrl' type='video/mp4' />
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
import StudentSidebar from '../components/StudentSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest, notifyAuthExpiredAndRedirect } from '../utils/http'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'StudentHistoryView',
  components: {
    AppTopbar,
    StudentSidebar,
    CommonPagination
  },
  data () {
    return {
      loading: false,
      videoVisible: false,
      videoLoading: false,
      videoError: '',
      currentVideo: {
        taskName: '',
        modelName: '',
        taskMode: '',
        videoUrl: '',
        sourceApiUrl: ''
      },
      historyPage: 1,
      historyPageSize: 5,
      historyList: []
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    displayUserName () {
      return localStorage.getItem('auth_name') || '学生'
    },
    pagedHistoryList () {
      const start = (this.historyPage - 1) * this.historyPageSize
      const end = start + this.historyPageSize
      return this.historyList.slice(start, end)
    }
  },
  created () {
    this.loadHistoryList()
  },
  methods: {
    async requestApi (url, options = {}) {
      return await apiRequest(url, options)
    },
    async loadHistoryList () {
      this.loading = true
      try {
        const result = await this.requestApi(`${API_BASE}/me/submissions`, {
          method: 'GET'
        })
        if (!result) return

        const list = Array.isArray(result.data) ? result.data : []
        this.historyList = list.map(item => ({
          evaluationId: item.evaluationId,
          evaluationResultId: item.evaluationResultId,
          taskName: item.taskTitle || '未知任务',
          taskMode: item.taskMode || '',
          modelName: item.modelName || '--',
          submitTime: item.submitTime || '--',
          status: item.status || '--',
          opponent: item.opponentName || '无',
          result: item.resultText || '-',
          detailedResult: item.detailedResult || '-',
          hasLog: !!item.evaluationResultId,
          hasVideo: !!item.hasVideo && !!item.evaluationResultId,
          canDownloadModel: !!item.evaluationId,
          sourceApiUrl: item.evaluationResultId
            ? `${API_BASE}/evaluation-results/${item.evaluationResultId}/video`
            : '',
          logApiUrl: item.evaluationResultId
            ? `${API_BASE}/evaluation-results/${item.evaluationResultId}/log`
            : '',
          modelDownloadUrl: item.evaluationId
            ? `${API_BASE}/evaluation-results/evaluation/${item.evaluationId}/model-package`
            : ''
        }))
      } catch (error) {
        this.historyList = []
        ElMessage.error(error.message || '提交历史加载失败')
      } finally {
        this.loading = false
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
    goStudentClass () {
      this.$router.push('/student/class')
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
    },
    logout () {
      clearAuthState()
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    },
    async openVideo (item) {
      this.closeVideoObjectUrlOnly()

      this.currentVideo = {
        taskName: item.taskName,
        modelName: item.modelName,
        taskMode: item.taskMode || '',
        videoUrl: '',
        sourceApiUrl: item.sourceApiUrl
      }
      this.videoError = ''
      this.videoLoading = true

      try {
        const token = localStorage.getItem('auth_token')
        if (!token) {
          notifyAuthExpiredAndRedirect(this.$router)
          return
        }

        const response = await fetch(item.sourceApiUrl, {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        })

        if (response.status === 401) {
          notifyAuthExpiredAndRedirect(this.$router)
          return
        }
        if (!response.ok) {
          throw new Error(`视频加载失败（${response.status}）`)
        }

        const blob = await response.blob()
        if (!blob || blob.size === 0) {
          throw new Error('视频文件为空')
        }

        const objectUrl = URL.createObjectURL(blob)
        this.currentVideo.videoUrl = objectUrl
        this.videoVisible = true
      } catch (error) {
        this.videoError = error.message || '视频加载失败'
        ElMessage.error(this.videoError)
      } finally {
        this.videoLoading = false
      }
    },
    async downloadLog (item) {
      try {
        await this.fetchAndDownload(item.logApiUrl, `${item.taskName || 'evaluation'}_log.txt`)
        ElMessage.success('日志下载成功')
      } catch (error) {
        ElMessage.error(error.message || '日志下载失败')
      }
    },
    async downloadModel (item) {
      try {
        await this.fetchAndDownload(item.modelDownloadUrl, `${item.taskName || 'evaluation'}_model.zip`)
        ElMessage.success('模型下载成功')
      } catch (error) {
        ElMessage.error(error.message || '模型下载失败')
      }
    },
    async fetchAndDownload (url, filename) {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        notifyAuthExpiredAndRedirect(this.$router)
        throw new Error('登录信息已失效，请重新登录')
      }

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (response.status === 401) {
        notifyAuthExpiredAndRedirect(this.$router)
        throw new Error('登录信息已失效，请重新登录')
      }
      if (!response.ok) {
        throw new Error(`下载失败（${response.status}）`)
      }

      const blob = await response.blob()
      if (!blob || blob.size === 0) {
        throw new Error('下载文件为空')
      }

      const objectUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(objectUrl)
    },
    closeVideoObjectUrlOnly () {
      if (this.currentVideo.videoUrl) {
        URL.revokeObjectURL(this.currentVideo.videoUrl)
      }
    },
    closeVideo () {
      const player = this.$refs.videoPlayer
      if (player) {
        player.pause()
        player.currentTime = 0
      }

      this.closeVideoObjectUrlOnly()

      this.currentVideo = {
        taskName: '',
        modelName: '',
        taskMode: '',
        videoUrl: '',
        sourceApiUrl: ''
      }
      this.videoError = ''
      this.videoLoading = false
      this.videoVisible = false
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

.history-table {
  width: 100%;
  border-collapse: collapse;
}

.history-table th,
.history-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  font-size: 14px;
  vertical-align: middle;
}

.history-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.empty-cell {
  text-align: center !important;
  color: #909399;
}

.detail-cell {
  min-width: 220px;
  white-space: normal;
  line-height: 1.6;
  color: #606266;
}

.table-btn {
  min-width: 84px;
  height: 34px;
  padding: 0 12px;
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

.disabled-btn:hover {
  background: #c0c4cc;
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
  min-height: 56px;
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
  margin-bottom: 14px;
}

.video-task-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.video-model-name {
  font-size: 14px;
  color: #606266;
}

.video-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.video-player {
  width: 100%;
  max-height: 520px;
  background: #000000;
  border-radius: 8px;
}

.video-loading-box,
.video-error-box {
  width: 100%;
  min-height: 220px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  background: #fafafa;
}

.video-error-box {
  color: #c45656;
  background: #fff6f6;
  border-color: #f3c2c2;
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

  .history-table {
    min-width: 1320px;
  }
}
</style>
