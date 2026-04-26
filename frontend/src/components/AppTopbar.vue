<template>
  <header class="topbar">
    <div class="topbar-left">
      <div class="platform-title" @click="$emit('platform-click')">
        强化学习智能体测评平台
      </div>

      <nav class="nav-list">
        <span
          class="nav-item"
          :class="{ active: activeNav === 'home' }"
          @click="$emit('platform-click')"
        >
          首页
        </span>
        <span class="nav-item" @click="openAnnouncementDialog">公告通知</span>
        <span class="nav-item" @click="openHelpDialog">帮助说明</span>
      </nav>
    </div>

    <div class="topbar-right">
      <template v-if="effectiveLoggedIn">
        <span class="user-name" @click="$emit('user-click')">{{ displayUserName }}</span>
        <button class="ghost-btn" @click="handleLogout">退出</button>
      </template>

      <template v-else>
        <button class="ghost-btn" @click="$emit('login')">登录</button>
        <button class="primary-btn" @click="$emit('register')">注册</button>
      </template>
    </div>
  </header>

  <div v-if="announcementDialogVisible" class="dialog-mask" @click.self="closeAnnouncementDialog">
    <div class="dialog-card topbar-dialog-card">
      <div class="dialog-title">公告通知</div>
      <div v-if="announcementLoading" class="empty-box">正在加载公告...</div>
      <div v-else-if="announcements.length === 0" class="empty-box">当前暂无公告。</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>公告内容</th>
              <th>发布时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in announcements" :key="`announcement_${item.id}`" class="clickable-row" @click="showAnnouncementDetail(item)">
              <td class="single-line-cell">{{ item.content || '--' }}</td>
              <td>{{ formatDateTime(item.updateTime || item.createTime) }}</td>
            </tr>
          </tbody>
        </table>
        <CommonPagination
          v-model:currentPage="announcementPage"
          v-model:pageSize="announcementPageSize"
          :total="announcementTotal"
          :page-size-options="[5, 10, 20]"
          @change="loadAnnouncements"
        />
      </div>
      <div class="dialog-actions">
        <button class="primary-btn" type="button" @click="closeAnnouncementDialog">关闭</button>
      </div>
    </div>
  </div>

  <div v-if="announcementDetailVisible" class="dialog-mask" @click.self="announcementDetailVisible = false">
    <div class="dialog-card detail-dialog-card">
      <div class="dialog-title">公告详情</div>
      <div class="dialog-body detail-content">{{ activeAnnouncement.content || '--' }}</div>
      <div class="dialog-actions">
        <button class="primary-btn" type="button" @click="announcementDetailVisible = false">关闭</button>
      </div>
    </div>
  </div>

  <div v-if="helpDialogVisible" class="dialog-mask" @click.self="closeHelpDialog">
    <div class="dialog-card topbar-dialog-card">
      <div class="dialog-title">帮助说明</div>
      <div v-if="helpLoading" class="empty-box">正在加载说明...</div>
      <div v-else-if="helpDocs.length === 0" class="empty-box">当前暂无说明。</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>标题</th>
              <th>更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in helpDocs" :key="`help_${item.id}`" class="clickable-row" @click="showHelpDetail(item)">
              <td>{{ item.title || '--' }}</td>
              <td>{{ formatDateTime(item.updateTime || item.createTime) }}</td>
            </tr>
          </tbody>
        </table>
        <CommonPagination
          v-model:currentPage="helpPage"
          v-model:pageSize="helpPageSize"
          :total="helpTotal"
          :page-size-options="[5, 10, 20]"
          @change="loadHelpDocs"
        />
      </div>
      <div class="dialog-actions">
        <button class="primary-btn" type="button" @click="closeHelpDialog">关闭</button>
      </div>
    </div>
  </div>

  <div v-if="helpDetailVisible" class="dialog-mask" @click.self="helpDetailVisible = false">
    <div class="dialog-card detail-dialog-card nested-detail-dialog-card">
      <div class="dialog-title">{{ activeHelp.title || '说明详情' }}</div>
      <div class="dialog-body detail-content help-detail-body">
        <div class="help-detail-title">{{ activeHelp.title || '--' }}</div>
        <div class="help-detail-text">{{ activeHelp.content || '--' }}</div>
      </div>
      <div class="dialog-actions">
        <button class="primary-btn" type="button" @click="helpDetailVisible = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest } from '../utils/http'
import CommonPagination from './CommonPagination.vue'

const API_BASE = (process.env.VUE_APP_API_BASE && process.env.VUE_APP_API_BASE.trim()) ||
  (typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : 'http://localhost:8080')

export default {
  name: 'AppTopbar',
  components: {
    CommonPagination
  },
  props: {
    loggedIn: {
      type: Boolean,
      default: false
    },
    userName: {
      type: String,
      default: ''
    },
    currentRole: {
      type: String,
      default: ''
    },
    activeNav: {
      type: String,
      default: 'home'
    }
  },
  data () {
    return {
      announcementDialogVisible: false,
      announcementDetailVisible: false,
      announcementLoading: false,
      announcements: [],
      announcementPage: 1,
      announcementPageSize: 5,
      announcementTotal: 0,
      activeAnnouncement: {},
      helpDialogVisible: false,
      helpDetailVisible: false,
      helpLoading: false,
      helpDocs: [],
      helpPage: 1,
      helpPageSize: 5,
      helpTotal: 0,
      activeHelp: {}
    }
  },
  computed: {
    effectiveLoggedIn () {
      return this.loggedIn && hasAuthToken()
    },
    displayUserName () {
      if (!this.effectiveLoggedIn) {
        return ''
      }
      return this.userName || localStorage.getItem('auth_name') || ''
    }
  },
  methods: {
    handleLogout () {
      clearAuthState()
      this.$emit('logout')
    },
    ensureLoggedIn () {
      if (this.effectiveLoggedIn) {
        return true
      }
      this.$emit('login')
      return false
    },
    async openAnnouncementDialog () {
      if (!this.ensureLoggedIn()) return
      this.announcementDialogVisible = true
      this.announcementPage = 1
      await this.loadAnnouncements()
    },
    closeAnnouncementDialog () {
      this.announcementDialogVisible = false
    },
    async loadAnnouncements () {
      this.announcementLoading = true
      try {
        const result = await apiRequest(`${API_BASE}/announcements?pageNum=${this.announcementPage - 1}&pageSize=${this.announcementPageSize}`, {
          method: 'GET',
          router: this.$router
        })
        const pageData = result?.data || {}
        this.announcements = Array.isArray(pageData.content) ? pageData.content : []
        this.announcementTotal = Number(pageData.totalElements || 0)
      } catch (error) {
        this.announcements = []
        this.announcementTotal = 0
        ElMessage.error(error.message || '公告加载失败')
      } finally {
        this.announcementLoading = false
      }
    },
    showAnnouncementDetail (item) {
      this.activeAnnouncement = { ...item }
      this.announcementDetailVisible = true
    },
    async openHelpDialog () {
      if (!this.ensureLoggedIn()) return
      this.helpDialogVisible = true
      this.helpPage = 1
      await this.loadHelpDocs()
    },
    closeHelpDialog () {
      this.helpDialogVisible = false
    },
    async loadHelpDocs () {
      this.helpLoading = true
      try {
        const result = await apiRequest(`${API_BASE}/help-docs?pageNum=${this.helpPage - 1}&pageSize=${this.helpPageSize}`, {
          method: 'GET',
          router: this.$router
        })
        const pageData = result?.data || {}
        this.helpDocs = Array.isArray(pageData.content) ? pageData.content : []
        this.helpTotal = Number(pageData.totalElements || 0)
      } catch (error) {
        this.helpDocs = []
        this.helpTotal = 0
        ElMessage.error(error.message || '说明加载失败')
      } finally {
        this.helpLoading = false
      }
    },
    showHelpDetail (item) {
      this.activeHelp = { ...item }
      this.helpDetailVisible = true
    },
    formatDateTime (value) {
      if (!value) return '--'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.topbar {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 28px;
}

.platform-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
  cursor: pointer;
}

.nav-list {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.nav-item:hover {
  color: #1f4e8c;
}

.nav-item.active {
  color: #1f4e8c;
  font-weight: 700;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #303133;
  cursor: pointer;
}

.ghost-btn,
.primary-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.ghost-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.ghost-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.primary-btn {
  border: none;
  background: #1f4e8c;
  color: #ffffff;
}

.primary-btn:hover {
  background: #173b69;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 4000;
}

.dialog-card {
  width: 760px;
  max-width: calc(100vw - 32px);
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
  padding: 20px;
}

.topbar-dialog-card {
  width: 820px;
}

.detail-dialog-card {
  width: 760px;
}

.nested-detail-dialog-card {
  z-index: 4100;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.dialog-body {
  margin-bottom: 18px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.empty-box {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  color: #909399;
  background: #fafafa;
}

.table-wrap {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 12px;
  font-size: 14px;
  color: #303133;
  text-align: left;
  vertical-align: middle;
}

.table th {
  background: #f8f9fb;
  color: #606266;
  font-weight: 600;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #f8fbff;
}

.single-line-cell {
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}

.help-detail-body {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.help-detail-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 12px;
}

.help-detail-text {
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
