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
        <span class="nav-item">公告通知</span>
        <span class="nav-item">帮助说明</span>
      </nav>
    </div>

    <div class="topbar-right">
      <template v-if="loggedIn">
        <span class="user-name" @click="$emit('user-click')">{{ displayUserName }}</span>
        <button class="ghost-btn" @click="handleLogout">退出</button>
      </template>

      <template v-else>
        <button class="ghost-btn" @click="$emit('login')">登录</button>
        <button class="primary-btn" @click="$emit('register')">注册</button>
      </template>
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
    currentRole: {
      type: String,
      default: ''
    },
    activeNav: {
      type: String,
      default: 'home'
    }
  },
  computed: {
    displayUserName () {
      if (!this.loggedIn) {
        return ''
      }
      return this.userName || ''
    }
  },
  methods: {
    handleLogout () {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
      sessionStorage.removeItem('mock_logged_out_view')
      this.$emit('logout')
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
</style>
