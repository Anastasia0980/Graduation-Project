<template>
  <header class='topbar'>
    <div class='topbar-left'>
      <div class='platform-name clickable' @click="$emit('platform-click')">
        强化学习智能体测评平台
      </div>

      <div class='top-nav'>
        <span class='top-nav-item' :class='{ active: activeNav === "home" }' @click="$emit('platform-click')">
          首页
        </span>
        <span class='top-nav-item' @click='showNoticeDialog = true'>公告通知</span>
        <span class='top-nav-item' @click='showHelpDialog = true'>帮助说明</span>
      </div>
    </div>

    <div class='topbar-right'>
      <template v-if='loggedIn'>
        <button class='switch-btn' @click="$emit('switch-role')">
          {{ switchButtonText }}
        </button>
        <span class='user-name clickable' @click="$emit('user-click')">{{ userName }}</span>
        <button class='text-btn' @click="$emit('logout')">退出</button>
      </template>

      <template v-else>
        <button class='text-btn' @click="$emit('login')">登录</button>
        <button class='primary-btn top-btn' @click="$emit('register')">注册</button>
      </template>
    </div>

    <div v-if='showNoticeDialog' class='dialog-mask' @click='showNoticeDialog = false'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>公告通知</div>
          <button class='close-btn' @click='showNoticeDialog = false'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div v-for='item in notices' :key='item.id' class='notice-item'>
            <div class='notice-item-title'>{{ item.title }}</div>
            <div class='notice-item-time'>{{ item.time }}</div>
            <div class='notice-item-content'>{{ item.content }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-if='showHelpDialog' class='dialog-mask' @click='showHelpDialog = false'>
      <div class='dialog-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>帮助说明</div>
          <button class='close-btn' @click='showHelpDialog = false'>关闭</button>
        </div>
        <div class='dialog-body'>
          <div class='help-section'>
            <div class='help-title'>学生端</div>
            <ul class='help-list'>
              <li>通过任务广场进入任务详情页，完成模型提交或查看任务说明。</li>
              <li>在提交历史中查看测评状态、胜负关系与录像回放。</li>
              <li>在个人主页中查看基础信息和近期记录。</li>
            </ul>
          </div>
          <div class='help-section'>
            <div class='help-title'>教师端</div>
            <ul class='help-list'>
              <li>通过发布任务页面创建单人模式、对战模式或团队锦标赛模式任务。</li>
              <li>在任务管理、班级管理和导出成绩页面查看对应数据。</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AppTopbar',
  props: {
    loggedIn: {
      type: Boolean,
      default: false
    },
    userName: {
      type: String,
      default: ''
    },
    activeNav: {
      type: String,
      default: 'home'
    },
    currentRole: {
      type: String,
      default: 'student'
    }
  },
  emits: ['platform-click', 'user-click', 'logout', 'login', 'register', 'switch-role'],
  data () {
    return {
      showNoticeDialog: false,
      showHelpDialog: false,
      notices: [
        { id: 1, title: '课堂测评提醒', time: '2026-03-12 09:00', content: '请按任务要求提交模型与配置文件。' },
        { id: 2, title: '井字棋任务开放', time: '2026-03-11 15:30', content: '对战模式任务已开放，可在任务详情页选择真人或人机对战。' }
      ]
    }
  },
  computed: {
    switchButtonText () {
      return this.currentRole === 'teacher' ? '切换到学生' : '切换到教师'
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 1000;
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
  gap: 32px;
}

.platform-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: #1f4e8c;
}

.top-nav {
  display: flex;
  gap: 24px;
}

.top-nav-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.top-nav-item:hover {
  color: #1f4e8c;
}

.top-nav-item.active {
  color: #1f4e8c;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #bfd7f4;
  background: #ecf5ff;
  color: #1f4e8c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.switch-btn:hover {
  background: #d9ecff;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.text-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.text-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.primary-btn {
  height: 36px;
  padding: 0 16px;
  border: none;
  background: #1f4e8c;
  color: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.primary-btn:hover {
  background: #173b69;
}

.top-btn {
  min-width: 72px;
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
  width: 680px;
  max-width: 100%;
  max-height: calc(100vh - 40px);
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  overflow-y: auto;
}

.notice-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
  margin-bottom: 12px;
}

.notice-item:last-child {
  margin-bottom: 0;
}

.notice-item-title,
.help-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.notice-item-time {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.notice-item-content,
.help-list li {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
}

.help-section {
  margin-bottom: 16px;
}

.help-section:last-child {
  margin-bottom: 0;
}

.help-list {
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 700px) {
  .topbar {
    height: auto;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }

  .topbar-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .top-nav {
    flex-wrap: wrap;
    gap: 16px;
  }

  .topbar-right {
    flex-wrap: wrap;
  }
}
</style>
